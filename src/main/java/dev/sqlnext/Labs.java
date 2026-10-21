package dev.sqlnext;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Self-contained synthetic fixtures. These modules never contact a target server. */
public final class Labs {
    public static Map<String,Object> run(String profile) throws Exception {
        return switch(profile) {
            case "SQL lab" -> sql();
            case "File lab" -> {
                String marker;try(var in=Objects.requireNonNull(Labs.class.getResourceAsStream("/lab-marker.txt"))){marker=new String(in.readNBytes(1024),StandardCharsets.UTF_8).strip();}
                yield Map.of("mode","Synthetic fixture only","file","sqlnext-lab-marker.txt","expected","SQLNEXT_HARMLESS_MARKER","actual",marker,"passed",marker.equals("SQLNEXT_HARMLESS_MARKER"),"remediation","Restrict DB file privileges and isolate service accounts. No server-side file was read.");
            }
            case "Script lab" -> {
                String input="select id from synthetic_items",actual=input.toUpperCase(Locale.ROOT);
                yield Map.of("mode","Local text transformation fixture; no WAF evasion execution","script","uppercase-fixture-v1","sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("uppercase-fixture-v1".getBytes(StandardCharsets.UTF_8))),"input",input,"output",actual,"passed",actual.equals("SELECT ID FROM SYNTHETIC_ITEMS"));
            }
            default -> throw new IllegalArgumentException("Unknown lab profile");
        };
    }
    private static Map<String,Object> sql() throws SQLException {
        try(Connection db=DriverManager.getConnection("jdbc:sqlite::memory:");Statement s=db.createStatement()) {
            s.execute("CREATE TABLE synthetic_users(id INTEGER PRIMARY KEY,name TEXT)");s.execute("INSERT INTO synthetic_users VALUES(1,'Alice'),(2,'Bob')");
            String fixture="' OR '1'='1";int unsafe=0,safe=0;
            try(ResultSet r=s.executeQuery("SELECT name FROM synthetic_users WHERE name='"+fixture+"'")){while(r.next())unsafe++;}
            try(PreparedStatement p=db.prepareStatement("SELECT name FROM synthetic_users WHERE name=?")){p.setString(1,fixture);try(ResultSet r=p.executeQuery()){while(r.next())safe++;}}
            return Map.of("mode","In-memory synthetic database; no target request","dbms","SQLite fixture","unsafeRowCount",unsafe,"parameterizedRowCount",safe,"passed",unsafe==2&&safe==0,"cwe","CWE-89","remediation","Bind untrusted input using prepared statements; never concatenate SQL.");
        }
    }
}
