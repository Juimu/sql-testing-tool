package dev.sqlnext;

import java.util.*;

/** Deterministic local remediation guidance, explicitly not an AI verdict. */
public final class Guidance {
    public static final Map<String,String> DBMS=Map.of(
        "MySQL","Use prepared statements; minimize FILE and administrative privileges; require TLS; review secure_file_priv; restrict network bind addresses.",
        "MariaDB","Bind parameters; restrict FILE and privileged roles; require TLS; separate application and administrative users.",
        "PostgreSQL","Use bound parameters, least-privilege roles, restricted pg_hba.conf and TLS; avoid superuser application accounts.",
        "SQL Server","Use parameterized commands; limit server roles; require encrypted connections; disable unnecessary extended procedures.",
        "Oracle","Use bind variables; least-privilege schemas; review PUBLIC grants and network ACLs; keep supported patches current.",
        "SQLite","Bind parameters; restrict database-file permissions; use read-only inspection; treat database backups as sensitive assets.");
    public static String explain(Store.Finding f,Store.Review r) {
        String text=(f.title()+" "+r.cwe()).toLowerCase(Locale.ROOT);
        String advice=text.contains("sql")||text.contains("cwe-89")?"Use prepared statements and parameter binding. Restrict DB privileges. Return generic errors. Reproduce in a controlled fixture and retest the fix.":text.contains("csp")||text.contains("content-security")?"Define a Content-Security-Policy matched to application resource needs. Test a report-only deployment before enforcing it.":text.contains("tls")||text.contains("hsts")?"Validate the TLS chain and hostname, deploy supported TLS versions and consider HSTS after testing HTTPS coverage.":"Review the recorded evidence, identify the affected control, document business impact and assign an owner. Verify changes with a targeted retest.";
        return "LOCAL RULE-BASED GUIDANCE — not an AI or verification result\n\nFinding #"+f.id()+"\nRecorded severity: "+f.severity()+"\nVerification: "+r.verification()+"\n\n"+advice+"\n\nNo evidence or credentials were sent to an external service.";
    }
    public static Map<String,Set<String>> compare(Store.Assessment a,Store.Assessment b)throws Exception {
        if(a.projectId()!=b.projectId()||!a.target().equals(b.target())||!a.profile().equals(b.profile())||!a.route().equals(b.route())||!a.status().equals("COMPLETED")||!b.status().equals("COMPLETED"))throw new IllegalArgumentException("Select two completed tasks for the same project, URL, profile and route.");
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();Set<String> before=new TreeSet<>(),after=new TreeSet<>();
        for(var node:mapper.readTree(a.result()).path("observations"))before.add(node.path("title").asText());
        for(var node:mapper.readTree(b.result()).path("observations"))after.add(node.path("title").asText());
        Set<String> added=new TreeSet<>(after);added.removeAll(before);Set<String> removed=new TreeSet<>(before);removed.removeAll(after);Set<String> persistent=new TreeSet<>(before);persistent.retainAll(after);
        return Map.of("newObservations",added,"noLongerObserved",removed,"stillObserved",persistent);
    }
}
