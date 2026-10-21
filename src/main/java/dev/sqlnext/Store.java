package dev.sqlnext;

import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Single-user SQLite repository; all mutations and audit entries commit together. */
public final class Store implements AutoCloseable {
    public record Project(long id, String name, String scope, String authorization) { @Override public String toString() { return name; } }
    public record Finding(long id, long projectId, String target, String title, String severity, String status, String evidence, String created) {}
    private final Connection db;
    public Store(Path file) throws Exception {
        Files.createDirectories(file.toAbsolutePath().getParent());
        db = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement s = db.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON"); s.execute("PRAGMA busy_timeout=5000");
            s.execute("CREATE TABLE IF NOT EXISTS schema_version(version INTEGER PRIMARY KEY)");
            s.execute("INSERT OR IGNORE INTO schema_version VALUES(1)");
            s.execute("CREATE TABLE IF NOT EXISTS projects(id INTEGER PRIMARY KEY, name TEXT NOT NULL, scope TEXT NOT NULL, authorization TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS findings(id INTEGER PRIMARY KEY, project_id INTEGER NOT NULL REFERENCES projects(id), target TEXT NOT NULL, title TEXT NOT NULL, severity TEXT NOT NULL, status TEXT NOT NULL, evidence TEXT NOT NULL, created TEXT NOT NULL, UNIQUE(project_id,target,title))");
            s.execute("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY, event TEXT NOT NULL, entity_id INTEGER NOT NULL, created TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS settings(name TEXT PRIMARY KEY, value TEXT NOT NULL)");
        }
    }
    private static String required(String s) { if (s == null || s.isBlank()) throw new IllegalArgumentException("All fields are required."); return s.strip(); }
    private void audit(String event, long id) throws SQLException {
        try (PreparedStatement s = db.prepareStatement("INSERT INTO audit(event,entity_id,created) VALUES(?,?,?)")) { s.setString(1,event); s.setLong(2,id); s.setString(3,Instant.now().toString()); s.executeUpdate(); }
    }
    private long insert(String sql, Object... args) throws SQLException {
        try (PreparedStatement s = db.prepareStatement(sql)) {
            for (int i=0; i<args.length; i++) s.setObject(i+1,args[i]);
            s.executeUpdate();
        }
        try (Statement s = db.createStatement(); ResultSet r=s.executeQuery("SELECT last_insert_rowid()")) { return r.getLong(1); }
    }
    public long addProject(String name, String scope, String authorization) throws SQLException {
        name=required(name); authorization=required(authorization); scope=Scope.parse(scope).toString();
        db.setAutoCommit(false);
        try { long id=insert("INSERT INTO projects(name,scope,authorization) VALUES(?,?,?)",name,scope,authorization); audit("project.created",id); db.commit(); return id; }
        catch (SQLException e) { db.rollback(); throw e; } finally { db.setAutoCommit(true); }
    }
    public List<Project> projects() throws SQLException {
        List<Project> out=new ArrayList<>();
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM projects ORDER BY id DESC")) { while(r.next()) out.add(new Project(r.getLong("id"),r.getString("name"),r.getString("scope"),r.getString("authorization"))); }
        return out;
    }
    public long addFinding(long projectId, String target, String title, String severity, String evidence) throws SQLException {
        Project p=projects().stream().filter(x->x.id()==projectId).findFirst().orElseThrow(()->new IllegalArgumentException("Select a project."));
        if (!Scope.allows(p.scope(),target)) throw new IllegalArgumentException("Target is outside the project's approved scope.");
        title=required(title); evidence=required(evidence);
        if (!List.of("Critical","High","Medium","Low","Info").contains(severity)) throw new IllegalArgumentException("Invalid severity.");
        db.setAutoCommit(false);
        try { long id=insert("INSERT INTO findings(project_id,target,title,severity,status,evidence,created) VALUES(?,?,?,?,?,?,?)",projectId,target,title,severity,"Open",evidence,Instant.now().toString()); audit("finding.recorded.unverified",id); db.commit(); return id; }
        catch (SQLException e) { db.rollback(); throw e; } finally { db.setAutoCommit(true); }
    }
    public List<Finding> findings() throws SQLException {
        List<Finding> out=new ArrayList<>();
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM findings ORDER BY id DESC")) { while(r.next()) out.add(new Finding(r.getLong("id"),r.getLong("project_id"),r.getString("target"),r.getString("title"),r.getString("severity"),r.getString("status"),r.getString("evidence"),r.getString("created"))); }
        return out;
    }
    public void status(long id, String status) throws SQLException {
        if (!List.of("Open","In Progress","Fixed","Accepted Risk","Reopened").contains(status)) throw new IllegalArgumentException("Invalid status.");
        db.setAutoCommit(false);
        try (PreparedStatement s=db.prepareStatement("UPDATE findings SET status=? WHERE id=?")) {
            s.setString(1,status); s.setLong(2,id); if(s.executeUpdate()!=1) throw new IllegalArgumentException("Finding no longer exists.");
            audit("finding.status."+status,id); db.commit();
        } catch (SQLException | RuntimeException e) { db.rollback(); throw e; } finally { db.setAutoCommit(true); }
    }
    public void putSetting(String name, String value) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("INSERT INTO settings VALUES(?,?) ON CONFLICT(name) DO UPDATE SET value=excluded.value")) {s.setString(1,name);s.setString(2,value);s.executeUpdate();}
    }
    public String setting(String name, String fallback) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("SELECT value FROM settings WHERE name=?")) {s.setString(1,name);try(ResultSet r=s.executeQuery()){return r.next()?r.getString(1):fallback;}}
    }
    public int auditCount() throws SQLException {try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM audit")){return r.getInt(1);}}
    @Override public void close() throws SQLException { db.close(); }
}
