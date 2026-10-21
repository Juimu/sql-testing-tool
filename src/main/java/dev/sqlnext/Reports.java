package dev.sqlnext;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class Reports {
    private Reports() {}
    private static String html(String s) { return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    public static void export(Store store, Path path, boolean html) throws Exception {
        // Evidence and authorization notes are excluded by design; review before sharing.
        var findings=store.findings().stream().map(f->Map.of("id",f.id(),"projectId",f.projectId(),"title",f.title(),"severity",f.severity(),"status",f.status(),"verification","Unverified manual entry","created",f.created())).toList();
        var projects=store.projects().stream().map(p->Map.of("id",p.id(),"name",p.name(),"scope",p.scope())).toList();
        String json=new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("generated",Instant.now().toString(),"methodology","Manual evidence registration; no automated assessment performed.","limitations","Findings are unverified. Evidence and target URLs are omitted; project scope URLs remain. Review before sharing.","projects",projects,"findings",findings));
        String content=html?"<!doctype html><html lang='en'><meta charset='utf-8'><title>SQLNext assessment report</title><style>body{max-width:960px;margin:48px auto;font:16px system-ui;background:#101827;color:#e8edf5}pre{white-space:pre-wrap;overflow-wrap:anywhere}</style><h1>SQLNext assessment report</h1><p>Manual findings register · Unverified evidence</p><pre>"+html(json)+"</pre></html>":json;
        Files.writeString(path,content,StandardOpenOption.CREATE_NEW);
    }
}
