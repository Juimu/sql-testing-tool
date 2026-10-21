package dev.sqlnext;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

public final class App extends Application {
    private Store store;
    private WorkspaceLock workspaceLock;
    private TaskEngine engine;
    private Timeline timer;
    private TableView<Store.Assessment> taskRows;
    private final java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"sqlnext-background");t.setDaemon(true);return t;});
    private String findingSearch="";
    private BorderPane root;
    private Scene scene;
    private Stage stage;
    private Path data;
    private final Label notice=new Label("Scoped diagnostics · Explicit project authorization · Local evidence");
    private interface Action { void run() throws Exception; }
    private void attempt(Action action) { try {action.run();} catch(Exception e) { new Alert(Alert.AlertType.ERROR, Objects.toString(e.getMessage(),"Operation failed."),ButtonType.OK).showAndWait(); } }
    private Button button(String text, Action action) {Button b=new Button(text);b.setOnAction(e->attempt(action));return b;}
    private VBox page(String title,String description, javafx.scene.Node... nodes) {
        Label heading=new Label(title);heading.getStyleClass().add("heading");
        Label sub=new Label(description);sub.setWrapText(true);sub.getStyleClass().add("muted");
        VBox box=new VBox(18,heading,sub);box.getChildren().addAll(nodes);box.setPadding(new Insets(30));ScrollPane scroll=new ScrollPane(box);scroll.setFitToWidth(true);VBox outer=new VBox(scroll);VBox.setVgrow(scroll,Priority.ALWAYS);return outer;
    }
    private TextField field(String hint) {TextField f=new TextField();f.setPromptText(hint);f.setAccessibleText(hint);return f;}
    private <T> TableColumn<T,String> column(String title,Function<T,String> value) {var c=new TableColumn<T,String>(title);c.setCellValueFactory(d->new ReadOnlyStringWrapper(value.apply(d.getValue())));return c;}
    private <T> TableView<T> table(List<T> rows) {TableView<T> t=new TableView<>(FXCollections.observableArrayList(rows));t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);t.setPrefHeight(300);t.setPlaceholder(new Label("No records yet"));return t;}
    @Override public void start(Stage stage) throws Exception {
        this.stage=stage;
        data=Path.of(System.getProperty("sqlnext.data",System.getenv().getOrDefault("SQLNEXT_DATA",Path.of(System.getProperty("user.home"),".sqlnext").toString())));
        workspaceLock=new WorkspaceLock(data);
        store=new Store(data.resolve("workspace.db"));
        engine=new TaskEngine(store,data);
        timer=new Timeline(new KeyFrame(Duration.seconds(1),e->attempt(()->{if(taskRows!=null&&taskRows.getScene()==scene){var selected=taskRows.getSelectionModel().getSelectedItem();taskRows.setItems(FXCollections.observableArrayList(store.tasks()));if(selected!=null)taskRows.getItems().stream().filter(t->t.id()==selected.id()).findFirst().ifPresent(t->taskRows.getSelectionModel().select(t));}if(!engine.error().isBlank())notice.setText(engine.error());})));timer.setCycleCount(Timeline.INDEFINITE);timer.play();
        root=new BorderPane(); scene=new Scene(root,1180,780);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/theme.css")).toExternalForm());
        applyTheme(store.setting("theme","dark"));
        Label brand=new Label("◈  SQLNext");brand.getStyleClass().add("brand");
        Label edition=new Label("ASSESSMENT WORKSPACE\nDesktop beta · v0.2.0");edition.getStyleClass().add("muted");
        VBox nav=new VBox(12,brand,edition);nav.setPadding(new Insets(24,18,24,18));nav.setPrefWidth(238);nav.getStyleClass().add("sidebar");
        nav.getChildren().addAll(button("Overview",this::dashboard),button("Projects & scope",this::projects),button("Single / batch assessment",this::assessments),button("Tasks & session history",this::sessions),button("Scope validator",this::validator),button("Findings",this::findings),button("Database inspection",this::database),button("Results & evidence",this::results),button("Discovery / endpoints",this::discovery),button("Controlled laboratories",this::laboratories),button("Proxy & Tor diagnostics",this::routes),button("Analytics / comparison",this::analytics),button("Reports",this::reports),button("Remediation assistant",this::assistant),button("Credential vault",this::vault),button("Audit & diagnostics",this::auditView),button("Compatibility",this::compatibility),button("Settings",this::settings));
        ScrollPane navigation=new ScrollPane(nav);navigation.setFitToWidth(true);navigation.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);root.setLeft(navigation);root.setBottom(notice);BorderPane.setMargin(notice,new Insets(12));
        dashboard();stage.setScene(scene);stage.setTitle("SQLNext — Assessment Workspace");stage.setMinWidth(940);stage.setMinHeight(650);stage.show();
        if(Boolean.getBoolean("sqlnext.smoke")) {
            // Exercise every implemented view, SQLite reads and the stylesheet on a real JavaFX toolkit.
            projects();assessments();sessions();validator();findings();database();results();discovery();laboratories();routes();analytics();reports();assistant();vault();auditView();compatibility();settings();dashboard();
            System.out.println("JavaFX smoke: all 18 views constructed successfully");
        }
    }
    private void dashboard() throws Exception {
        var fs=store.findings();
        HBox cards=new HBox(16,metric("PROJECTS",store.projects().size()),metric("RECORDED FINDINGS",fs.size()),metric("OPEN FINDINGS",fs.stream().filter(f->!f.status().equals("Fixed")).count()),metric("AUDIT EVENTS",store.auditCount()));
        PieChart chart=new PieChart();chart.setTitle("Recorded findings by severity");
        for(String severity:List.of("Critical","High","Medium","Low","Info")) {long count=fs.stream().filter(f->f.severity().equals(severity)).count();if(count>0)chart.getData().add(new PieChart.Data(severity,count));}
        chart.setPrefHeight(280);
        var recent=table(fs.stream().limit(5).toList());recent.getColumns().add(column("Recent finding",Store.Finding::title));recent.getColumns().add(column("Severity",Store.Finding::severity));recent.getColumns().add(column("Status",Store.Finding::status));
        root.setCenter(page("Workspace overview","Run scoped diagnostics, inspect results and track remediation. Engine observations require reviewer verification.",cards,new Label("Assessment tasks: "+store.tasks().size()+" · Completed: "+store.tasks().stream().filter(t->t.status().equals("COMPLETED")).count()+" · Verified findings: "+verifiedCount()),button("Refresh overview",this::dashboard),fs.isEmpty()?new Label("Create your first project to begin. Dashboard values come from your local database."):chart,recent));
    }
    private VBox metric(String label,long value) {Label number=new Label(Long.toString(value));number.getStyleClass().add("number");VBox box=new VBox(10,new Label(label),number);box.getStyleClass().add("card");HBox.setHgrow(box,Priority.ALWAYS);box.setMaxWidth(Double.MAX_VALUE);return box;}
    private void projects() throws Exception {
        var rows=table(store.projects());rows.getColumns().add(column("Project",Store.Project::name));rows.getColumns().add(column("Approved URL scope",Store.Project::scope));rows.getColumns().add(column("Authorization reference",Store.Project::authorization));
        TextField name=field("Project name"),scope=field("Approved scope, e.g. https://example.org/app"),authorization=field("Authorization reference (ticket or written approval; no secrets)");
        root.setCenter(page("Projects & authorized assets","Scope uses an exact scheme, hostname, port and path boundary. A project declaration records your authorization reference; it does not independently verify ownership.",rows,name,scope,authorization,button("Create project",()->{store.addProject(name.getText(),scope.getText(),authorization.getText());projects();})));
    }
    private ComboBox<Store.Project> projectChoice() throws Exception {
        var c=new ComboBox<Store.Project>(FXCollections.observableArrayList(store.projects()));c.setPromptText("Select project");c.setMaxWidth(Double.MAX_VALUE);c.setAccessibleText("Project");
        String selected=store.setting("selected.project","");c.getItems().stream().filter(p->Long.toString(p.id()).equals(selected)).findFirst().ifPresent(c::setValue);
        c.valueProperty().addListener((o,a,b)->{if(b!=null)attempt(()->store.putSetting("selected.project",Long.toString(b.id())));});return c;
    }
    private void validator() throws Exception {
        var project=projectChoice();TextArea urls=new TextArea();urls.setPromptText("One HTTP(S) target URL per line");urls.setAccessibleText("Target URLs");urls.setPrefRowCount(7);
        TextArea results=new TextArea();results.setEditable(false);results.setAccessibleText("Scope validation results");
        root.setCenter(page("Scope validator","Validate a single target or a batch locally. This does not send requests or test for SQL injection.",project,urls,button("Validate URLs",()->{
            if(project.getValue()==null)throw new IllegalArgumentException("Select a project first.");
            if(urls.getText().lines().filter(s->!s.isBlank()).count()>1000)throw new IllegalArgumentException("Use at most 1,000 URLs per validation.");
            Set<String> seen=new HashSet<>();StringBuilder out=new StringBuilder();
            for(String raw:urls.getText().lines().filter(s->!s.isBlank()).toList()) {String u=raw.strip();String state;
                try{state=!seen.add(u)?"DUPLICATE":Scope.allows(project.getValue().scope(),u)?"IN SCOPE":"OUT OF SCOPE";}catch(IllegalArgumentException e){state="INVALID URL";}
                out.append(state).append("  ").append(u).append('\n');
            } results.setText(out.toString());
        }),results));
    }
    private void findings() throws Exception {
        var all=store.findings();var rows=table(all);
        rows.getColumns().add(column("Finding",Store.Finding::title));rows.getColumns().add(column("Target",Store.Finding::target));rows.getColumns().add(column("Severity",Store.Finding::severity));rows.getColumns().add(column("Status",Store.Finding::status));
        TextField search=field("Search findings");search.textProperty().addListener((o,a,b)->{findingSearch=b;rows.setItems(FXCollections.observableArrayList(all.stream().filter(f->(f.title()+" "+f.target()+" "+f.status()+" "+f.severity()).toLowerCase(Locale.ROOT).contains(b.toLowerCase(Locale.ROOT))).toList()));});search.setText(findingSearch);
        var status=new ComboBox<String>(FXCollections.observableArrayList("Open","In Progress","Fixed","Accepted Risk","Reopened"));status.setValue("In Progress");status.setAccessibleText("Finding status");
        HBox actions=new HBox(12,status,button("Update selected",()->{if(rows.getSelectionModel().getSelectedItem()==null)throw new IllegalArgumentException("Select a finding.");store.status(rows.getSelectionModel().getSelectedItem().id(),status.getValue());findings();}),button("View evidence",()->{var f=rows.getSelectionModel().getSelectedItem();if(f==null)throw new IllegalArgumentException("Select a finding.");TextArea text=new TextArea("Source: "+store.source(f.id())+"\n\n"+f.evidence());text.setWrapText(true);text.setEditable(false);Dialog<Void> d=new Dialog<>();d.setTitle("Unverified manual evidence");d.getDialogPane().setContent(text);d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);d.showAndWait();}));
        root.setCenter(page("Findings register","Use Review selected to record a human verification decision, owner, CWE and assessed CVSS score. Status and verification are separate. The workspace database is plaintext; store only redacted evidence.",search,rows,actions,new HBox(12,button("Record a finding",this::findingDialog),button("Review selected",()->reviewDialog(rows.getSelectionModel().getSelectedItem())),button("Status history",()->{var f=rows.getSelectionModel().getSelectedItem();if(f==null)throw new IllegalArgumentException("Select a finding.");showText("Finding status history",String.join("\n",store.history(f.id())));}))));
    }
    private void findingDialog() throws Exception {
        var project=projectChoice();TextField target=field("Target URL within project scope"),title=field("Finding title");
        var severity=new ComboBox<String>(FXCollections.observableArrayList("Critical","High","Medium","Low","Info"));severity.setValue("Info");severity.setAccessibleText("Severity");
        TextArea evidence=new TextArea();evidence.setPromptText("Redacted evidence and remediation notes; do not enter secrets");evidence.setAccessibleText("Redacted evidence");evidence.setPrefRowCount(5);
        Dialog<Void> dialog=new Dialog<>();dialog.setTitle("Record unverified finding");dialog.getDialogPane().setContent(new VBox(12,project,target,title,severity,evidence));dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            try{if(project.getValue()==null)throw new IllegalArgumentException("Select a project.");store.addFinding(project.getValue().id(),target.getText(),title.getText(),severity.getValue(),evidence.getText());}
            catch(Exception ex){e.consume();attempt(()->{throw ex;});}
        });dialog.showAndWait();findings();
    }
    private void reports() {
        VBox exports=new VBox(12);
        for(var format:Reports.Format.values())exports.getChildren().add(button("Export "+format,()->{
            FileChooser picker=new FileChooser();picker.setTitle("Export to a new file");picker.setInitialFileName("sqlnext-report."+format.name().toLowerCase(Locale.ROOT));var selected=picker.showSaveDialog(stage);
            if(selected!=null)background(()->Reports.export(store,selected.toPath(),format),()->notice.setText("Report exported: "+selected.getName()));
        }));
        root.setCenter(page("Reports","JSON/HTML include project scopes, findings and task summaries. CSV/SARIF export findings. PDF is a portable summary by finding IDs; use HTML/JSON for full Unicode titles. Evidence, engine output, authorization notes and finding target URLs are omitted. Review before sharing.",exports));
    }
    private void compatibility() throws Exception {
        String matrix=new String(Objects.requireNonNull(getClass().getResourceAsStream("/compatibility.tsv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        TextArea text=new TextArea(matrix);text.setEditable(false);text.setWrapText(true);VBox.setVgrow(text,Priority.ALWAYS);
        root.setCenter(page("Compatibility & delivery status","The supplied SRS is the baseline. Original v5.5 source and executable behavior have not been verified. No legacy feature is claimed compatible yet.",text));
    }
    private void settings() throws Exception {
        var theme=new ComboBox<String>(FXCollections.observableArrayList("dark","light"));theme.setValue(store.setting("theme","dark"));theme.setAccessibleText("Color theme");
        root.setCenter(page("Settings & data","Single-user local workspace. The separate credential vault is encrypted, but workspace evidence is plaintext. OS keychain, team roles, signed extensions and external AI remain pending.",theme,button("Save appearance",()->{store.putSetting("theme",theme.getValue());applyTheme(theme.getValue());}),new Label("Database: "+data.resolve("workspace.db")),new Label("Runtime: Java "+System.getProperty("java.version")+" · JavaFX 21"),button("Configure sqlmap paths",this::sqlmapPaths),button("Configure external AI",this::aiSettings)));
    }
    private void background(Action work,Action success) {
        notice.setText("Working in background…");
        io.submit(()->{try{work.run();Platform.runLater(()->attempt(success));}catch(Exception e){Platform.runLater(()->attempt(()->{throw e;}));}});
    }
    private void showText(String title,String body) {
        TextArea text=new TextArea(body.isBlank()?"No records yet.":body);text.setEditable(false);text.setWrapText(true);text.setPrefSize(760,480);
        Dialog<Void> d=new Dialog<>();d.setTitle(title);d.getDialogPane().setContent(text);d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);d.showAndWait();
    }
    private long verifiedCount() throws Exception {long n=0;for(var f:store.findings())if(store.review(f.id()).verification().equals("Verified"))n++;return n;}
    private void assessments() throws Exception {
        var project=projectChoice();var profile=new ComboBox<String>(FXCollections.observableArrayList("HTTP baseline","Endpoint exposure","sqlmap diagnostic","SQL lab","File lab","Script lab"));profile.setValue("HTTP baseline");profile.setAccessibleText("Assessment profile");
        TextArea targets=new TextArea();targets.setPromptText("One approved URL per line (maximum 100). Labs use URLs only as project attribution, and never send requests.");targets.setPrefRowCount(5);targets.setAccessibleText("Assessment target URLs");
        TextField route=field("DIRECT, http://host:port or socks5://host:port");route.setText(store.setting("network.route","DIRECT"));
        Spinner<Integer> priority=new Spinner<>(0,10,5),delay=new Spinner<>(0,10080,0);priority.setAccessibleText("Priority from zero to ten");delay.setAccessibleText("Schedule delay in minutes");
        CheckBox authorized=new CheckBox("I confirm that this project authorizes these targets and the selected assessment profile.");
        Button importUrls=button("Import URL text file",()->{FileChooser c=new FileChooser();var f=c.showOpenDialog(stage);if(f!=null){if(Files.size(f.toPath())>262144)throw new IllegalArgumentException("URL list must be under 256 KiB.");targets.setText(Files.readString(f.toPath()));}});
        root.setCenter(page("Single / batch assessment","HTTP baseline performs bounded GET diagnostics, not injection testing. Endpoint exposure checks only the exact URLs you enter. Optional sqlmap performs risk-1, level-1 boolean/error diagnostics without extraction. Labs are local synthetic fixtures. A single worker spaces tasks by at least 1.1 seconds; no redirects or proxy fallback.",project,profile,targets,importUrls,new Label("Route (explicit; DIRECT bypasses application proxy profiles)"),route,new HBox(12,new Label("Priority"),priority,new Label("Delay in minutes"),delay),authorized,button("Validate & queue",()->{
            if(project.getValue()==null||!authorized.isSelected())throw new IllegalArgumentException("Select a project and confirm authorization.");
            var urls=targets.getText().lines().map(String::strip).filter(v->!v.isBlank()).distinct().toList();if(urls.isEmpty()||urls.size()>100)throw new IllegalArgumentException("Enter 1–100 distinct URLs.");
            Network.Route.parse(route.getText());
            for(String url:urls)if(!Scope.allows(project.getValue().scope(),url))throw new IllegalArgumentException("A target is invalid or out of scope. No URLs were queued.");
            if(profile.getValue().equals("sqlmap diagnostic")&&(store.setting("python.path","").isBlank()||store.setting("sqlmap.path","").isBlank()))throw new IllegalArgumentException("Configure installed Python and sqlmap.py paths in Settings first.");
            int count=0;for(String url:urls){try{store.enqueue(project.getValue().id(),url,profile.getValue(),route.getText(),priority.getValue(),java.time.Instant.now().plusSeconds(delay.getValue()*60L));count++;}catch(IllegalArgumentException duplicate){if(!duplicate.getMessage().startsWith("An unfinished"))throw duplicate;}}
            notice.setText(count+" tasks queued; duplicate unfinished tasks skipped.");sessions();
        })));
    }
    private Store.Assessment selectedTask() {var task=taskRows.getSelectionModel().getSelectedItem();if(task==null)throw new IllegalArgumentException("Select a task.");return task;}
    private void sessions() throws Exception {
        taskRows=table(store.tasks());taskRows.setPrefHeight(340);
        taskRows.getColumns().add(column("ID",t->Long.toString(t.id())));taskRows.getColumns().add(column("Target",t->Network.redactTarget(t.target())));taskRows.getColumns().add(column("Profile",Store.Assessment::profile));taskRows.getColumns().add(column("State",Store.Assessment::status));taskRows.getColumns().add(column("Priority",t->Integer.toString(t.priority())));taskRows.getColumns().add(column("Due (UTC)",Store.Assessment::due));
        FlowPane actions=new FlowPane(12,12);
        actions.getChildren().addAll(button("Pause queued",()->engine.pause(selectedTask().id())),button("Resume / retry",()->engine.resume(selectedTask().id())),button("Cancel",()->engine.cancel(selectedTask().id())),button("View result",()->{var t=selectedTask();showText("Task #"+t.id()+" · "+t.status(),t.result()+"\n"+t.diagnostic());}),button("Event history",()->showText("Task events",String.join("\n",store.taskEvents(selectedTask().id())))),button("Import observations",this::importObservations));
        root.setCenter(page("Tasks & session history","Queue state and results are saved in SQLite. Queued tasks pause/resume; running HTTP/process tasks can be cancelled. On crash, RUNNING becomes INTERRUPTED and requires manual retry. Completed results remain available. Refreshes every second.",taskRows,actions));
    }
    private void importObservations() throws Exception {
        var t=selectedTask();if(!t.status().equals("COMPLETED"))throw new IllegalArgumentException("Select a completed HTTP task.");
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(t.result());int count=0;
        for(var o:json.path("observations")) {
            try{store.importObservation(t.id(),new com.fasterxml.jackson.databind.ObjectMapper().treeToValue(o,Network.Observation.class));count++;
            }catch(java.sql.SQLException e){if(e.getErrorCode()!=19)throw e;}
        }
        notice.setText(count+" unverified findings imported. Duplicates retained without overwriting evidence.");
    }
    private void reviewDialog(Store.Finding f) throws Exception {
        if(f==null)throw new IllegalArgumentException("Select a finding.");var old=store.review(f.id());
        var state=new ComboBox<String>(FXCollections.observableArrayList("Unverified","Verified","False positive"));state.setValue(old.verification());state.setAccessibleText("Verification decision");
        TextField reviewer=field("Reviewer name"),owner=field("Remediation owner"),cwe=field("CWE identifier, e.g. CWE-89 (optional)"),cvss=field("Independently assessed CVSS score, 0–10 (optional)");reviewer.setText(old.reviewer());owner.setText(old.owner());cwe.setText(old.cwe());cvss.setText(old.cvss());
        Dialog<Void> d=new Dialog<>();d.setTitle("Reviewer decision · Finding #"+f.id());d.getDialogPane().setContent(new VBox(12,new Label("Review evidence before marking Verified. A manual decision is recorded."),state,reviewer,owner,cwe,cvss));d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{store.review(f.id(),state.getValue(),reviewer.getText(),cwe.getText(),cvss.getText(),owner.getText());}catch(Exception ex){e.consume();attempt(()->{throw ex;});}});d.showAndWait();findings();
    }
    private void database() throws Exception {
        var project=projectChoice();
        Label file=new Label("No local SQLite database selected");ComboBox<String> tables=new ComboBox<>();tables.setPromptText("Select a table");tables.setAccessibleText("Database table");
        VBox columns=new VBox(8);TextArea preview=new TextArea();preview.setEditable(false);preview.setPrefRowCount(7);preview.setAccessibleText("Redacted database sample");
        final Path[] selected={null};
        tables.valueProperty().addListener((o,a,b)->{if(b!=null)background(()->{
            try(var db=new DatabaseInspector(selected[0])){var schema=db.columns(b);Platform.runLater(()->{columns.getChildren().clear();for(var c:schema){CheckBox check=new CheckBox(c.name()+" · "+c.type()+(c.primaryKey()?" · PK":""));check.setUserData(c.name());columns.getChildren().add(check);}});}
        },()->notice.setText("Read-only schema loaded."));});
        root.setCenter(page("Database inspection","Choose a local SQLite file you are authorized to inspect. The connection is read-only; previews show up to 20 rows with values redacted. Other DBMS guidance is available in Remediation assistant; remote JDBC drivers are not bundled. Select a project to retain redacted samples.",project,button("Open SQLite database",()->{FileChooser picker=new FileChooser();var f=picker.showOpenDialog(stage);if(f!=null)background(()->{try(var db=new DatabaseInspector(f.toPath())){var names=db.tables();Platform.runLater(()->{selected[0]=f.toPath();file.setText(f.getName());tables.setItems(FXCollections.observableArrayList(names));columns.getChildren().clear();preview.clear();});}},()->notice.setText("Read-only database opened."));}),file,tables,new HBox(12,button("Select all",()->columns.getChildren().forEach(n->((CheckBox)n).setSelected(true))),button("Select none",()->columns.getChildren().forEach(n->((CheckBox)n).setSelected(false)))),columns,button("Append redacted sample",()->{
            if(selected[0]==null||tables.getValue()==null)throw new IllegalArgumentException("Choose a database and table.");
            if(project.getValue()==null)throw new IllegalArgumentException("Select a project for sample provenance.");long projectId=project.getValue().id();
            var chosen=columns.getChildren().stream().map(n->(CheckBox)n).filter(CheckBox::isSelected).map(c->(String)c.getUserData()).toList();Path source=selected[0];String table=tables.getValue();
            background(()->{try(var db=new DatabaseInspector(source)){var sample=db.redactedPreview(table,chosen);String text=new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sample);store.addResultGroup(projectId,source.getFileName().toString(),table,sample.size(),text);Platform.runLater(()->preview.appendText("\n"+java.time.Instant.now()+" · "+source.getFileName()+" · "+table+"\n"+text+"\n"));}},()->notice.setText("Redacted sample appended (up to 20 rows)."));
        }),preview));
    }
    private void discovery() throws Exception {
        var project=projectChoice();TextArea reviewed=new TextArea();reviewed.setPromptText("Paste manually reviewed URLs, one per line. No automatic search harvesting.");reviewed.setPrefRowCount(6);TextArea out=new TextArea();out.setEditable(false);out.setPrefRowCount(5);
        root.setCenter(page("Owned assets & endpoint exposure","Use reviewed discovery results or an approved endpoint list. Only in-scope URLs can be queued. A 200 response does not establish admin access or an authentication bypass.",project,reviewed,button("Generate owned-site query",()->{if(project.getValue()==null)throw new IllegalArgumentException("Select a project.");out.setText("site:"+Scope.parse(project.getValue().scope()).getHost()+"\nReview any discovered URL manually before adding it. This tool does not search or queue arbitrary results.");}),button("Validate reviewed endpoints",()->{if(project.getValue()==null)throw new IllegalArgumentException("Select a project.");StringBuilder b=new StringBuilder();for(String u:reviewed.getText().lines().filter(v->!v.isBlank()).distinct().limit(100).toList()){try{b.append(Scope.allows(project.getValue().scope(),u)?"IN SCOPE  ":"OUT OF SCOPE  ");}catch(Exception ex){b.append("INVALID  ");}b.append(u).append('\n');}out.setText(b.toString()+"\nUse Single / batch assessment → Endpoint exposure to run exact approved URLs.");}),out));
    }
    private void laboratories() {
        VBox actions=new VBox(12);for(String profile:List.of("SQL lab","File lab","Script lab"))actions.getChildren().add(button("Run "+profile,()->background(()->{String result=new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Labs.run(profile));Platform.runLater(()->showText(profile,result));},()->notice.setText("Synthetic fixture completed. No remote target tested."))));
        root.setCenter(page("Controlled laboratories","SQL lab compares concatenated SQL with bound parameters in an in-memory synthetic SQLite database. File lab reads only a packaged harmless marker. Script lab checks a fixed local text transformation and its SHA-256 identifier; it is not a WAF bypass engine. To preserve lab results, queue a lab profile under an authorized project.",actions));
    }
    private void routes() throws Exception {
        TextField route=field("DIRECT, http://host:port or socks5://host:port");route.setText(store.setting("network.route","DIRECT"));Label health=new Label("No route check has run.");health.setWrapText(true);
        root.setCenter(page("Proxy & Tor route diagnostics","Use an explicit HTTP or SOCKS5 proxy. Existing Tor can be configured as socks5://127.0.0.1:9050 (use its actual configured port). The app does not install/start Tor, change system routes or silently fall back to direct access. A handshake alone does not verify Tor or an exit IP. Proxy credentials are not supported by this adapter.",route,button("Save default route",()->{Network.Route.parse(route.getText());store.putSetting("network.route",route.getText());notice.setText("Default route saved for new tasks. Existing tasks retain their recorded routes.");}),button("Check route",()->{var parsed=Network.Route.parse(route.getText());background(()->{String text=Network.routeHealth(parsed);Platform.runLater(()->health.setText(text));},()->notice.setText("Route check completed."));}),health));
    }
    private void analytics() throws Exception {
        var tasks=store.tasks();VBox summary=new VBox(8);for(String state:List.of("QUEUED","RUNNING","PAUSED","COMPLETED","FAILED","CANCELLED","INTERRUPTED"))summary.getChildren().add(new Label(state+": "+tasks.stream().filter(t->t.status().equals(state)).count()));
        var completed=tasks.stream().filter(t->t.status().equals("COMPLETED")&&List.of("HTTP baseline","Endpoint exposure").contains(t.profile())).toList();
        var before=new ComboBox<Store.Assessment>(FXCollections.observableArrayList(completed));var after=new ComboBox<Store.Assessment>(FXCollections.observableArrayList(completed));
        javafx.util.StringConverter<Store.Assessment> converter=new javafx.util.StringConverter<>() {public String toString(Store.Assessment t){return t==null?"":"#"+t.id()+" · "+Network.redactTarget(t.target())+" · "+t.updated();}public Store.Assessment fromString(String v){return null;}};
        before.setConverter(converter);after.setConverter(converter);before.setPromptText("Earlier completed HTTP task");after.setPromptText("Later completed HTTP task");before.setMaxWidth(Double.MAX_VALUE);after.setMaxWidth(Double.MAX_VALUE);
        root.setCenter(page("Analytics & comparison","Compare observations from two completed tasks with matching project, URL, profile and route. A missing observation is not proof that a vulnerability was fixed.",summary,before,after,button("Compare observations",()->{if(before.getValue()==null||after.getValue()==null||before.getValue().id()==after.getValue().id())throw new IllegalArgumentException("Select two distinct completed tasks.");showText("Assessment comparison",new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Guidance.compare(before.getValue(),after.getValue())));}),button("Refresh analytics",this::analytics)));
    }
    private void assistant() throws Exception {
        var findings=new ComboBox<Store.Finding>(FXCollections.observableArrayList(store.findings()));findings.setConverter(new javafx.util.StringConverter<>(){public String toString(Store.Finding f){return f==null?"":"#"+f.id()+" · "+f.title();}public Store.Finding fromString(String s){return null;}});findings.setPromptText("Select a finding");findings.setMaxWidth(Double.MAX_VALUE);
        var dbms=new ComboBox<String>(FXCollections.observableArrayList(new TreeSet<>(Guidance.DBMS.keySet())));dbms.setPromptText("DBMS guidance");
        root.setCenter(page("Remediation assistant","Deterministic local guidance. This is not an AI model and does not automatically verify findings. No evidence is sent by local guidance. External AI requires a verified finding, a configured HTTPS endpoint, an encrypted API key and explicit approval of the displayed title/severity/CWE.",findings,button("Explain selected finding",()->{var f=findings.getValue();if(f==null)throw new IllegalArgumentException("Select a finding.");showText("Local remediation guidance",Guidance.explain(f,store.review(f.id())));}),button("External AI (review & approve)",()->externalAi(findings.getValue())),dbms,button("Show DBMS guidance",()->{if(dbms.getValue()==null)throw new IllegalArgumentException("Select a DBMS.");showText(dbms.getValue(),Guidance.DBMS.get(dbms.getValue()));})));
    }
    private void vault() {
        PasswordField master=new PasswordField(),secret=new PasswordField();master.setPromptText("Master password: at least 12 characters");master.setAccessibleText("Vault master password");secret.setPromptText("Secret value (never saved as plaintext by the vault)");secret.setAccessibleText("Secret value");TextField name=field("Secret name");
        Label info=new Label("Vault locked. Passwords are not recoverable; keep your master password safe.");info.setWrapText(true);
        root.setCenter(page("Encrypted credential vault","Separate AES-256-GCM vault protected by PBKDF2-HMAC-SHA256 (600,000 iterations). The workspace database is not encrypted. OS keychain integration is not implemented. Secrets are not automatically injected into tasks, proxies or external services.",master,name,secret,button("Save / update secret",()->{
            if(name.getText().isBlank()||secret.getText().isBlank())throw new IllegalArgumentException("Enter a name and secret.");
            char[] password=master.getText().toCharArray();String label=name.getText().strip(),value=secret.getText();master.clear();secret.clear();
            background(()->{try{Path path=data.resolve("credentials.vault");var entries=Files.exists(path)?Vault.load(path,password):new HashMap<String,String>();entries.put(label,value);Vault.save(path,password,entries);}finally{Arrays.fill(password,'\0');}},()->info.setText("Secret encrypted and saved. Vault remains locked; enter the master password for each operation."));
        }),button("List secret names",()->{char[] password=master.getText().toCharArray();master.clear();background(()->{try{var entries=Vault.load(data.resolve("credentials.vault"),password);String names=String.join("\n",new TreeSet<>(entries.keySet()));Platform.runLater(()->showText("Saved secret names (values hidden)",names));}finally{Arrays.fill(password,'\0');}},()->info.setText("Secret names read; values remain hidden."));}),button("Remove named secret",()->{char[] password=master.getText().toCharArray();String label=name.getText().strip();master.clear();background(()->{try{Path file=data.resolve("credentials.vault");var entries=Vault.load(file,password);if(entries.remove(label)==null)throw new IllegalArgumentException("Secret name not found.");Vault.save(file,password,entries);}finally{Arrays.fill(password,'\0');}},()->info.setText("Secret removed from the current vault. Old filesystem backups may retain earlier encrypted copies."));}),info));
    }
    private void auditView() throws Exception {
        var rows=table(store.audits());rows.getColumns().add(column("Event",Store.Audit::event));rows.getColumns().add(column("Entity ID",a->Long.toString(a.entityId())));rows.getColumns().add(column("Time (UTC)",Store.Audit::created));
        root.setCenter(page("Audit & diagnostics","Local mutation events are committed with the corresponding changes. These records are not tamper-proof and do not establish authenticated user identity. Task events retain execution state transitions.",rows,button("Refresh audit",this::auditView),button("Export audit JSON",()->{FileChooser chooser=new FileChooser();chooser.setInitialFileName("sqlnext-audit.json");var f=chooser.showSaveDialog(stage);if(f!=null)Files.writeString(f.toPath(),new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(store.audits()),StandardOpenOption.CREATE_NEW);})));
    }
    private void sqlmapPaths() throws Exception {
        TextField python=field("Full path to Python executable"),script=field("Full path to installed sqlmap.py");python.setText(store.setting("python.path",""));script.setText(store.setting("sqlmap.path",""));
        Dialog<Void> d=new Dialog<>();d.setTitle("Optional sqlmap dependency");d.getDialogPane().setContent(new VBox(12,new Label("Select an installed, trusted sqlmap distribution. Shell commands and extra flags are not accepted."),python,script));d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{if(!Files.isRegularFile(Path.of(python.getText()))||!Files.isRegularFile(Path.of(script.getText())))throw new IllegalArgumentException("Both paths must identify existing files.");store.putSetting("python.path",python.getText());store.putSetting("sqlmap.path",script.getText());}catch(Exception ex){e.consume();attempt(()->{throw ex;});}});d.showAndWait();
    }
    private void results() throws Exception {
        var rows=table(store.resultGroups());rows.getColumns().add(column("ID",r->Long.toString(r.id())));rows.getColumns().add(column("Project",r->Long.toString(r.projectId())));rows.getColumns().add(column("Source",Store.ResultGroup::source));rows.getColumns().add(column("Table / group",Store.ResultGroup::label));rows.getColumns().add(column("Rows (sample)",r->Integer.toString(r.rows())));rows.getColumns().add(column("Created",Store.ResultGroup::created));
        root.setCenter(page("Results & evidence","Redacted database samples append as independent persistent groups. Task results remain in session history. Workspace data is plaintext; deletion removes the current record, not filesystem backups or forensic remnants.",rows,new HBox(12,button("View group",()->{var r=rows.getSelectionModel().getSelectedItem();if(r==null)throw new IllegalArgumentException("Select a result group.");showText("Result #"+r.id(),r.content());}),button("Export group JSON",()->{var r=rows.getSelectionModel().getSelectedItem();if(r==null)throw new IllegalArgumentException("Select a result group.");FileChooser picker=new FileChooser();picker.setInitialFileName("sqlnext-result-"+r.id()+".json");var file=picker.showSaveDialog(stage);if(file!=null)Files.writeString(file.toPath(),new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(r),StandardOpenOption.CREATE_NEW);}),button("Delete selected",()->{var r=rows.getSelectionModel().getSelectedItem();if(r==null)throw new IllegalArgumentException("Select a result group.");Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Delete result group #"+r.id()+" from the current workspace?",ButtonType.OK,ButtonType.CANCEL);if(confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){store.deleteResultGroup(r.id());results();}}))));
    }
    private void aiSettings() throws Exception {
        TextField endpoint=field("HTTPS OpenAI-compatible chat-completions endpoint"),model=field("Model identifier"),key=field("Encrypted vault entry name for API key");endpoint.setText(store.setting("ai.endpoint",""));model.setText(store.setting("ai.model",""));key.setText(store.setting("ai.key.name",""));
        Dialog<Void> d=new Dialog<>();d.setTitle("Optional external AI service");d.getDialogPane().setContent(new VBox(12,new Label("No request is sent when saving settings. Store the API key in Credential vault first."),endpoint,model,key));d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{var uri=Scope.parse(endpoint.getText());if(!uri.getScheme().equalsIgnoreCase("https")||uri.getRawQuery()!=null||model.getText().isBlank()||key.getText().isBlank())throw new IllegalArgumentException("Provide HTTPS endpoint without query, model and vault entry name.");store.putSetting("ai.endpoint",endpoint.getText());store.putSetting("ai.model",model.getText());store.putSetting("ai.key.name",key.getText());}catch(Exception ex){e.consume();attempt(()->{throw ex;});}});d.showAndWait();
    }
    private void externalAi(Store.Finding finding)throws Exception {
        if(finding==null)throw new IllegalArgumentException("Select a finding.");var review=store.review(finding.id());if(!review.verification().equals("Verified"))throw new IllegalArgumentException("A reviewer must verify the finding first.");
        String endpoint=store.setting("ai.endpoint",""),model=store.setting("ai.model",""),keyName=store.setting("ai.key.name","");var route=Network.Route.parse(store.setting("network.route","DIRECT"));if(endpoint.isBlank()||model.isBlank()||keyName.isBlank())throw new IllegalArgumentException("Configure the external AI service in Settings first.");
        PasswordField master=new PasswordField();master.setPromptText("Vault master password");CheckBox approved=new CheckBox("I reviewed this summary and approve sending it to this endpoint.");
        Dialog<Void> d=new Dialog<>();d.setTitle("Approve external AI request");Label summary=new Label("Destination: "+endpoint+"\nRoute: "+route.label()+"\nModel: "+model+"\nTitle: "+finding.title()+"\nSeverity: "+finding.severity()+"\nCWE: "+review.cwe()+"\nOnly these three finding fields are sent; title text may itself contain sensitive information.");summary.setWrapText(true);d.getDialogPane().setContent(new VBox(12,summary,approved,master));d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e->{if(!approved.isSelected()){e.consume();return;}char[] password=master.getText().toCharArray();master.clear();background(()->{try{var secrets=Vault.load(data.resolve("credentials.vault"),password);String token=secrets.get(keyName);if(token==null)throw new IllegalArgumentException("API key name not found in vault.");String output=new AiClient(route).explain(finding,review,endpoint,model,token,true);Platform.runLater(()->showText("AI draft — human review required",output));}finally{Arrays.fill(password,'\0');}},()->notice.setText("External AI response received; review the draft before use."));});d.showAndWait();
    }
    private void applyTheme(String theme) {root.getStyleClass().removeAll("dark","light");root.getStyleClass().add(theme);}
    @Override public void stop() throws Exception {if(timer!=null)timer.stop();io.shutdownNow();if(engine!=null)engine.close();if(store!=null)store.close();if(workspaceLock!=null)workspaceLock.close();}
    public static void main(String[] args) {launch(args);}
}
