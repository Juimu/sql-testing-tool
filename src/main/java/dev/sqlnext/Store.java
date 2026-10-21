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
            s.execute("CREATE TABLE IF NOT EXISTS tasks(id INTEGER PRIMARY KEY,project_id INTEGER NOT NULL REFERENCES projects(id),target TEXT NOT NULL,profile TEXT NOT NULL,route TEXT NOT NULL,status TEXT NOT NULL,priority INTEGER NOT NULL,due TEXT NOT NULL,created TEXT NOT NULL,updated TEXT NOT NULL,result TEXT NOT NULL DEFAULT '',diagnostic TEXT NOT NULL DEFAULT '')");
            s.execute("CREATE TABLE IF NOT EXISTS task_events(id INTEGER PRIMARY KEY,task_id INTEGER NOT NULL REFERENCES tasks(id),state TEXT NOT NULL,created TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS finding_reviews(finding_id INTEGER PRIMARY KEY REFERENCES findings(id),verification TEXT NOT NULL,reviewer TEXT NOT NULL,cwe TEXT NOT NULL,cvss TEXT NOT NULL,owner TEXT NOT NULL,updated TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS finding_history(id INTEGER PRIMARY KEY,finding_id INTEGER NOT NULL REFERENCES findings(id),status TEXT NOT NULL,created TEXT NOT NULL)");
            s.execute("INSERT OR IGNORE INTO schema_version VALUES(2)");
            s.execute("CREATE TABLE IF NOT EXISTS result_groups(id INTEGER PRIMARY KEY,project_id INTEGER NOT NULL REFERENCES projects(id),source TEXT NOT NULL,label TEXT NOT NULL,row_count INTEGER NOT NULL,content TEXT NOT NULL,created TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS finding_sources(finding_id INTEGER PRIMARY KEY REFERENCES findings(id),task_id INTEGER NOT NULL REFERENCES tasks(id),confidence TEXT NOT NULL)");
            s.execute("INSERT OR IGNORE INTO schema_version VALUES(3)");
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
    public synchronized long addProject(String name, String scope, String authorization) throws SQLException {
        name=required(name); authorization=required(authorization); scope=Scope.parse(scope).toString();
        db.setAutoCommit(false);
        try { long id=insert("INSERT INTO projects(name,scope,authorization) VALUES(?,?,?)",name,scope,authorization); audit("project.created",id); db.commit(); return id; }
        catch (SQLException e) { db.rollback(); throw e; } finally { db.setAutoCommit(true); }
    }
    public synchronized List<Project> projects() throws SQLException {
        List<Project> out=new ArrayList<>();
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM projects ORDER BY id DESC")) { while(r.next()) out.add(new Project(r.getLong("id"),r.getString("name"),r.getString("scope"),r.getString("authorization"))); }
        return out;
    }
    public synchronized long addFinding(long projectId, String target, String title, String severity, String evidence) throws SQLException {
        Project p=projects().stream().filter(x->x.id()==projectId).findFirst().orElseThrow(()->new IllegalArgumentException("Select a project."));
        if (!Scope.allows(p.scope(),target)) throw new IllegalArgumentException("Target is outside the project's approved scope.");
        title=required(title); evidence=required(evidence);
        if (!List.of("Critical","High","Medium","Low","Info").contains(severity)) throw new IllegalArgumentException("Invalid severity.");
        db.setAutoCommit(false);
        try { long id=insert("INSERT INTO findings(project_id,target,title,severity,status,evidence,created) VALUES(?,?,?,?,?,?,?)",projectId,target,title,severity,"Open",evidence,Instant.now().toString()); audit("finding.recorded.unverified",id); db.commit(); return id; }
        catch (SQLException e) { db.rollback(); throw e; } finally { db.setAutoCommit(true); }
    }
    public synchronized List<Finding> findings() throws SQLException {
        List<Finding> out=new ArrayList<>();
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM findings ORDER BY id DESC")) { while(r.next()) out.add(new Finding(r.getLong("id"),r.getLong("project_id"),r.getString("target"),r.getString("title"),r.getString("severity"),r.getString("status"),r.getString("evidence"),r.getString("created"))); }
        return out;
    }
    public synchronized void status(long id, String status) throws SQLException {
        if (!List.of("Open","In Progress","Fixed","Accepted Risk","Reopened").contains(status)) throw new IllegalArgumentException("Invalid status.");
        db.setAutoCommit(false);
        try (PreparedStatement s=db.prepareStatement("UPDATE findings SET status=? WHERE id=?")) {
            s.setString(1,status); s.setLong(2,id); if(s.executeUpdate()!=1) throw new IllegalArgumentException("Finding no longer exists.");
            insert("INSERT INTO finding_history(finding_id,status,created) VALUES(?,?,?)",id,status,Instant.now().toString()); audit("finding.status."+status,id); db.commit();
        } catch (SQLException | RuntimeException e) { db.rollback(); throw e; } finally { db.setAutoCommit(true); }
    }
    public synchronized void putSetting(String name, String value) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("INSERT INTO settings VALUES(?,?) ON CONFLICT(name) DO UPDATE SET value=excluded.value")) {s.setString(1,name);s.setString(2,value);s.executeUpdate();}
    }
    public synchronized String setting(String name, String fallback) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("SELECT value FROM settings WHERE name=?")) {s.setString(1,name);try(ResultSet r=s.executeQuery()){return r.next()?r.getString(1):fallback;}}
    }
    public synchronized int auditCount() throws SQLException {try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM audit")){return r.getInt(1);}}
    public record Assessment(long id,long projectId,String target,String profile,String route,String status,int priority,String due,String created,String updated,String result,String diagnostic) {}
    public record Review(String verification,String reviewer,String cwe,String cvss,String owner,String updated) {}
    public record Audit(long id,String event,long entityId,String created) {}
    public synchronized List<Assessment> tasks() throws SQLException {
        List<Assessment> out=new ArrayList<>();
        try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT * FROM tasks ORDER BY priority DESC,id DESC")) {
            while(r.next())out.add(new Assessment(r.getLong("id"),r.getLong("project_id"),r.getString("target"),r.getString("profile"),r.getString("route"),r.getString("status"),r.getInt("priority"),r.getString("due"),r.getString("created"),r.getString("updated"),r.getString("result"),r.getString("diagnostic")));
        }return out;
    }
    public synchronized long enqueue(long project,String target,String profile,String route,int priority,Instant due) throws SQLException {
        var p=projects().stream().filter(x->x.id()==project).findFirst().orElseThrow(()->new IllegalArgumentException("Select a project."));
        if(!Scope.allows(p.scope(),target))throw new IllegalArgumentException("Target is outside approved scope.");
        if(!List.of("HTTP baseline","Endpoint exposure","SQL lab","File lab","Script lab","sqlmap diagnostic").contains(profile))throw new IllegalArgumentException("Unknown assessment profile.");
        Network.Route.parse(route);
        if(tasks().stream().anyMatch(t->t.projectId()==project && t.target().equals(target) && t.profile().equals(profile) && List.of("QUEUED","RUNNING","PAUSED").contains(t.status())))throw new IllegalArgumentException("An unfinished task already exists for this URL/profile.");
        String now=Instant.now().toString();db.setAutoCommit(false);
        try {long id=insert("INSERT INTO tasks(project_id,target,profile,route,status,priority,due,created,updated) VALUES(?,?,?,?,?,?,?,?,?)",project,target,profile,route,"QUEUED",priority,due.toString(),now,now);insert("INSERT INTO task_events(task_id,state,created) VALUES(?,?,?)",id,"QUEUED",now);audit("task.queued",id);db.commit();return id;}
        catch(SQLException e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
    }
    public synchronized boolean transition(long id,String expected,String next,String result,String diagnostic) throws SQLException {
        Map<String,List<String>> allowed=Map.of("QUEUED",List.of("RUNNING","PAUSED","CANCELLED"),"RUNNING",List.of("COMPLETED","FAILED","CANCELLED","INTERRUPTED"),"PAUSED",List.of("QUEUED","CANCELLED"),"FAILED",List.of("QUEUED"),"INTERRUPTED",List.of("QUEUED","CANCELLED"));
        if(!allowed.getOrDefault(expected,List.of()).contains(next))throw new IllegalArgumentException("Unsupported task transition.");
        db.setAutoCommit(false);
        try(PreparedStatement s=db.prepareStatement("UPDATE tasks SET status=?,result=?,diagnostic=?,updated=? WHERE id=? AND status=?")) {
            String now=Instant.now().toString();s.setString(1,next);s.setString(2,result);s.setString(3,diagnostic);s.setString(4,now);s.setLong(5,id);s.setString(6,expected);
            if(s.executeUpdate()==0){db.rollback();return false;}
            insert("INSERT INTO task_events(task_id,state,created) VALUES(?,?,?)",id,next,now);audit("task."+next.toLowerCase(Locale.ROOT),id);db.commit();return true;
        }catch(SQLException e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
    }
    public synchronized List<String> taskEvents(long id) throws SQLException {
        var out=new ArrayList<String>();try(PreparedStatement s=db.prepareStatement("SELECT created,state FROM task_events WHERE task_id=? ORDER BY id")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){while(r.next())out.add(r.getString(1)+"  "+r.getString(2));}}return out;
    }
    public synchronized void review(long id,String verification,String reviewer,String cwe,String cvss,String owner) throws SQLException {
        if(!List.of("Unverified","Verified","False positive").contains(verification))throw new IllegalArgumentException("Invalid verification state.");
        reviewer=required(reviewer);
        if(!cwe.isBlank() && !cwe.matches("CWE-[0-9]+"))throw new IllegalArgumentException("CWE must look like CWE-89.");
        if(!cvss.isBlank()){double score=Double.parseDouble(cvss);if(!Double.isFinite(score)||score<0||score>10)throw new IllegalArgumentException("CVSS score must be between 0 and 10. Enter a score independently assessed by a reviewer.");}
        db.setAutoCommit(false);
        try {insert("INSERT INTO finding_reviews VALUES(?,?,?,?,?,?,?) ON CONFLICT(finding_id) DO UPDATE SET verification=excluded.verification,reviewer=excluded.reviewer,cwe=excluded.cwe,cvss=excluded.cvss,owner=excluded.owner,updated=excluded.updated",id,verification,reviewer,cwe,cvss,owner,Instant.now().toString());audit("finding.review."+verification,id);db.commit();}
        catch(SQLException e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
    }
    public synchronized Review review(long id) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("SELECT * FROM finding_reviews WHERE finding_id=?")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?new Review(r.getString("verification"),r.getString("reviewer"),r.getString("cwe"),r.getString("cvss"),r.getString("owner"),r.getString("updated")):new Review("Unverified","","","","","");}}
    }
    public synchronized List<String> history(long id) throws SQLException {
        var out=new ArrayList<String>();try(PreparedStatement s=db.prepareStatement("SELECT created,status FROM finding_history WHERE finding_id=? ORDER BY id")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){while(r.next())out.add(r.getString(1)+"  "+r.getString(2));}}return out;
    }
    public synchronized List<Audit> audits() throws SQLException {
        var out=new ArrayList<Audit>();try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT * FROM audit ORDER BY id DESC")){while(r.next())out.add(new Audit(r.getLong("id"),r.getString("event"),r.getLong("entity_id"),r.getString("created")));}return out;
    }
    public record ResultGroup(long id,long projectId,String source,String label,int rows,String content,String created) {}
    public synchronized List<ResultGroup> resultGroups()throws SQLException {
        var out=new ArrayList<ResultGroup>();try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT * FROM result_groups ORDER BY id DESC")){while(r.next())out.add(new ResultGroup(r.getLong("id"),r.getLong("project_id"),r.getString("source"),r.getString("label"),r.getInt("row_count"),r.getString("content"),r.getString("created")));}return out;
    }
    public synchronized long addResultGroup(long project,String source,String label,int rows,String content)throws SQLException {
        required(source);required(label);required(content);if(rows<0||content.length()>1_048_576)throw new IllegalArgumentException("Invalid result group size.");
        db.setAutoCommit(false);try{long id=insert("INSERT INTO result_groups(project_id,source,label,row_count,content,created) VALUES(?,?,?,?,?,?)",project,source,label,rows,content,Instant.now().toString());audit("result.appended",id);db.commit();return id;}
        catch(SQLException e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
    }
    public synchronized void deleteResultGroup(long id)throws SQLException {
        db.setAutoCommit(false);try(PreparedStatement s=db.prepareStatement("DELETE FROM result_groups WHERE id=?")){s.setLong(1,id);if(s.executeUpdate()!=1)throw new IllegalArgumentException("Result group no longer exists.");audit("result.deleted",id);db.commit();}
        catch(SQLException|RuntimeException e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
    }
    public synchronized String source(long finding)throws SQLException {
        try(PreparedStatement s=db.prepareStatement("SELECT task_id,confidence FROM finding_sources WHERE finding_id=?")){s.setLong(1,finding);try(ResultSet r=s.executeQuery()){return r.next()?"Task #"+r.getLong(1)+" · "+r.getString(2):"Manual entry";}}
    }
    public synchronized long importObservation(long taskId,Network.Observation observation)throws SQLException {
        var task=tasks().stream().filter(t->t.id()==taskId).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown task."));
        if(!task.status().equals("COMPLETED")||!List.of("HTTP baseline","Endpoint exposure").contains(task.profile()))throw new IllegalArgumentException("Import observations from completed HTTP tasks only.");
        var project=projects().stream().filter(p->p.id()==task.projectId()).findFirst().orElseThrow();if(!Scope.allows(project.scope(),task.target()))throw new IllegalArgumentException("Task target is no longer in scope.");
        if(!List.of("Critical","High","Medium","Low","Info").contains(observation.severity()))throw new IllegalArgumentException("Invalid severity.");
        String now=Instant.now().toString(),title=required(observation.title());
        db.setAutoCommit(false);try {
            long id=insert("INSERT INTO findings(project_id,target,title,severity,status,evidence,created) VALUES(?,?,?,?,?,?,?)",task.projectId(),task.target(),title,observation.severity(),"Open",observation.evidence()+"\nRemediation: "+observation.remediation(),now);
            insert("INSERT INTO finding_sources VALUES(?,?,?)",id,taskId,observation.confidence());insert("INSERT INTO finding_reviews VALUES(?,?,?,?,?,?,?)",id,"Unverified","Engine import",observation.cwe(),"","",now);audit("finding.imported.unverified",id);db.commit();return id;
        }catch(SQLException|RuntimeException e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
    }
    @Override public synchronized void close() throws SQLException { db.close(); }
}
