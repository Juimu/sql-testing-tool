package dev.sqlnext;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class AssessmentTest {
    @TempDir Path dir;
    private HttpServer server;
    private String base;
    private final AtomicInteger requests=new AtomicInteger();
    @BeforeEach void server() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/app",exchange->{requests.incrementAndGet();byte[] response="<html>SQLITE_ERROR: harmless fixture</html>".getBytes();exchange.getResponseHeaders().set("Content-Type","text/html");exchange.sendResponseHeaders(200,response.length);try(var out=exchange.getResponseBody()){out.write(response);}});
        server.createContext("/redirect",exchange->{requests.incrementAndGet();exchange.getResponseHeaders().set("Location","/outside");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.start();base="http://127.0.0.1:"+server.getAddress().getPort();
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void readOnlyDiagnosticsScopeRedactionAndNoRedirect() throws Exception {
        var result=Network.inspect(base+"/app",base+"/app?token=private-value",Network.Route.parse("DIRECT"),new AtomicBoolean());
        assertEquals(200,result.status());assertEquals(3,result.observations().size());assertFalse(new ObjectMapper().writeValueAsString(result).contains("private-value"));
        assertThrows(IllegalArgumentException.class,()->Network.inspect(base+"/app",base+"/outside",Network.Route.parse("DIRECT"),new AtomicBoolean()));
        var redirect=Network.inspect(base+"/",base+"/redirect",Network.Route.parse("DIRECT"),new AtomicBoolean());assertEquals(302,redirect.status());assertEquals(2,requests.get());
        assertThrows(InterruptedException.class,()->Network.inspect(base+"/",base+"/app",Network.Route.parse("DIRECT"),new AtomicBoolean(true)));assertEquals(2,requests.get());
    }
    @Test void invalidRoutesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->Network.Route.parse("http://user:password@localhost:8080"));
        assertThrows(IllegalArgumentException.class,()->Network.Route.parse("socks5://localhost:0"));
        assertThrows(IllegalArgumentException.class,()->Network.Route.parse("https://localhost:8080/path"));
        assertEquals(java.net.Proxy.Type.SOCKS,Network.Route.parse("socks5://127.0.0.1:9050").proxy().type());
    }
    @Test void queuePersistsPauseResumeResultsAndAudits() throws Exception {
        try(Store store=new Store(dir.resolve("workspace.db"))) {
            long project=store.addProject("Fixture",base+"/app","AUTH-TEST");
            long task=store.enqueue(project,base+"/app","HTTP baseline","DIRECT",5,Instant.now());
            assertThrows(IllegalArgumentException.class,()->store.enqueue(project,base+"/app","HTTP baseline","DIRECT",5,Instant.now()));
            assertTrue(store.transition(task,"QUEUED","PAUSED","",""));
            assertFalse(store.transition(task,"QUEUED","RUNNING","",""));
            try(TaskEngine engine=new TaskEngine(store,dir)) {
                assertEquals("PAUSED",store.tasks().getFirst().status());engine.resume(task);
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                while(!store.tasks().getFirst().status().equals("COMPLETED")&&System.nanoTime()<deadline)Thread.sleep(20);
                var completed=store.tasks().getFirst();assertEquals("COMPLETED",completed.status(),completed.diagnostic());
                assertEquals(200,new ObjectMapper().readTree(completed.result()).get("status").asInt());
                assertEquals(5,store.taskEvents(task).size());assertEquals(1,requests.get());
                var observation=new ObjectMapper().treeToValue(new ObjectMapper().readTree(completed.result()).get("observations").get(0),Network.Observation.class);
                long finding=store.importObservation(task,observation);assertTrue(store.source(finding).startsWith("Task #"+task));assertEquals("Unverified",store.review(finding).verification());
                assertThrows(java.sql.SQLException.class,()->store.importObservation(task,observation));assertEquals(1,store.findings().size());
            }
        }
        try(Store reopened=new Store(dir.resolve("workspace.db"))){assertEquals("COMPLETED",reopened.tasks().getFirst().status());}
    }
    @Test void crashRecoveryRequiresExplicitRetryAndCancellationIsTerminal() throws Exception {
        try(Store store=new Store(dir.resolve("recovery.db"))) {
            long p=store.addProject("Recovery",base+"/app","AUTH-RECOVER");long task=store.enqueue(p,base+"/app","HTTP baseline","DIRECT",0,Instant.now());store.transition(task,"QUEUED","RUNNING","","");
            try(TaskEngine engine=new TaskEngine(store,dir)) {
                assertEquals("INTERRUPTED",store.tasks().getFirst().status());engine.cancel(task);assertEquals("CANCELLED",store.tasks().getFirst().status());assertThrows(IllegalArgumentException.class,()->engine.resume(task));
            }
            assertEquals(0,requests.get());
        }
    }
    @Test void failedNetworkDoesNotBecomeCompleted() throws Exception {
        int closedPort;try(java.net.ServerSocket socket=new java.net.ServerSocket(0)){closedPort=socket.getLocalPort();}
        try(Store store=new Store(dir.resolve("failed.db"))) {
            String target="http://127.0.0.1:"+closedPort+"/";long p=store.addProject("Closed port",target,"AUTH-FAIL");store.enqueue(p,target,"HTTP baseline","DIRECT",0,Instant.now());
            try(TaskEngine engine=new TaskEngine(store,dir)) {
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(!store.tasks().getFirst().status().equals("FAILED")&&System.nanoTime()<deadline)Thread.sleep(20);
                assertEquals("FAILED",store.tasks().getFirst().status());assertTrue(store.tasks().getFirst().result().isEmpty());assertFalse(store.tasks().getFirst().diagnostic().isBlank());
            }
        }
    }
    @Test void labsExerciseRealFixturesWithoutTargetRequests() throws Exception {
        assertEquals(2,Labs.run("SQL lab").get("unsafeRowCount"));assertEquals(0,Labs.run("SQL lab").get("parameterizedRowCount"));
        for(String profile:List.of("SQL lab","File lab","Script lab"))assertEquals(true,Labs.run(profile).get("passed"));assertEquals(0,requests.get());
    }
    @Test void compareChecksTargetIdentity() throws Exception {
        try(Store s=new Store(dir.resolve("compare.db"))) {
            long p=s.addProject("Compare",base+"/","AUTH-COMPARE");long a=s.enqueue(p,base+"/app","HTTP baseline","DIRECT",0,Instant.now());s.transition(a,"QUEUED","RUNNING","","");s.transition(a,"RUNNING","COMPLETED","{\"observations\":[{\"title\":\"CSP\"}]}","");
            long b=s.enqueue(p,base+"/app","HTTP baseline","DIRECT",0,Instant.now());s.transition(b,"QUEUED","RUNNING","","");s.transition(b,"RUNNING","COMPLETED","{\"observations\":[]}","");
            var tasks=s.tasks();var comparison=Guidance.compare(tasks.get(1),tasks.get(0));assertTrue(comparison.get("noLongerObserved").contains("CSP"));
        }
    }
}
