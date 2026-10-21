package dev.sqlnext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class Reports {
    public enum Format { JSON, HTML, CSV, SARIF, PDF }
    private Reports() {}
    private static String html(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static String csv(String s) {
        // Defend spreadsheet formula execution even when the CSV field is quoted.
        String value=s.stripLeading();if(!value.isEmpty()&&"=+-@".indexOf(value.charAt(0))>=0)s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static void export(Store store,Path path,boolean html)throws Exception{export(store,path,html?Format.HTML:Format.JSON);}
    public static void export(Store store,Path path,Format format)throws Exception {
        if(Files.exists(path))throw new FileAlreadyExistsException(path.toString());
        var mapper=new ObjectMapper();var fs=store.findings();var ps=store.projects();
        List<Map<String,Object>> findings=new ArrayList<>();
        for(var f:fs){var r=store.review(f.id());findings.add(Map.of("id",f.id(),"projectId",f.projectId(),"title",f.title(),"severity",f.severity(),"status",f.status(),"verification",r.verification(),"cwe",r.cwe(),"cvss",r.cvss(),"created",f.created()));}
        var projects=ps.stream().map(p->Map.of("id",p.id(),"name",p.name(),"scope",Network.redactTarget(p.scope()))).toList();
        var tasks=store.tasks().stream().map(t->Map.of("id",t.id(),"projectId",t.projectId(),"profile",t.profile(),"status",t.status(),"updated",t.updated())).toList();
        String json=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("generated",Instant.now().toString(),"methodology","Scoped read-only HTTP diagnostics, optional sqlmap diagnostics, synthetic labs and manual review. Only recorded completed tasks establish which checks ran.","limitations","Engine observations require human review. Evidence, authorization notes, engine output and finding target URLs are omitted; project scope URLs and user titles remain. Review before sharing.","projects",projects,"findings",findings,"tasks",tasks));
        if(format==Format.PDF){pdf(store,path);return;}
        String content=switch(format) {
            case JSON -> json;
            case HTML -> "<!doctype html><html lang='en'><meta charset='utf-8'><title>SQLNext report</title><style>body{max-width:960px;margin:48px auto;font:16px system-ui;background:#101827;color:#e8edf5}pre{white-space:pre-wrap;overflow-wrap:anywhere}</style><h1>SQLNext assessment report</h1><p>Scope, findings and task summary. Review verification states.</p><pre>"+html(json)+"</pre></html>";
            case CSV -> {
                StringBuilder b=new StringBuilder("id,project_id,title,severity,status,verification,cwe,cvss,created\r\n");
                for(var f:fs){var r=store.review(f.id());b.append(f.id()).append(',').append(f.projectId()).append(',').append(csv(f.title())).append(',').append(csv(f.severity())).append(',').append(csv(f.status())).append(',').append(csv(r.verification())).append(',').append(csv(r.cwe())).append(',').append(csv(r.cvss())).append(',').append(csv(f.created())).append("\r\n");}yield b.toString();
            }
            case SARIF -> {
                var results=new ArrayList<Object>();
                for(var f:fs){var r=store.review(f.id());if(r.verification().equals("False positive"))continue;
                    String level=!r.verification().equals("Verified")?"note":List.of("Critical","High").contains(f.severity())?"error":f.severity().equals("Info")?"note":"warning";
                    results.add(Map.of("ruleId",r.cwe().isBlank()?"SQLNEXT-REVIEW":r.cwe(),"level",level,"message",Map.of("text",f.title()),"properties",Map.of("verification",r.verification(),"status",f.status(),"findingId",f.id(),"cvss",r.cvss())));
                }
                yield mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("$schema","https://json.schemastore.org/sarif-2.1.0.json","version","2.1.0","runs",List.of(Map.of("tool",Map.of("driver",Map.of("name","SQLNext","version","0.2.0")),"results",results))));
            }
            default -> throw new IllegalArgumentException();
        };
        Files.writeString(path,content,StandardOpenOption.CREATE_NEW);
    }
    private static void pdf(Store store,Path path)throws Exception {
        // Portable ASCII summary: Unicode titles and complete context remain in HTML/JSON.
        try(PDDocument doc=new PDDocument()) {
            List<String> lines=new ArrayList<>(List.of("SQLNext assessment summary", "Generated: "+Instant.now(),"Projects: "+store.projects().size()+" | Findings: "+store.findings().size()+" | Tasks: "+store.tasks().size(),"Evidence and target URLs omitted. Full Unicode titles are in HTML/JSON.","Verification is a recorded reviewer decision, not an automatic conclusion.",""));
            for(var f:store.findings())lines.add("Finding #"+f.id()+" | Project #"+f.projectId()+" | "+f.severity()+" | "+f.status()+" | "+store.review(f.id()).verification());
            PDFont font=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for(int offset=0;offset<lines.size();offset+=45){PDPage page=new PDPage();doc.addPage(page);try(PDPageContentStream c=new PDPageContentStream(doc,page)){c.beginText();c.setFont(font,10);c.newLineAtOffset(35,750);c.setLeading(15);for(String line:lines.subList(offset,Math.min(lines.size(),offset+45))){c.showText(line);c.newLine();}c.endText();}}
            try(var out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)){doc.save(out);}
        }
    }
}
