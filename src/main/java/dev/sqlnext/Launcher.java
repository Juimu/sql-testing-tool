package dev.sqlnext;

import java.nio.file.*;
import java.util.*;

/** Classpath/native-image entry point, with a display-free packaged-runtime self-test. */
public final class Launcher {
    public static void main(String[] args)throws Exception {
        if(Arrays.asList(args).contains("--self-test")) {
            Path temporary=Files.createTempDirectory("sqlnext-self-test-");
            try {
                try(Store store=new Store(temporary.resolve("workspace.db"))) {
                    long p=store.addProject("Self test","https://example.org/app","SYNTHETIC-ONLY");store.addFinding(p,"https://example.org/app/item","Synthetic observation","Info","Synthetic redacted evidence");
                    if(!Boolean.TRUE.equals(Labs.run("SQL lab").get("passed")))throw new IllegalStateException("SQL fixture failed");
                    Reports.export(store,temporary.resolve("report.json"),Reports.Format.JSON);
                    if(store.findings().size()!=1)throw new IllegalStateException("Persistence failed");
                }
                System.out.println("SQLNext packaged-runtime self-test passed");
            }finally{try(var files=Files.walk(temporary)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
            return;
        }
        App.main(args);
    }
}
