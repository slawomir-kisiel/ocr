package pl.sk.ocr.configurator.trace;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.sk.ocr.configurator.app.TraceImageStore;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.result.StageResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceEntry;
import pl.sk.ocr.domain.trace.TraceImageRef;

public final class TraceViewerPanel {
    private static final String TEXT_COLOR_STYLE = "-fx-text-fill: #111827;";

    private final Supplier<ProcessingTrace> traceSupplier;
    private final Supplier<TraceImageStore> imageStoreSupplier;
    private final TableView<TraceRow> stages = new TableView<>();
    private final Label summary = new Label("Trace: OFF");
    private final TextArea details = new TextArea();
    private final ListView<String> issues = new ListView<>();
    private final HBox images = new HBox(8);
    private final VBox root;

    public TraceViewerPanel(Supplier<ProcessingTrace> traceSupplier, Supplier<TraceImageStore> imageStoreSupplier) {
        this.traceSupplier = traceSupplier;
        this.imageStoreSupplier = imageStoreSupplier;
        configureStages();
        details.setEditable(false);
        details.setWrapText(false);
        details.setPrefRowCount(6);
        details.setStyle(TEXT_COLOR_STYLE);
        issues.setPrefHeight(90);
        issues.setCellFactory(list -> textCell());
        images.setPadding(new Insets(4));
        var imageScroll = new ScrollPane(images);
        imageScroll.setFitToHeight(true);
        imageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        imageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        var lower = new VBox(6, label("Details"), details, label("Issues"), issues, label("Images"), imageScroll);
        var split = new SplitPane(stages, lower);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.42);
        root = new VBox(6, summary, split);
        summary.setStyle(TEXT_COLOR_STYLE);
        VBox.setVgrow(split, Priority.ALWAYS);
        refresh();
    }

    public Node view() {
        return root;
    }

    public void refresh() {
        var trace = traceSupplier.get();
        if (trace == null) {
            trace = ProcessingTrace.off();
        }
        var rows = rows(trace);
        summary.setText("Trace: " + trace.mode() + " | stages=" + rows.size() + " | images=" + imageStoreSupplier.get().size());
        stages.getItems().setAll(rows);
        if (rows.isEmpty()) {
            clearSelection();
        } else if (stages.getSelectionModel().getSelectedItem() == null) {
            stages.getSelectionModel().selectFirst();
        } else {
            show(stages.getSelectionModel().getSelectedItem());
        }
    }

    private void configureStages() {
        stages.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        stages.setPrefHeight(170);
        stages.getColumns().add(column("Stage", "stage", 150));
        stages.getColumns().add(column("Status", "status", 90));
        stages.getColumns().add(column("Duration", "duration", 80));
        stages.getColumns().add(column("Message", "message", 220));
        stages.setStyle(TEXT_COLOR_STYLE);
        stages.skinProperty().addListener((obs, old, skin) -> styleTableHeaderLabels());
        stages.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                clearSelection();
            } else {
                show(selected);
            }
        });
    }

    private TableColumn<TraceRow, String> column(String title, String property, double width) {
        var column = new TableColumn<TraceRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setStyle(TEXT_COLOR_STYLE);
        column.setPrefWidth(width);
        return column;
    }

    private void styleTableHeaderLabels() {
        Platform.runLater(() -> stages.lookupAll(".column-header .label")
            .forEach(node -> node.setStyle(TEXT_COLOR_STYLE)));
    }

    private List<TraceRow> rows(ProcessingTrace trace) {
        var stagesByStage = trace.stages().stream()
            .collect(java.util.stream.Collectors.toMap(StageResult::stage, stage -> stage, (first, second) -> first));
        var rows = new java.util.ArrayList<TraceRow>();
        for (var entry : trace.entries()) {
            var stage = stagesByStage.get(entry.stage());
            rows.add(new TraceRow(
                entry.stage(),
                stage == null ? null : stage.status(),
                duration(entry.attributes()),
                entry.message(),
                entry.attributes(),
                stage == null ? List.of() : stage.issues(),
                entry.images()
            ));
        }
        for (var stage : trace.stages()) {
            var hasEntry = trace.entries().stream().anyMatch(entry -> entry.stage() == stage.stage());
            if (!hasEntry) {
                rows.add(new TraceRow(stage.stage(), stage.status(), "-", "", Map.of(), stage.issues(), List.of()));
            }
        }
        return rows.stream()
            .sorted(Comparator.comparing(row -> row.stage().ordinal()))
            .toList();
    }

    private String duration(Map<String, Object> attributes) {
        var value = attributes.get("durationMs");
        if (value == null) {
            value = attributes.get("duration");
        }
        return value == null ? "-" : value + " ms";
    }

    private void show(TraceRow row) {
        details.setText(detailsText(row));
        issues.getItems().setAll(row.issues().stream().map(this::issueText).toList());
        images.getChildren().setAll(row.images().stream().map(this::imageNode).toList());
        if (row.issues().isEmpty()) {
            issues.getItems().setAll("No issues");
        }
        if (row.images().isEmpty()) {
            images.getChildren().setAll(label("No images for this stage"));
        }
    }

    private String detailsText(TraceRow row) {
        var builder = new StringBuilder();
        builder.append("stage=").append(row.stage()).append(System.lineSeparator());
        builder.append("status=").append(row.status()).append(System.lineSeparator());
        builder.append("duration=").append(row.duration()).append(System.lineSeparator());
        builder.append("message=").append(row.message()).append(System.lineSeparator());
        if (!row.attributes().isEmpty()) {
            builder.append(System.lineSeparator()).append("context:").append(System.lineSeparator());
            row.attributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(entry.getKey()).append("=").append(entry.getValue()).append(System.lineSeparator()));
        }
        return builder.toString();
    }

    private String issueText(ProcessingIssue issue) {
        return issue.severity() + " " + issue.code() + " | " + issue.message();
    }

    private Node imageNode(TraceImageRef ref) {
        var store = imageStoreSupplier.get();
        var image = store.get(ref);
        if (image.isEmpty()) {
            return new VBox(4, label(ref.label()), label("Image not available"));
        }
        var view = new ImageView(SwingFXUtils.toFXImage(image.get().asBufferedImage(), null));
        view.setPreserveRatio(true);
        view.setFitWidth(260);
        view.setFitHeight(180);
        var box = new VBox(4, label(ref.label()), view);
        box.setPadding(new Insets(4));
        box.setStyle("-fx-border-color: #c8cdd4; -fx-border-radius: 4; -fx-background-radius: 4;");
        return box;
    }

    private Label label(String text) {
        var label = new Label(text);
        label.setStyle(TEXT_COLOR_STYLE);
        return label;
    }

    private ListCell<String> textCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(TEXT_COLOR_STYLE);
            }
        };
    }

    private void clearSelection() {
        details.clear();
        issues.getItems().setAll("No trace entries");
        images.getChildren().setAll(label("No images"));
    }

    public record TraceRow(
        ProcessingStage stage,
        ProcessingStatus status,
        String duration,
        String message,
        Map<String, Object> attributes,
        List<ProcessingIssue> issues,
        List<TraceImageRef> images
    ) {
        public String getStage() {
            return stage == null ? "" : stage.name();
        }

        public String getStatus() {
            return status == null ? "" : status.name();
        }

        public String getDuration() {
            return duration == null ? "" : duration;
        }

        public String getMessage() {
            return message == null ? "" : message;
        }
    }
}
