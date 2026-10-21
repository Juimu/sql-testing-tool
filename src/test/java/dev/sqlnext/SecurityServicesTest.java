package dev.sqlnext;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class SecurityServicesTest {
    @TempDir Path dir;
    @Test void vaultAuthenticatedEncryptionRejectsWrongPasswordAndTampering() throws Exception {
        Path file=dir.resolve("credentials.vault");char[] password="a strong test password".toCharArray();
        Vault.save(file,password,Map.of("example","sensitive-secret-value"));assertEquals("sensitive-secret-value",Vault.load(file,password).get("example"));
        assertFalse(new String(Files.readAllBytes(file),java.nio.charset.StandardCharsets.ISO_8859_1).contains("sensitive-secret-value"));
        assertThrows(javax.crypto.AEADBadTagException.class,()->Vault.load(file,"a different password".toCharArray()));
        byte[] changed=Files.readAllBytes(file);changed[changed.length-1]^=1;Files.write(file,changed);assertThrows(javax.crypto.AEADBadTagException.class,()->Vault.load(file,password));
        assertThrows(IllegalArgumentException.class,()->Vault.save(file,"short".toCharArray(),Map.of()));
    }
    @Test void sqliteInspectionQuotesIdentifiersAndRedactsValues() throws Exception {
        Path file=dir.resolve("fixture.db");try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=c.createStatement()){s.execute("CREATE TABLE \"odd table\"(id INTEGER PRIMARY KEY, secret TEXT)");s.execute("INSERT INTO \"odd table\" VALUES(1,'private-value')");}
        byte[] before=Files.readAllBytes(file);
        try(DatabaseInspector inspector=new DatabaseInspector(file)) {
            assertEquals(List.of("odd table"),inspector.tables());assertEquals(2,inspector.columns("odd table").size());
            var preview=inspector.redactedPreview("odd table",List.of("id","secret"));assertEquals("[REDACTED]",preview.getFirst().get("secret"));assertFalse(preview.toString().contains("private-value"));
            assertThrows(IllegalArgumentException.class,()->inspector.columns("odd table; DROP TABLE x"));assertThrows(IllegalArgumentException.class,()->inspector.redactedPreview("odd table",List.of("missing")));
        }
        assertArrayEquals(before,Files.readAllBytes(file));
    }
    @Test void reviewerStateHistoryAndValidation() throws Exception {
        try(Store s=new Store(dir.resolve("reviews.db"))) {
            long p=s.addProject("Project","https://example.org/","AUTH-REVIEW");long id=s.addFinding(p,"https://example.org/app","SQL error","Medium","Redacted");
            assertEquals("Unverified",s.review(id).verification());s.review(id,"Verified","Alice","CWE-89","8.1","Bob");assertEquals("Alice",s.review(id).reviewer());
            s.status(id,"In Progress");s.status(id,"Fixed");assertEquals(2,s.history(id).size());
            assertThrows(IllegalArgumentException.class,()->s.review(id,"Verified","Alice","CWE-89","NaN",""));assertThrows(IllegalArgumentException.class,()->s.review(id,"Verified","Alice","89","5",""));
            assertThrows(SQLException.class,()->s.review(999,"Verified","Alice","","",""));assertEquals("8.1",s.review(id).cvss());
        }
    }
    @Test void reportsAreValidAndSpreadsheetSafe() throws Exception {
        try(Store s=new Store(dir.resolve("reports.db"))) {
            long p=s.addProject("বাংলা প্রজেক্ট","https://example.org/","private auth");long id=s.addFinding(p,"https://example.org/app?secret=private","=1+2","High","private evidence");s.review(id,"Verified","Alice","CWE-89","8.1","");
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
            for(var format:Reports.Format.values()){Path path=dir.resolve("report."+format);Reports.export(s,path,format);assertTrue(Files.size(path)>20);assertThrows(FileAlreadyExistsException.class,()->Reports.export(s,path,format));}
            assertTrue(Files.readString(dir.resolve("report.CSV")).contains("\"'=1+2\""));
            var sarif=mapper.readTree(Files.readString(dir.resolve("report.SARIF")));assertEquals("2.1.0",sarif.path("version").asText());assertEquals("error",sarif.path("runs").get(0).path("results").get(0).path("level").asText());
            try(var pdf=org.apache.pdfbox.Loader.loadPDF(dir.resolve("report.PDF").toFile())){assertEquals(1,pdf.getNumberOfPages());assertTrue(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).contains("Finding #"));}
            String html=Files.readString(dir.resolve("report.HTML"));assertTrue(html.contains("বাংলা প্রজেক্ট"));assertFalse(html.contains("private evidence"));assertFalse(html.contains("private auth"));
        }
    }
    @Test void sqlmapParserSeparatesCandidatesAndNegativeRuns() {
        assertTrue(Sqlmap.parse("GET parameter 'id' is 'boolean-based blind' injectable\nback-end DBMS: SQLite",0).candidate());assertFalse(Sqlmap.parse("all tested parameters do not appear to be injectable",0).candidate());assertEquals("SQLite",Sqlmap.parse("back-end DBMS: SQLite",0).dbms());
    }
    @Test void supervisedProcessHasDeadlineAndHandlesNonzeroExit() throws Exception {
        String javaBinary=Path.of(System.getProperty("java.home"),"bin","java").toString();
        assertTrue(Sqlmap.run(List.of(javaBinary,"-version"),Duration.ofSeconds(10),new AtomicBoolean()).contains("version"));
        assertThrows(java.io.IOException.class,()->Sqlmap.run(List.of(javaBinary,"--invalid-sqlnext-option"),Duration.ofSeconds(10),new AtomicBoolean()));
        assertThrows(InterruptedException.class,()->Sqlmap.run(List.of(javaBinary,"-version"),Duration.ofSeconds(10),new AtomicBoolean(true)));
    }
    @Test void externalAiRequiresApprovalAndNeverIncludesEvidenceOrTargets() throws Exception {
        var finding=new Store.Finding(1,1,"https://example.org/app?token=private","SQL error","Medium","Open","private evidence","2026-10-22T00:00:00Z");
        var verified=new Store.Review("Verified","Alice","CWE-89","5.0","Bob","");
        java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        AiClient client=new AiClient((uri,token,body)->{calls.incrementAndGet();assertEquals("private-api-key",token);assertFalse(body.contains("private evidence"));assertFalse(body.contains("example.org"));assertTrue(body.contains("CWE-89"));return "{\"choices\":[{\"message\":{\"content\":\"Use prepared statements.\"}}]}";});
        assertThrows(IllegalArgumentException.class,()->client.explain(finding,verified,"https://api.example.org/v1/chat/completions","model","private-api-key",false));
        assertThrows(IllegalArgumentException.class,()->client.explain(finding,new Store.Review("Unverified","","","","",""),"https://api.example.org/v1/chat/completions","model","private-api-key",true));
        assertThrows(IllegalArgumentException.class,()->client.explain(finding,verified,"http://api.example.org/v1/chat/completions","model","private-api-key",true));
        assertEquals(0,calls.get());assertTrue(client.explain(finding,verified,"https://api.example.org/v1/chat/completions","model","private-api-key",true).contains("AI-GENERATED DRAFT"));assertEquals(1,calls.get());
    }
    @Test void versionOneWorkspaceCanUpgradeWithoutLosingData() throws Exception {
        Path file=dir.resolve("legacy.db");
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+file);Statement s=db.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER PRIMARY KEY)");s.execute("INSERT INTO schema_version VALUES(1)");
            s.execute("CREATE TABLE projects(id INTEGER PRIMARY KEY,name TEXT NOT NULL,scope TEXT NOT NULL,authorization TEXT NOT NULL)");s.execute("INSERT INTO projects VALUES(1,'Existing project','https://example.org/','AUTH-LEGACY')");
        }
        try(Store s=new Store(file)){assertEquals("Existing project",s.projects().getFirst().name());assertTrue(s.tasks().isEmpty());s.enqueue(1,"https://example.org/","SQL lab","DIRECT",0,java.time.Instant.now());}
        try(Store s=new Store(file)){assertEquals(1,s.projects().size());assertEquals(1,s.tasks().size());}
    }
    public static class ProcessFixture {
        public static void main(String[] args)throws Exception {
            if(args.length>0&&args[0].equals("child")){Thread.sleep(30000);return;}
            String javaBinary=Path.of(System.getProperty("java.home"),"bin","java").toString();
            Process child=new ProcessBuilder(javaBinary,"-cp",System.getProperty("java.class.path"),ProcessFixture.class.getName(),"child").start();
            Files.writeString(Path.of(args[0]),Long.toString(child.pid()));Thread.sleep(30000);
        }
    }
    @Test void cancellationTerminatesTheOwnedProcessTree() throws Exception {
        Path pid=dir.resolve("child.pid");AtomicBoolean cancelled=new AtomicBoolean();
        Thread signal=Thread.ofPlatform().daemon().start(()->{try{long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);while(!Files.exists(pid)&&System.nanoTime()<deadline)Thread.sleep(20);cancelled.set(true);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        String javaBinary=Path.of(System.getProperty("java.home"),"bin","java").toString();
        try {assertThrows(InterruptedException.class,()->Sqlmap.run(List.of(javaBinary,"-cp",System.getProperty("java.class.path"),ProcessFixture.class.getName(),pid.toString()),Duration.ofSeconds(15),cancelled));}
        finally{signal.interrupt();}
        assertTrue(Files.exists(pid));long child=Long.parseLong(Files.readString(pid));
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);while(ProcessHandle.of(child).map(ProcessHandle::isAlive).orElse(false)&&System.nanoTime()<deadline)Thread.sleep(20);
        assertFalse(ProcessHandle.of(child).map(ProcessHandle::isAlive).orElse(false),"Owned descendant process survived cancellation");
    }
    @Test void resultGroupsAppendPersistAndDeleteWithAudit() throws Exception {
        Path file=dir.resolve("results.db");long first,second;
        try(Store s=new Store(file)){long project=s.addProject("Results","https://example.org/","AUTH-RESULTS");first=s.addResultGroup(project,"local.db","users",1,"[{\"name\":\"[REDACTED]\"}]");second=s.addResultGroup(project,"local.db","users",0,"[]");assertNotEquals(first,second);assertEquals(2,s.resultGroups().size());}
        try(Store s=new Store(file)){assertEquals(2,s.resultGroups().size());s.deleteResultGroup(first);assertEquals(second,s.resultGroups().getFirst().id());assertTrue(s.audits().stream().anyMatch(a->a.event().equals("result.deleted")));assertThrows(IllegalArgumentException.class,()->s.deleteResultGroup(first));assertEquals(1,s.resultGroups().size());}
    }
    @Test void onlyOneApplicationCanOwnTheWorkspace() throws Exception {
        try(WorkspaceLock first=new WorkspaceLock(dir)){assertThrows(java.io.IOException.class,()->new WorkspaceLock(dir));}
        try(WorkspaceLock afterRelease=new WorkspaceLock(dir)){assertNotNull(afterRelease);}
    }
}
