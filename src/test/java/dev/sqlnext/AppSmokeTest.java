package dev.sqlnext;

import javafx.application.Platform;
import javafx.scene.control.*;
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
    record Window(App app,Stage stage) {}
    private static <T> T fx(Callable<T> action)throws Exception {
        CompletableFuture<T> done=new CompletableFuture<>();Platform.runLater(()->{try{done.complete(action.call());}catch(Throwable e){done.completeExceptionally(e);}});return done.get(30,TimeUnit.SECONDS);
    }
    private static Button button(Stage stage,String title){return stage.getScene().getRoot().lookupAll(".button").stream().filter(n->n instanceof Button b&&b.getText().equals(title)).map(n->(Button)n).findFirst().orElseThrow();}
    @Test void toolkitRendersViewsAndQueuesALabFromTheInterface() throws Exception {
        System.setProperty("sqlnext.data",data.toString());System.setProperty("sqlnext.smoke","true");
        try(Store store=new Store(data.resolve("workspace.db"))) {
            long project=store.addProject("Local training lab","https://lab.example.org/app","LAB-001");store.putSetting("selected.project",Long.toString(project));
            store.addFinding(project,"https://lab.example.org/app/item","Database error exposed in response","Medium","Synthetic, redacted test evidence");
        }
        CompletableFuture<Void> started=new CompletableFuture<>();Platform.startup(()->{Platform.setImplicitExit(false);started.complete(null);});started.get(10,TimeUnit.SECONDS);
        Window window=fx(()->{App app=new App();Stage stage=new Stage();app.start(stage);return new Window(app,stage);});
        try {
            fx(()->{
                Stage stage=window.stage();button(stage,"Single / batch assessment").fire();stage.getScene().getRoot().applyCss();
                @SuppressWarnings("unchecked") ComboBox<String> profile=(ComboBox<String>)stage.getScene().getRoot().lookupAll(".combo-box").stream().filter(n->"Assessment profile".equals(n.getAccessibleText())).findFirst().orElseThrow();profile.setValue("SQL lab");
                TextArea targets=(TextArea)stage.getScene().getRoot().lookupAll(".text-area").stream().filter(n->"Assessment target URLs".equals(n.getAccessibleText())).findFirst().orElseThrow();targets.setText("https://lab.example.org/app/item");
                CheckBox authorized=(CheckBox)stage.getScene().getRoot().lookup(".check-box");authorized.setSelected(true);button(stage,"Validate & queue").fire();return null;
            });
            try(Store verifier=new Store(data.resolve("workspace.db"))) {
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(verifier.tasks().stream().noneMatch(t->t.status().equals("COMPLETED"))&&System.nanoTime()<deadline)Thread.sleep(20);
                assertEquals(1,verifier.tasks().size());assertEquals("COMPLETED",verifier.tasks().getFirst().status());assertTrue(verifier.tasks().getFirst().result().contains("parameterizedRowCount"));
            }
            fx(()->{
                Stage stage=window.stage();button(stage,"Overview").fire();stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();assertTrue(stage.getScene().getRoot().getBoundsInLocal().getWidth()>0);
                if(System.getProperty("sqlnext.screenshot")!=null){var image=stage.getScene().snapshot(null);var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);for(int y=0;y<(int)image.getHeight();y++)for(int x=0;x<(int)image.getWidth();x++)buffered.setRGB(x,y,image.getPixelReader().getArgb(x,y));javax.imageio.ImageIO.write(buffered,"png",Path.of(System.getProperty("sqlnext.screenshot")).toFile());}
                return null;
            });
        }finally{fx(()->{window.stage().close();window.app().stop();return null;});Platform.exit();}
    }
}
