package dev.sqlnext;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="sqlnext.uiTest",matches="true")
class AppSmokeTest {
    @TempDir Path data;
    @Test void toolkitRendersAllViews() throws Exception {
        System.setProperty("sqlnext.data",data.toString());
        System.setProperty("sqlnext.smoke","true");
        try(Store store=new Store(data.resolve("workspace.db"))) {
            long project=store.addProject("Local training lab","https://lab.example.org/app","LAB-001");
            store.addFinding(project,"https://lab.example.org/app/item","Database error exposed in response","Medium","Synthetic, redacted test evidence");
        }
        CompletableFuture<Void> done=new CompletableFuture<>();
        Platform.startup(()->{
            App app=new App();Stage stage=new Stage();
            try {
                app.start(stage);stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();
                assertTrue(stage.getScene().getRoot().getBoundsInLocal().getWidth()>0);
                if(System.getProperty("sqlnext.screenshot")!=null) {
                    var image=stage.getScene().snapshot(null);
                    var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<(int)image.getHeight();y++)for(int x=0;x<(int)image.getWidth();x++)buffered.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                    javax.imageio.ImageIO.write(buffered,"png",Path.of(System.getProperty("sqlnext.screenshot")).toFile());
                }
                stage.close();app.stop();done.complete(null);
            }
            catch(Throwable e){done.completeExceptionally(e);}
        });
        done.get(30,TimeUnit.SECONDS);
        Platform.exit();
        assertTrue(java.nio.file.Files.size(data.resolve("workspace.db"))>0);
    }
}
