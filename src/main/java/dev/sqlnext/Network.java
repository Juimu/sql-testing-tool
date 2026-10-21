package dev.sqlnext;

import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded, read-only HTTP diagnostics. Redirects and implicit proxy failover are disabled. */
public final class Network {
    public record Route(String type,String host,int port) {
        public static Route parse(String input) {
            if(input.equals("DIRECT"))return new Route("DIRECT","",0);
            try {
                URI u=URI.create(input);
                if(!List.of("http","socks5").contains(u.getScheme())||u.getHost()==null||u.getPort()<1||u.getPort()>65535||u.getRawUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||(u.getPath()!=null&&!u.getPath().isEmpty()))throw new IllegalArgumentException();
                return new Route(u.getScheme(),u.getHost(),u.getPort());
            }catch(Exception e){throw new IllegalArgumentException("Route must be DIRECT, http://host:port or socks5://host:port, without credentials.");}
        }
        public Proxy proxy(){return type.equals("DIRECT")?Proxy.NO_PROXY:new Proxy(type.equals("http")?Proxy.Type.HTTP:Proxy.Type.SOCKS,new InetSocketAddress(host,port));}
        public String label(){return type.equals("DIRECT")?"DIRECT":type+"://"+host+":"+port;}
    }
    public record Observation(String title,String severity,String confidence,String cwe,String evidence,String remediation) {}
    public record Result(String target,int status,long elapsedMs,int bytes,String server,String route,String tls,List<Observation> observations) {}
    public static Result inspect(String approved,String target,Route route,AtomicBoolean cancelled) throws Exception {
        if(!Scope.allows(approved,target))throw new IllegalArgumentException("Request outside approved scope.");
        if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedException("Cancelled");
        URI uri=Scope.parse(target);long start=System.nanoTime();
        HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection(route.proxy());
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(1000);
        c.setRequestMethod("GET");c.setRequestProperty("User-Agent","SQLNext/0.2 authorized-read-only-diagnostics");c.setRequestProperty("Accept","text/html,application/json;q=0.9,*/*;q=0.1");
        try {
            int status=c.getResponseCode();
            byte[] body;
            InputStream input=status>=400?c.getErrorStream():c.getInputStream();
            try(InputStream stream=input){
                ByteArrayOutputStream collected=new ByteArrayOutputStream();byte[] chunk=new byte[4096];
                while(stream!=null&&collected.size()<65536){
                    if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedException("Cancelled");
                    if(System.nanoTime()-start>10_000_000_000L)throw new SocketTimeoutException("Overall request deadline");
                    int n=stream.read(chunk,0,Math.min(chunk.length,65536-collected.size()));if(n<0)break;collected.write(chunk,0,n);
                }body=collected.toByteArray();
            }
            if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedException("Cancelled");
            String text=new String(body,StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            List<Observation> obs=new ArrayList<>();
            String content=Objects.toString(c.getContentType(),"");
            if(status>=200&&status<300&&content.toLowerCase(Locale.ROOT).contains("text/html")) {
                if(c.getHeaderField("Content-Security-Policy")==null)obs.add(new Observation("Content-Security-Policy header absent","Low","Observed header absence","CWE-693","No CSP header on this response.","Define an application-specific Content-Security-Policy; validate script and frame sources."));
                if(!"nosniff".equalsIgnoreCase(c.getHeaderField("X-Content-Type-Options")))obs.add(new Observation("X-Content-Type-Options nosniff absent","Low","Observed header absence","CWE-693","Response did not declare nosniff.","Return X-Content-Type-Options: nosniff and correct MIME types."));
                if(uri.getScheme().equalsIgnoreCase("https")&&c.getHeaderField("Strict-Transport-Security")==null)obs.add(new Observation("HSTS header absent","Low","Observed header absence","CWE-319","No Strict-Transport-Security header on HTTPS response.","Consider HSTS after validating HTTPS coverage and deployment requirements."));
            }
            Map<String,String> markers=Map.of("you have an error in your sql syntax","MySQL/MariaDB","pg_query():","PostgreSQL","ora-009","Oracle","unclosed quotation mark after the character string","Microsoft SQL Server","sqlite_error","SQLite");
            for(var marker:markers.entrySet())if(text.contains(marker.getKey()))obs.add(new Observation("Database error marker exposed: "+marker.getValue(),"Medium","Heuristic; review required","CWE-209","Recognized a database error marker in a bounded response. Response contents are not retained.","Use parameterized queries and generic public error messages. This marker alone does not establish SQL injection or DBMS identity."));
            String tls="Not applicable (HTTP)";
            if(c instanceof HttpsURLConnection https) {
                X509Certificate cert=(X509Certificate)https.getServerCertificates()[0];cert.checkValidity();
                tls="Certificate trust and hostname validated; expires "+cert.getNotAfter().toInstant()+"; cipher "+https.getCipherSuite();
            }
            return new Result(redactTarget(target),status,(System.nanoTime()-start)/1_000_000,body.length,redact(Objects.toString(c.getHeaderField("Server"),"Not disclosed")),route.label(),tls,List.copyOf(obs));
        }finally{c.disconnect();}
    }
    public static String redactTarget(String url) {
        URI u=Scope.parse(url);
        try{return new URI(u.getScheme(),null,u.getHost(),u.getPort(),u.getPath(),u.getQuery()==null?null:"REDACTED",null).toString();}catch(Exception e){throw new IllegalArgumentException(e);}
    }
    public static String redact(String text) {return text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").substring(0,Math.min(text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").length(),500));}
    public static String routeHealth(Route route) throws IOException {
        if(route.type().equals("DIRECT"))return "DIRECT uses the system network; test an approved URL to check reachability.";
        try(Socket socket=new Socket()) {
            socket.connect(new InetSocketAddress(route.host(),route.port()),3000);socket.setSoTimeout(3000);
            if(route.type().equals("socks5")) {
                socket.getOutputStream().write(new byte[]{5,1,0});socket.getOutputStream().flush();
                if(socket.getInputStream().read()!=5||socket.getInputStream().read()!=0)throw new IOException("SOCKS5 no-auth handshake was rejected.");
                return "SOCKS5 no-auth handshake succeeded. An approved HTTP request is still needed to verify routing. Exit IP is not verified.";
            }
            return "TCP connection succeeded. HTTP forwarding and exit IP are not yet verified; run an approved HTTP task.";
        }
    }
}
