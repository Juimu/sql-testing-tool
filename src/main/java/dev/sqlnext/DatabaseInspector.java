package dev.sqlnext;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Explicitly selected local SQLite database, opened read-only. Values are redacted. */
public final class DatabaseInspector implements AutoCloseable {
    public record Column(String name,String type,boolean nullable,boolean primaryKey) {}
    private final Connection db;
    public DatabaseInspector(Path file) throws Exception {
        if(!Files.isRegularFile(file))throw new IllegalArgumentException("Select an existing SQLite database.");
        db=DriverManager.getConnection("jdbc:sqlite:"+file.toAbsolutePath().toUri()+"?mode=ro");
        try(Statement s=db.createStatement()){s.execute("PRAGMA query_only=ON");s.execute("PRAGMA busy_timeout=3000");}
    }
    public List<String> tables() throws SQLException {
        var out=new ArrayList<String>();try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT name FROM sqlite_schema WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())out.add(r.getString(1));}return out;
    }
    private String table(String name) throws SQLException {if(!tables().contains(name))throw new IllegalArgumentException("Unknown table.");return "\""+name.replace("\"","\"\"")+"\"";}
    public List<Column> columns(String name) throws SQLException {
        var out=new ArrayList<Column>();try(Statement s=db.createStatement();ResultSet r=s.executeQuery("PRAGMA table_info("+table(name)+")")){while(r.next())out.add(new Column(r.getString("name"),r.getString("type"),r.getInt("notnull")==0,r.getInt("pk")>0));}return out;
    }
    public List<Map<String,String>> redactedPreview(String name,List<String> selected) throws SQLException {
        var allowed=columns(name).stream().map(Column::name).toList();
        if(selected.isEmpty()||!allowed.containsAll(selected))throw new IllegalArgumentException("Select existing columns.");
        String names=selected.stream().map(c->"\""+c.replace("\"","\"\"")+"\"").collect(java.util.stream.Collectors.joining(","));
        var out=new ArrayList<Map<String,String>>();
        try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT "+names+" FROM "+table(name)+" LIMIT 20")) {
            while(r.next()){var row=new LinkedHashMap<String,String>();for(int i=0;i<selected.size();i++){r.getObject(i+1);row.put(selected.get(i),r.wasNull()?"NULL":"[REDACTED]");}out.add(row);}
        }return out;
    }
    @Override public void close() throws SQLException {db.close();}
}
