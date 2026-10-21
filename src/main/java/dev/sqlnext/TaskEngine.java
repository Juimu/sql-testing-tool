package dev.sqlnext;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persisted single-worker queue. Recovery requires an explicit retry, never silent reexecution. */
public final class TaskEngine implements AutoCloseable {
    private final Store store;
    private final java.nio.file.Path data;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"sqlnext-assessments");t.setDaemon(true);return t;});
    private final ConcurrentMap<Long,AtomicBoolean> cancellations=new ConcurrentHashMap<>();
    private volatile boolean closed;
    private volatile String error="";
    public TaskEngine(Store store,java.nio.file.Path data) throws Exception {
        this.store=store;this.data=data;
        for(var t:store.tasks())if(t.status().equals("RUNNING"))store.transition(t.id(),"RUNNING","INTERRUPTED",t.result(),"Interrupted by a previous shutdown; retry manually.");
        worker.scheduleWithFixedDelay(this::tick,0,1100,TimeUnit.MILLISECONDS);
    }
    public String error(){return error;}
    private void tick() {
        if(closed)return;
        try {
            var task=store.tasks().stream().filter(t->t.status().equals("QUEUED")&&!Instant.parse(t.due()).isAfter(Instant.now())).sorted(Comparator.comparingInt(Store.Assessment::priority).reversed().thenComparingLong(Store.Assessment::id)).findFirst();
            if(task.isPresent())execute(task.get());
        }catch(Exception e){error=e.getClass().getSimpleName()+": unable to process queue; inspect local configuration.";}
    }
    private void execute(Store.Assessment task) throws Exception {
        AtomicBoolean cancel=new AtomicBoolean();cancellations.put(task.id(),cancel);
        try {
            if(!store.transition(task.id(),"QUEUED","RUNNING","",""))return;
            var project=store.projects().stream().filter(p->p.id()==task.projectId()).findFirst().orElseThrow();
            Object result;
            if(task.profile().endsWith("lab"))result=Labs.run(task.profile());
            else if(task.profile().equals("sqlmap diagnostic"))result=Sqlmap.assess(project.scope(),task.target(),store.setting("python.path",""),store.setting("sqlmap.path",""),task.route(),data.resolve("engine-output/task-"+task.id()),cancel);
            else result=Network.inspect(project.scope(),task.target(),Network.Route.parse(task.route()),cancel);
            if(cancel.get()||closed)store.transition(task.id(),"RUNNING","CANCELLED","","Cancelled by user or shutdown.");
            else store.transition(task.id(),"RUNNING","COMPLETED",new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result),"");
        }catch(Exception e) {
            store.transition(task.id(),"RUNNING",cancel.get()||closed?"CANCELLED":"FAILED","",diagnose(e));
        }finally{cancellations.remove(task.id());}
    }
    private static String diagnose(Exception e) {
        // Exception messages may contain target credentials/query strings; persist categories only.
        if(e instanceof javax.net.ssl.SSLException)return "TLS certificate/hostname validation failed. Verification was not bypassed.";
        if(e instanceof java.net.SocketTimeoutException)return "Network timeout (5 second connect/read limit).";
        if(e instanceof java.net.UnknownHostException)return "DNS resolution failed.";
        if(e instanceof java.net.ConnectException)return "Connection or proxy route refused.";
        if(e instanceof InterruptedException)return "Task interrupted.";
        return e.getClass().getSimpleName()+": task failed; check scope, route and target availability.";
    }
    public void cancel(long id) throws Exception {
        var token=cancellations.get(id);if(token!=null)token.set(true);
        var t=store.tasks().stream().filter(x->x.id()==id).findFirst().orElseThrow();
        if(List.of("QUEUED","RUNNING","PAUSED","INTERRUPTED").contains(t.status()))store.transition(id,t.status(),"CANCELLED",t.result(),"Cancelled by user.");
    }
    public void pause(long id) throws Exception {store.transition(id,"QUEUED","PAUSED","","");}
    public void resume(long id) throws Exception {
        var t=store.tasks().stream().filter(x->x.id()==id).findFirst().orElseThrow();
        if(!List.of("PAUSED","FAILED","INTERRUPTED").contains(t.status()))throw new IllegalArgumentException("Only paused, failed or interrupted tasks can be resumed.");
        store.transition(id,t.status(),"QUEUED","","");
    }
    @Override public void close() throws Exception {
        closed=true;cancellations.values().forEach(c->c.set(true));worker.shutdownNow();
        if(!worker.awaitTermination(12,TimeUnit.SECONDS))throw new IllegalStateException("Worker has not stopped; keep workspace open until network timeout completes.");
        for(var t:store.tasks())if(t.status().equals("RUNNING"))store.transition(t.id(),"RUNNING","INTERRUPTED",t.result(),"Application shutdown.");
    }
}
