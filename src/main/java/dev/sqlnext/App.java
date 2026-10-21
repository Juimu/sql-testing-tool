package dev.sqlnext;

import javafx.application.Application;
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
    private BorderPane root;
    private Scene scene;
    private Stage stage;
    private Path data;
    private final Label notice=new Label("Local workspace · No network assessments are performed");
    private interface Action { void run() throws Exception; }
    private void attempt(Action action) { try {action.run();} catch(Exception e) { new Alert(Alert.AlertType.ERROR, Objects.toString(e.getMessage(),"Operation failed."),ButtonType.OK).showAndWait(); } }
    private Button button(String text, Action action) {Button b=new Button(text);b.setOnAction(e->attempt(action));return b;}
    private VBox page(String title,String description, javafx.scene.Node... nodes) {
        Label heading=new Label(title);heading.getStyleClass().add("heading");
        Label sub=new Label(description);sub.setWrapText(true);sub.getStyleClass().add("muted");
        VBox box=new VBox(18,heading,sub);box.getChildren().addAll(nodes);box.setPadding(new Insets(30));return box;
    }
    private TextField field(String hint) {TextField f=new TextField();f.setPromptText(hint);f.setAccessibleText(hint);return f;}
    private <T> TableColumn<T,String> column(String title,Function<T,String> value) {var c=new TableColumn<T,String>(title);c.setCellValueFactory(d->new ReadOnlyStringWrapper(value.apply(d.getValue())));return c;}
    private <T> TableView<T> table(List<T> rows) {TableView<T> t=new TableView<>(FXCollections.observableArrayList(rows));t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);t.setPrefHeight(300);t.setPlaceholder(new Label("No records yet"));return t;}
    @Override public void start(Stage stage) throws Exception {
        this.stage=stage;
        data=Path.of(System.getProperty("sqlnext.data",System.getenv().getOrDefault("SQLNEXT_DATA",Path.of(System.getProperty("user.home"),".sqlnext").toString())));
        store=new Store(data.resolve("workspace.db"));
        root=new BorderPane(); scene=new Scene(root,1180,780);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/theme.css")).toExternalForm());
        applyTheme(store.setting("theme","dark"));
        Label brand=new Label("◈  SQLNext");brand.getStyleClass().add("brand");
        Label edition=new Label("ASSESSMENT WORKSPACE\nFoundation · v0.1.0");edition.getStyleClass().add("muted");
        VBox nav=new VBox(12,brand,edition);nav.setPadding(new Insets(24,18,24,18));nav.setPrefWidth(238);nav.getStyleClass().add("sidebar");
        nav.getChildren().addAll(button("Overview",this::dashboard),button("Projects & scope",this::projects),button("Scope validator",this::validator),button("Findings",this::findings),button("Reports",this::reports),button("Compatibility",this::compatibility),button("Settings",this::settings));
        root.setLeft(nav);root.setBottom(notice);BorderPane.setMargin(notice,new Insets(12));
        dashboard();stage.setScene(scene);stage.setTitle("SQLNext — Assessment Workspace");stage.setMinWidth(940);stage.setMinHeight(650);stage.show();
        if(Boolean.getBoolean("sqlnext.smoke")) {
            // Exercise every implemented view, SQLite reads and the stylesheet on a real JavaFX toolkit.
            projects();validator();findings();reports();compatibility();settings();dashboard();
            System.out.println("JavaFX smoke: all seven views constructed successfully");
        }
    }
    private void dashboard() throws Exception {
        var fs=store.findings();
        HBox cards=new HBox(16,metric("PROJECTS",store.projects().size()),metric("RECORDED FINDINGS",fs.size()),metric("OPEN FINDINGS",fs.stream().filter(f->!f.status().equals("Fixed")).count()),metric("AUDIT EVENTS",store.auditCount()));
        PieChart chart=new PieChart();chart.setTitle("Recorded severity · manually supplied");
        for(String severity:List.of("Critical","High","Medium","Low","Info")) {long count=fs.stream().filter(f->f.severity().equals(severity)).count();if(count>0)chart.getData().add(new PieChart.Data(severity,count));}
        chart.setPrefHeight(280);
        var recent=table(fs.stream().limit(5).toList());recent.getColumns().add(column("Recent finding",Store.Finding::title));recent.getColumns().add(column("Severity",Store.Finding::severity));recent.getColumns().add(column("Status",Store.Finding::status));
        root.setCenter(page("Workspace overview","Register authorized projects, organize evidence and track remediation. Recorded findings are unverified manual entries.",cards,fs.isEmpty()?new Label("Create your first project to begin. Dashboard values come from your local database."):chart,recent));
    }
    private VBox metric(String label,long value) {Label number=new Label(Long.toString(value));number.getStyleClass().add("number");VBox box=new VBox(10,new Label(label),number);box.getStyleClass().add("card");HBox.setHgrow(box,Priority.ALWAYS);box.setMaxWidth(Double.MAX_VALUE);return box;}
    private void projects() throws Exception {
        var rows=table(store.projects());rows.getColumns().add(column("Project",Store.Project::name));rows.getColumns().add(column("Approved URL scope",Store.Project::scope));rows.getColumns().add(column("Authorization reference",Store.Project::authorization));
        TextField name=field("Project name"),scope=field("Approved scope, e.g. https://example.org/app"),authorization=field("Authorization reference (ticket or written approval; no secrets)");
        root.setCenter(page("Projects & authorized assets","Scope uses an exact scheme, hostname, port and path boundary. A project declaration records your authorization reference; it does not independently verify ownership.",rows,name,scope,authorization,button("Create project",()->{store.addProject(name.getText(),scope.getText(),authorization.getText());projects();})));
    }
    private ComboBox<Store.Project> projectChoice() throws Exception {var c=new ComboBox<Store.Project>(FXCollections.observableArrayList(store.projects()));c.setPromptText("Select project");c.setMaxWidth(Double.MAX_VALUE);c.setAccessibleText("Project");return c;}
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
        TextField search=field("Search findings");search.textProperty().addListener((o,a,b)->rows.setItems(FXCollections.observableArrayList(all.stream().filter(f->(f.title()+" "+f.target()+" "+f.status()+" "+f.severity()).toLowerCase(Locale.ROOT).contains(b.toLowerCase(Locale.ROOT))).toList())));
        var status=new ComboBox<String>(FXCollections.observableArrayList("Open","In Progress","Fixed","Accepted Risk","Reopened"));status.setValue("In Progress");status.setAccessibleText("Finding status");
        HBox actions=new HBox(12,status,button("Update selected",()->{if(rows.getSelectionModel().getSelectedItem()==null)throw new IllegalArgumentException("Select a finding.");store.status(rows.getSelectionModel().getSelectedItem().id(),status.getValue());findings();}),button("View evidence",()->{var f=rows.getSelectionModel().getSelectedItem();if(f==null)throw new IllegalArgumentException("Select a finding.");TextArea text=new TextArea(f.evidence());text.setWrapText(true);text.setEditable(false);Dialog<Void> d=new Dialog<>();d.setTitle("Unverified manual evidence");d.getDialogPane().setContent(text);d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);d.showAndWait();}));
        root.setCenter(page("Findings register","Manual entries remain unverified. Status tracks remediation, not independent security verification. Store only redacted evidence; this foundation database is not encrypted.",search,rows,actions,button("Record a finding",this::findingDialog)));
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
        root.setCenter(page("Reports","Export the current workspace as JSON or a standalone HTML report. Evidence, authorization notes and finding target URLs are omitted; project scope URLs and titles remain. Review exports before sharing.",button("Export JSON",()->export(false)),button("Export HTML",()->export(true)),new Label("PDF, CSV, SARIF, branding and historical comparison are planned.")));
    }
    private void export(boolean html) throws Exception {
        FileChooser picker=new FileChooser();picker.setTitle("Export report to a new file");picker.setInitialFileName("sqlnext-report."+(html?"html":"json"));var selected=picker.showSaveDialog(stage);
        if(selected!=null){Reports.export(store,selected.toPath(),html);notice.setText("Report exported: "+selected.getName());}
    }
    private void compatibility() throws Exception {
        String matrix=new String(Objects.requireNonNull(getClass().getResourceAsStream("/compatibility.tsv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        TextArea text=new TextArea(matrix);text.setEditable(false);text.setWrapText(true);VBox.setVgrow(text,Priority.ALWAYS);
        root.setCenter(page("Compatibility & delivery status","The supplied SRS is the baseline. Original v5.5 source and executable behavior have not been verified. No legacy feature is claimed compatible yet.",text));
    }
    private void settings() throws Exception {
        var theme=new ComboBox<String>(FXCollections.observableArrayList("dark","light"));theme.setValue(store.setting("theme","dark"));theme.setAccessibleText("Color theme");
        root.setCenter(page("Settings & data","Single-user local workspace. Encryption, OS keychain, roles, plugins and external AI are pending. No credentials are collected or transmitted.",theme,button("Save appearance",()->{store.putSetting("theme",theme.getValue());applyTheme(theme.getValue());}),new Label("Database: "+data.resolve("workspace.db")),new Label("Runtime: Java "+System.getProperty("java.version")+" · JavaFX 21")));
    }
    private void applyTheme(String theme) {root.getStyleClass().removeAll("dark","light");root.getStyleClass().add(theme);}
    @Override public void stop() throws Exception {if(store!=null)store.close();}
    public static void main(String[] args) {launch(args);}
}
