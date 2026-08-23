package pl.sk.ocr.configurator.result;

import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.FieldResult;

public final class CategoryTestResultPanel {
    private static final String TEXT_COLOR_STYLE = "-fx-text-fill: #111827;";

    private final Supplier<DocumentResult> resultSupplier;
    private final Label document = label("Document: -");
    private final Label category = label("Category: -");
    private final Label status = label("Status: -");
    private final Label identification = label("Identification: -");
    private final Label geometry = label("Geometry: -");
    private final Label trace = label("Trace: -");
    private final TableView<FieldRow> fields = new TableView<>();
    private final ListView<String> issues = new ListView<>();
    private final ListView<String> traceDetails = new ListView<>();
    private final VBox root;

    public CategoryTestResultPanel(Supplier<DocumentResult> resultSupplier) {
        this.resultSupplier = resultSupplier;
        configureFields();
        issues.setPrefHeight(120);
        issues.setCellFactory(list -> textCell());
        traceDetails.setPrefHeight(150);
        traceDetails.setCellFactory(list -> textCell());
        root = new VBox(6,
            document,
            category,
            status,
            identification,
            geometry,
            trace,
            label("Field results"),
            fields,
            label("Identification trace"),
            traceDetails,
            label("Errors / Warnings"),
            issues
        );
        refresh();
    }

    public Node view() {
        return root;
    }

    public void refresh() {
        var result = resultSupplier.get();
        if (result == null) {
            document.setText("Document: -");
            category.setText("Category: -");
            status.setText("Status: -");
            identification.setText("Identification: -");
            geometry.setText("Geometry: -");
            trace.setText("Trace: -");
            fields.getItems().clear();
            traceDetails.getItems().setAll("No trace entries");
            issues.getItems().setAll("No category test result");
            return;
        }
        document.setText("Document: " + result.documentId().value());
        category.setText("Category: " + (result.categoryId() == null ? "-" : result.categoryId().value()));
        status.setText("Status: " + result.status());
        identification.setText("Identification: " + identificationStatus(result));
        geometry.setText("Geometry: " + geometryStatus(result));
        trace.setText("Trace: " + result.trace().mode() + " | entries=" + result.trace().entries().size());
        fields.getItems().setAll(result.fields().stream().map(FieldRow::from).toList());
        var traceTexts = traceTexts(result);
        traceDetails.getItems().setAll(traceTexts.isEmpty() ? List.of("No identification trace entries") : traceTexts);
        var issueTexts = issueTexts(result);
        issues.getItems().setAll(issueTexts.isEmpty() ? List.of("No errors or warnings") : issueTexts);
    }

    private void configureFields() {
        fields.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fields.setPrefHeight(150);
        fields.getColumns().add(column("Field", "field", 120));
        fields.getColumns().add(column("Status", "status", 90));
        fields.getColumns().add(column("Value", "value", 180));
        fields.getColumns().add(column("Issues", "issues", 70));
        fields.setStyle(TEXT_COLOR_STYLE);
        fields.skinProperty().addListener((obs, old, skin) ->
            javafx.application.Platform.runLater(() -> fields.lookupAll(".column-header .label")
                .forEach(node -> node.setStyle(TEXT_COLOR_STYLE))));
    }

    private TableColumn<FieldRow, String> column(String title, String property, double width) {
        var column = new TableColumn<FieldRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setStyle(TEXT_COLOR_STYLE);
        column.setPrefWidth(width);
        return column;
    }

    private String identificationStatus(DocumentResult result) {
        if (result.categoryId() != null) {
            return "MATCHED";
        }
        return result.issues().stream().anyMatch(issue -> issue.stage() == pl.sk.ocr.domain.issue.ProcessingStage.CATEGORY_IDENTIFICATION)
            ? "FAILED"
            : "-";
    }

    private String geometryStatus(DocumentResult result) {
        if (result.issues().stream().anyMatch(issue -> issue.stage() == pl.sk.ocr.domain.issue.ProcessingStage.GEOMETRY_RESOLUTION)) {
            return "FAILED";
        }
        return result.categoryId() == null ? "-" : "OK";
    }

    private List<String> issueTexts(DocumentResult result) {
        var all = new java.util.ArrayList<ProcessingIssue>();
        all.addAll(result.issues());
        result.fields().stream().flatMap(field -> field.issues().stream()).forEach(all::add);
        return all.stream().map(this::issueText).toList();
    }

    private List<String> traceTexts(DocumentResult result) {
        if (result.trace() == null || result.trace().entries() == null) {
            return List.of();
        }
        return result.trace().entries().stream()
            .filter(entry -> entry.stage() == pl.sk.ocr.domain.issue.ProcessingStage.CATEGORY_IDENTIFICATION)
            .map(entry -> {
                var attributes = entry.attributes();
                return "category=" + attributes.getOrDefault("categoryId", "")
                    + " | group=" + attributes.getOrDefault("group", "")
                    + " | condition=" + attributes.getOrDefault("condition", "")
                    + " | matched=" + attributes.getOrDefault("matched", "")
                    + " | matcher=" + attributes.getOrDefault("matcherId", "")
                    + " | matcherStatus=" + attributes.getOrDefault("matcherStatus", "")
                    + " | expected=" + attributes.getOrDefault("expectedText", "")
                    + " | ocrTextInRegion=" + attributes.getOrDefault("ocrTextInRegion", "");
            })
            .toList();
    }

    private String issueText(ProcessingIssue issue) {
        var field = issue.fieldId() == null ? "" : " | field=" + issue.fieldId().value();
        var anchor = issue.anchorId() == null ? "" : " | anchor=" + issue.anchorId().value();
        return issue.severity() + " " + issue.code().value() + " | " + issue.stage() + field + anchor + " | " + issue.message();
    }

    private static Label label(String text) {
        var label = new Label(text);
        label.setStyle(TEXT_COLOR_STYLE);
        return label;
    }

    private static ListCell<String> textCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(TEXT_COLOR_STYLE);
            }
        };
    }

    public record FieldRow(String field, String status, String value, String issues) {
        static FieldRow from(FieldResult result) {
            return new FieldRow(
                result.fieldId().value(),
                result.status().name(),
                result.value() == null ? "" : result.value(),
                String.valueOf(result.issues().size())
            );
        }

        public String getField() {
            return field;
        }

        public String getStatus() {
            return status;
        }

        public String getValue() {
            return value;
        }

        public String getIssues() {
            return issues;
        }
    }
}
