package dev.sqlnext;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** Optional real-engine integration against an isolated in-memory vulnerable fixture only. */
@EnabledIfSystemProperty(named="sqlnext.sqlmap.script",matches=".+")
class SqlmapIntegrationTest {
    @TempDir Path dir;
    @Test @Timeout(220) void realEngineFindsTheSyntheticBooleanFixture() throws Exception {
        try(Connection db=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try(Statement s=db.createStatement()){s.execute("CREATE TABLE products(id INTEGER,name TEXT)");s.execute("INSERT INTO products VALUES(1,'Alice'),(2,'Bob')");}
            HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/lab",exchange->{
                String query=exchange.getRequestURI().getRawQuery(),value="1";
                if(query!=null)for(String part:query.split("&")){String[] pair=part.split("=",2);if(pair[0].equals("id"))value=URLDecoder.decode(pair.length>1?pair[1]:"",java.nio.charset.StandardCharsets.UTF_8);}
                String body="<html><h1>Synthetic product fixture</h1>";int status=200;
                synchronized(db){try(Statement s=db.createStatement();ResultSet rows=s.executeQuery("SELECT name FROM products WHERE id="+value)){while(rows.next())body+="<p>Product: "+rows.getString(1)+"</p>";}catch(SQLException e){body+="SQLITE_ERROR: "+e.getMessage();status=500;}}
                body+="</html>";byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");exchange.sendResponseHeaders(status,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}
            });server.start();
            try {
                String scope="http://127.0.0.1:"+server.getAddress().getPort()+"/lab";
                var result=Sqlmap.assess(scope,scope+"?id=1",System.getProperty("sqlnext.python.path"),System.getProperty("sqlnext.sqlmap.script"),"DIRECT",dir.resolve("engine-output"),new AtomicBoolean());
                assertTrue(result.candidate(),result.summary());assertEquals(0,result.exitCode());
            }finally{server.stop(0);}
        }
    }
}
