package dev.sqlnext;

import java.nio.file.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/** Optional installed sqlmap adapter, with bounded diagnostics and no data extraction flags. */
public final class Sqlmap {
    public record Detection(boolean candidate,String dbms,int exitCode,String summary) {}
    private static final Pattern CANDIDATE=Pattern.compile("(?im)(?:parameter .+ (?:is vulnerable|appears to be .+ injectable)|(?:GET|POST) parameter .+ is .+ injectable)");
    public static Detection parse(String output,int exit) {
        String text=output.replaceAll("\\u001B\\[[;\\d]*m", "");
        String dbms="Not established";
        var matcher=Pattern.compile("(?im)back-end DBMS:\\s*([^\\r\\n]+)").matcher(text);
        if(matcher.find())dbms=Network.redact(matcher.group(1));
        boolean candidate=CANDIDATE.matcher(text).find();
        return new Detection(candidate,dbms,exit,exit!=0?"Engine failed; no conclusive assessment.":candidate?"Engine reported a possible injection. Independent reviewer verification is required.":"No positive engine signature recognized. This is not proof that the target is secure.");
    }
    public static Detection assess(String approved,String target,String python,String script,String route,Path output,AtomicBoolean cancelled) throws Exception {
        if(!Scope.allows(approved,target))throw new IllegalArgumentException("Target outside scope.");
        if(!Files.isRegularFile(Path.of(script))||!Files.isRegularFile(Path.of(python)))throw new IllegalArgumentException("Configure valid Python executable and sqlmap.py paths in Settings.");
        Network.Route proxy=Network.Route.parse(route);
        List<String> args=new ArrayList<>(List.of(python,script,"-u",target,"--batch","--risk=1","--level=1","--technique=BE","--threads=1","--timeout=5","--retries=0","--delay=1","--disable-coloring","--ignore-redirects","--output-dir="+output.toAbsolutePath()));
        if(!proxy.type().equals("DIRECT"))args.add("--proxy="+proxy.label());
        String text=run(args,Duration.ofMinutes(3),cancelled);
        if((text.contains("[CRITICAL]")||text.contains("[ERROR]"))&&!text.contains("all tested parameters do not appear to be injectable"))throw new IOException("Engine reported an incomplete or failed assessment.");
        return parse(text,0);
    }
    static String run(List<String> command,Duration timeout,AtomicBoolean cancelled) throws Exception {
        if(cancelled.get())throw new InterruptedException();
        Process process=new ProcessBuilder(command).redirectErrorStream(true).start();
        ExecutorService reader=Executors.newSingleThreadExecutor();
        Future<String> text=reader.submit(()->{
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            try(InputStream in=process.getInputStream()){while((n=in.read(buffer))!=-1){int keep=Math.min(n,1_048_576-out.size());if(keep>0)out.write(buffer,0,keep);}}
            return out.toString(java.nio.charset.StandardCharsets.UTF_8);
        });
        long deadline=System.nanoTime()+timeout.toNanos();
        try {
            while(!process.waitFor(100,TimeUnit.MILLISECONDS)) {
                if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedException("Process cancelled");
                if(System.nanoTime()>deadline)throw new TimeoutException("Engine deadline exceeded");
            }
            String output=text.get(5,TimeUnit.SECONDS);
            if(process.exitValue()!=0)throw new IOException("Assessment engine exited unsuccessfully ("+process.exitValue()+").");
            return output;
        }finally {
            // Stop descendants before the parent so its process reaper can collect them.
            // Force only survivors, then give the parent a short chance to reap those too.
            boolean interrupted=Thread.interrupted();
            var children=process.descendants().toList();
            try {
                children.forEach(ProcessHandle::destroy);
                awaitChildren(children,500);
                for(var child:children)if(child.isAlive())child.destroyForcibly();
                awaitChildren(children,1500);
                process.destroy();if(!process.waitFor(500,TimeUnit.MILLISECONDS))process.destroyForcibly();
                process.waitFor(2,TimeUnit.SECONDS);
            }finally{reader.shutdownNow();if(interrupted)Thread.currentThread().interrupt();}
        }
    }
    private static void awaitChildren(List<ProcessHandle> children,long milliseconds)throws InterruptedException {
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(milliseconds);
        while(children.stream().anyMatch(ProcessHandle::isAlive)&&System.nanoTime()<deadline)Thread.sleep(20);
    }
}
