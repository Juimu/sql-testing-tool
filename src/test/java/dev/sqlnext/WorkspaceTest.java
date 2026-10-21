package dev.sqlnext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceTest {
    @TempDir Path dir;
    @Test void scopeRejectsBoundaryAndOriginBypasses() {
        String scope="https://example.org/app";
        assertTrue(Scope.allows(scope,"https://EXAMPLE.org:443/app/orders?id=1"));
        assertTrue(Scope.allows(scope,"https://example.org/app"));
        for(String target:new String[]{"https://example.org/application","https://example.org.evil.org/app","http://example.org/app","https://example.org:8443/app","https://other.org/app"}) assertFalse(Scope.allows(scope,target),target);
        for(String target:new String[]{"file:///etc/passwd","https://user:secret@example.org/app","https://example.org/app/../private","https://example.org/app/%2e%2e/private","https://example.org/app#fragment","https://example.org:99999/app"}) assertThrows(IllegalArgumentException.class,()->Scope.parse(target),target);
        assertTrue(Scope.allows("https://example.org/","https://example.org"));
    }
    @Test void persistenceAuditAndInvalidMutations() throws Exception {
        Path file=dir.resolve("workspace.db");long finding;
        try(Store s=new Store(file)) {
            long p=s.addProject("Project","https://example.org/app","AUTH-42");
            assertThrows(IllegalArgumentException.class,()->s.addFinding(p,"https://elsewhere.org/","SQL error","High","redacted"));
            assertEquals(1,s.auditCount());
            finding=s.addFinding(p,"https://example.org/app/item","SQL error","High","redacted evidence");
            assertThrows(java.sql.SQLException.class,()->s.addFinding(p,"https://example.org/app/item","SQL error","High","duplicate"));
            assertEquals(2,s.auditCount());assertEquals(1,s.findings().size());
            s.status(finding,"Fixed");
            assertThrows(IllegalArgumentException.class,()->s.status(finding,"Verified"));
            assertThrows(IllegalArgumentException.class,()->s.status(9999,"Fixed"));
            assertEquals(3,s.auditCount());s.putSetting("theme","light");
        }
        try(Store s=new Store(file)) {assertEquals("Fixed",s.findings().getFirst().status());assertEquals("light",s.setting("theme","dark"));assertEquals(3,s.auditCount());}
    }
    @Test void reportOmitsEvidenceAndEscapesHtml() throws Exception {
        try(Store s=new Store(dir.resolve("workspace.db"))) {
            long p=s.addProject("<script>alert(1)</script>","https://example.org/","private auth note");
            s.addFinding(p,"https://example.org/item?token=private-token","<img src=x onerror=alert(1)>","Info","private evidence");
            Path json=dir.resolve("report.json"),html=dir.resolve("report.html");
            Reports.export(s,json,false);Reports.export(s,html,true);
            var parsed=new ObjectMapper().readTree(Files.readString(json));assertEquals(1,parsed.get("findings").size());
            String report=Files.readString(html);assertFalse(report.contains("<script>"));assertFalse(report.contains("<img"));assertTrue(report.contains("&lt;script&gt;"));
            for(String secret:new String[]{"private evidence","private auth note","private-token"}) assertFalse(report.contains(secret));
            assertThrows(FileAlreadyExistsException.class,()->Reports.export(s,json,false));
        }
    }
}
