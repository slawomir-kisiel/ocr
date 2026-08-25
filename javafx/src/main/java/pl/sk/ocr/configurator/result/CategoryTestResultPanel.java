package pl.sk.ocr.configurator.result;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.FieldResult;

public final class CategoryTestResultPanel {
    private static final String TEXT_COLOR_STYLE = "-fx-text-fill: #111827;";

    private final Supplier<DocumentResult> resultSupplier;
    private final Supplier<List<CategoryReferenceDocumentTestResult>> batchResultSupplier;
    private final Consumer<CategoryReferenceDocumentTestResult> resultSelection;
    private final Consumer<FieldResult> fieldSelection;
    private final Consumer<AnchorTraceSelection> anchorSelection;
    private final TableView<DocumentRow> documents = new TableView<>();
    private final Label document = label("Document: -");
    private final Label category = label("Category: -");
    private final Label status = label("Status: -");
    private final Label identification = label("Identification: -");
    private final Label geometry = label("Geometry: -");
    private final Label trace = label("Trace: -");
    private final TableView<FieldRow> fields = new TableView<>();
    private final ListView<String> issues = new ListView<>();
    private final TableView<AnchorTraceRow> anchorTrace = new TableView<>();
    private final TableView<IdentificationTraceRow> identificationTrace = new TableView<>();
    private final Label transformMain = label("dx = -, dy = -, scaleX = -, scaleY = -");
    private final Label transformAffine = label("affine - [a = -, b = -, c = -, d = -]");
    private final VBox root;

    public CategoryTestResultPanel(Supplier<DocumentResult> resultSupplier,
                                   Supplier<List<CategoryReferenceDocumentTestResult>> batchResultSupplier,
                                   Consumer<CategoryReferenceDocumentTestResult> resultSelection,
                                   Consumer<FieldResult> fieldSelection,
                                   Consumer<AnchorTraceSelection> anchorSelection) {
        this.resultSupplier = resultSupplier;
        this.batchResultSupplier = batchResultSupplier;
        this.resultSelection = resultSelection;
        this.fieldSelection = fieldSelection;
        this.anchorSelection = anchorSelection;
        configureDocuments();
        configureFields();
        issues.setPrefHeight(120);
        issues.setCellFactory(list -> textCell());
        configureAnchorTrace();
        configureIdentificationTrace();
        root = new VBox(6,
            label("Reference documents"),
            documents,
            document,
            category,
            status,
            identification,
            geometry,
            trace,
            label("Field results"),
            fields,
            label("Geometry trace"),
            anchorTrace,
            transformSummary(),
            label("Identification trace"),
            identificationTrace,
            label("Errors / Warnings"),
            issues
        );
        documents.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            var source = selected == null ? null : selected.source();
            refreshDetails(source == null ? null : source.result());
            if (resultSelection != null) {
                resultSelection.accept(source);
            }
        });
        fields.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (fieldSelection != null) {
                fieldSelection.accept(selected == null ? null : selected.source());
            }
        });
        anchorTrace.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (anchorSelection != null) {
                anchorSelection.accept(selected == null ? null : new AnchorTraceSelection(selected.bounds(), selected.searchRegion()));
            }
        });
        identificationTrace.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (anchorSelection != null) {
                anchorSelection.accept(selected == null ? null : new AnchorTraceSelection(null, selected.searchRegion()));
            }
        });
        refresh();
    }

    public Node view() {
        return root;
    }

    public void refresh() {
        var batchResults = batchResultSupplier == null ? List.<CategoryReferenceDocumentTestResult>of() : batchResultSupplier.get();
        if (batchResults != null && !batchResults.isEmpty()) {
            documents.getItems().setAll(batchResults.stream().map(DocumentRow::from).toList());
            if (documents.getSelectionModel().getSelectedIndex() < 0) {
                documents.getSelectionModel().selectFirst();
            } else {
                refreshDetails(documents.getSelectionModel().getSelectedItem().source().result());
            }
            return;
        }
        documents.getItems().clear();
        if (resultSelection != null) {
            resultSelection.accept(null);
        }
        refreshDetails(resultSupplier.get());
    }

    private void refreshDetails(DocumentResult result) {
        if (result == null) {
            document.setText("Document: -");
            category.setText("Category: -");
            status.setText("Status: -");
            identification.setText("Identification: -");
            geometry.setText("Geometry: -");
            trace.setText("Trace: -");
            fields.getItems().clear();
            fields.getSelectionModel().clearSelection();
        anchorTrace.getItems().clear();
        anchorTrace.getSelectionModel().clearSelection();
            resizeTraceTable(anchorTrace);
            identificationTrace.getItems().clear();
            identificationTrace.getSelectionModel().clearSelection();
            resizeTraceTable(identificationTrace);
            setTransformSummary(null);
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
        fields.getSelectionModel().clearSelection();
        anchorTrace.getItems().setAll(anchorTraceRows(result));
        resizeTraceTable(anchorTrace);
        anchorTrace.getSelectionModel().clearSelection();
        identificationTrace.getItems().setAll(identificationTraceRows(result));
        resizeTraceTable(identificationTrace);
        identificationTrace.getSelectionModel().clearSelection();
        setTransformSummary(result);
        var issueTexts = issueTexts(result);
        issues.getItems().setAll(issueTexts.isEmpty() ? List.of("No errors or warnings") : issueTexts);
    }

    private void configureDocuments() {
        documents.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        documents.setPrefHeight(130);
        documents.getColumns().add(documentColumn("Reference", "reference", 140));
        documents.getColumns().add(documentColumn("Path", "path", 240));
        documents.getColumns().add(documentColumn("Status", "status", 90));
        documents.getColumns().add(documentColumn("Category", "category", 120));
        documents.getColumns().add(documentColumn("Issues", "issues", 70));
        documents.setStyle(TEXT_COLOR_STYLE);
        documents.skinProperty().addListener((obs, old, skin) ->
            javafx.application.Platform.runLater(() -> documents.lookupAll(".column-header .label")
                .forEach(node -> node.setStyle(TEXT_COLOR_STYLE))));
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

    private void configureAnchorTrace() {
        anchorTrace.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        anchorTrace.setFixedCellSize(28);
        anchorTrace.setPrefHeight(traceTableHeight(1));
        anchorTrace.getColumns().add(anchorColumn("Used", "usedMark", 60));
        anchorTrace.getColumns().add(anchorColumn("Anchor", "anchorId", 140));
        anchorTrace.getColumns().add(anchorColumn("Status", "status", 95));
        anchorTrace.getColumns().add(anchorColumn("Required", "requiredMark", 85));
        anchorTrace.getColumns().add(anchorColumn("Confidence", "confidence", 110));
        anchorTrace.getColumns().add(anchorColumn("X", "x", 70));
        anchorTrace.getColumns().add(anchorColumn("Y", "y", 70));
        anchorTrace.getColumns().add(anchorColumn("W", "width", 70));
        anchorTrace.getColumns().add(anchorColumn("H", "height", 70));
        anchorTrace.getColumns().add(anchorOcrColumn());
        anchorTrace.setStyle(TEXT_COLOR_STYLE);
        forwardVerticalScrollToParent(anchorTrace);
        anchorTrace.skinProperty().addListener((obs, old, skin) ->
            javafx.application.Platform.runLater(() -> anchorTrace.lookupAll(".column-header .label")
                .forEach(node -> node.setStyle(TEXT_COLOR_STYLE))));
    }

    private void configureIdentificationTrace() {
        identificationTrace.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        identificationTrace.setFixedCellSize(28);
        identificationTrace.setPrefHeight(traceTableHeight(1));
        identificationTrace.getColumns().add(identificationColumn("Category", "categoryId", 120));
        identificationTrace.getColumns().add(identificationColumn("Group", "group", 70));
        identificationTrace.getColumns().add(identificationColumn("Condition", "condition", 90));
        identificationTrace.getColumns().add(identificationColumn("Matched", "matched", 85));
        identificationTrace.getColumns().add(identificationColumn("Detector", "detectorId", 100));
        identificationTrace.getColumns().add(identificationColumn("Matcher", "matcherId", 100));
        identificationTrace.getColumns().add(identificationColumn("Matcher Status", "matcherStatus", 130));
        identificationTrace.getColumns().add(identificationColumn("Expected", "expectedText", 160));
        identificationTrace.getColumns().add(identificationColumn("Search Region", "searchRegionText", 180));
        identificationTrace.getColumns().add(identificationOcrColumn());
        identificationTrace.setStyle(TEXT_COLOR_STYLE);
        forwardVerticalScrollToParent(identificationTrace);
        identificationTrace.skinProperty().addListener((obs, old, skin) ->
            javafx.application.Platform.runLater(() -> identificationTrace.lookupAll(".column-header .label")
                .forEach(node -> node.setStyle(TEXT_COLOR_STYLE))));
    }

    private TableColumn<FieldRow, String> column(String title, String property, double width) {
        var column = new TableColumn<FieldRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setStyle(TEXT_COLOR_STYLE);
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<DocumentRow, String> documentColumn(String title, String property, double width) {
        var column = new TableColumn<DocumentRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setStyle(TEXT_COLOR_STYLE);
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<AnchorTraceRow, String> anchorColumn(String title, String property, double width) {
        var column = new TableColumn<AnchorTraceRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setStyle(TEXT_COLOR_STYLE);
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<IdentificationTraceRow, String> identificationColumn(String title, String property, double width) {
        var column = new TableColumn<IdentificationTraceRow, String>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setStyle(TEXT_COLOR_STYLE);
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<AnchorTraceRow, Void> anchorOcrColumn() {
        var column = new TableColumn<AnchorTraceRow, Void>("OCR");
        column.setPrefWidth(70);
        column.setCellFactory(table -> new TableCell<>() {
            private final Button button = ocrPreviewButton();

            {
                button.setTooltip(new javafx.scene.control.Tooltip("Show OCR text"));
                button.setOnAction(event -> {
                    var row = getTableView().getItems().get(getIndex());
                    showOcrDialog(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                var row = getTableView().getItems().get(getIndex());
                button.setDisable(row.ocrText() == null || row.ocrText().isBlank());
                setGraphic(button);
            }
        });
        return column;
    }

    private TableColumn<IdentificationTraceRow, Void> identificationOcrColumn() {
        var column = new TableColumn<IdentificationTraceRow, Void>("OCR");
        column.setPrefWidth(70);
        column.setCellFactory(table -> new TableCell<>() {
            private final Button button = ocrPreviewButton();

            {
                button.setTooltip(new javafx.scene.control.Tooltip("Show OCR text"));
                button.setOnAction(event -> {
                    var row = getTableView().getItems().get(getIndex());
                    showOcrDialog("Identification OCR", "Condition: " + row.conditionPath(), row.ocrText());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                var row = getTableView().getItems().get(getIndex());
                button.setDisable(row.ocrText() == null || row.ocrText().isBlank());
                setGraphic(button);
            }
        });
        return column;
    }

    private static Button ocrPreviewButton() {
        var icon = new SVGPath();
        icon.setContent("M9.5 3a6.5 6.5 0 1 0 0 13a6.5 6.5 0 0 0 0-13z M9.5 5a4.5 4.5 0 1 1 0 9a4.5 4.5 0 0 1 0-9z M14.3 14.3l4.4 4.4l-1.4 1.4l-4.4-4.4z");
        icon.setFill(Color.web("#111827"));
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);
        var button = new Button();
        button.setGraphic(icon);
        button.setMinSize(28, 28);
        button.setPrefSize(28, 28);
        button.setMaxSize(28, 28);
        return button;
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

    private List<IdentificationTraceRow> identificationTraceRows(DocumentResult result) {
        if (result.trace() == null || result.trace().entries() == null) {
            return List.of();
        }
        return result.trace().entries().stream()
            .filter(entry -> entry.stage() == pl.sk.ocr.domain.issue.ProcessingStage.CATEGORY_IDENTIFICATION)
            .filter(entry -> isIdentificationConditionTrace(entry.attributes()))
            .map(entry -> {
                var attributes = entry.attributes();
                return new IdentificationTraceRow(
                    String.valueOf(attributes.getOrDefault("categoryId", "")),
                    String.valueOf(attributes.getOrDefault("group", "")),
                    String.valueOf(attributes.getOrDefault("condition", "")),
                    booleanValue(attributes.get("matched")) ? "✓" : "",
                    String.valueOf(attributes.getOrDefault("detectorId", "")),
                    String.valueOf(attributes.getOrDefault("matcherId", "")),
                    String.valueOf(attributes.getOrDefault("matcherStatus", "")),
                    String.valueOf(attributes.getOrDefault("expectedText", "")),
                    String.valueOf(attributes.getOrDefault("searchRegion", "")),
                    region(attributes, "search"),
                    String.valueOf(attributes.getOrDefault("ocrTextInRegion", ""))
                );
            })
            .toList();
    }

    private List<AnchorTraceRow> anchorTraceRows(DocumentResult result) {
        if (result.trace() == null || result.trace().entries() == null) {
            return List.of();
        }
        return result.trace().entries().stream()
            .filter(entry -> entry.stage() == ProcessingStage.ANCHOR_DETECTION)
            .map(entry -> {
                var attributes = entry.attributes();
                var bounds = region(attributes, "detected");
                var searchRegion = region(attributes, "search");
                return new AnchorTraceRow(
                    String.valueOf(attributes.getOrDefault("anchorId", "")),
                    booleanValue(attributes.get("matched")),
                    booleanValue(attributes.get("used")),
                    booleanValue(attributes.get("required")),
                    String.valueOf(attributes.getOrDefault("confidence", "")),
                    bounds,
                    searchRegion,
                    String.valueOf(attributes.getOrDefault("ocrTextInSearchRegion", ""))
                );
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private void showOcrDialog(AnchorTraceRow row) {
        showOcrDialog("Anchor OCR", "Anchor: " + row.anchorId(), row.ocrText());
    }

    private void showOcrDialog(String title, String header, String ocrText) {
        var dialog = new Dialog<Void>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(640, 420);
        var text = new TextArea(ocrText == null ? "" : ocrText);
        text.setEditable(false);
        text.setWrapText(true);
        text.setStyle(TEXT_COLOR_STYLE);
        dialog.getDialogPane().setContent(text);
        dialog.showAndWait();
    }

    private VBox transformSummary() {
        var details = new VBox(2, transformMain, transformAffine);
        details.setPadding(new javafx.geometry.Insets(0, 0, 0, 8));
        details.setBorder(new Border(new BorderStroke(
            Color.web("#9ca3af"),
            BorderStrokeStyle.SOLID,
            null,
            new BorderWidths(0, 0, 0, 2)
        )));
        var box = new VBox(3, label("Transform:"), details);
        box.setPadding(new javafx.geometry.Insets(2, 0, 2, 0));
        return box;
    }

    private void setTransformSummary(DocumentResult result) {
        if (result == null || result.trace() == null || result.trace().entries() == null) {
            transformMain.setText("dx = -, dy = -, scaleX = -, scaleY = -");
            transformAffine.setText("affine - [a = -, b = -, c = -, d = -]");
            return;
        }
        var attributes = result.trace().entries().stream()
            .filter(entry -> entry.stage() == ProcessingStage.GEOMETRY_RESOLUTION)
            .findFirst()
            .map(entry -> entry.attributes())
            .orElse(null);
        if (attributes == null) {
            transformMain.setText("dx = -, dy = -, scaleX = -, scaleY = -");
            transformAffine.setText("affine - [a = -, b = -, c = -, d = -]");
            return;
        }
        transformMain.setText("dx = " + formatNumber(attributes.get("translateX"))
            + ", dy = " + formatNumber(attributes.get("translateY"))
            + ", scaleX = " + formatNumber(attributes.get("scaleX"))
            + ", scaleY = " + formatNumber(attributes.get("scaleY")));
        transformAffine.setText("affine - [a = " + formatNumber(attributes.get("affineA"))
            + ", b = " + formatNumber(attributes.get("affineB"))
            + ", c = " + formatNumber(attributes.get("affineC"))
            + ", d = " + formatNumber(attributes.get("affineD"))
            + "]");
    }

    private void resizeTraceTable(TableView<?> table) {
        table.setPrefHeight(traceTableHeight(table.getItems().size()));
    }

    private double traceTableHeight(int rows) {
        var visibleRows = Math.max(1, rows);
        return 34 + visibleRows * 28 + 18;
    }

    private void forwardVerticalScrollToParent(TableView<?> table) {
        table.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (Math.abs(event.getDeltaY()) <= Math.abs(event.getDeltaX()) || event.isShiftDown()) {
                return;
            }
            var parentScroll = parentScrollPane(table);
            if (parentScroll == null) {
                return;
            }
            var viewport = parentScroll.getViewportBounds().getHeight();
            var content = parentScroll.getContent() == null ? viewport : parentScroll.getContent().getBoundsInLocal().getHeight();
            var scrollable = Math.max(1, content - viewport);
            parentScroll.setVvalue(clamp(parentScroll.getVvalue() - event.getDeltaY() / scrollable));
            event.consume();
        });
    }

    private javafx.scene.control.ScrollPane parentScrollPane(Node node) {
        var parent = node.getParent();
        while (parent != null) {
            if (parent instanceof javafx.scene.control.ScrollPane scrollPane) {
                return scrollPane;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private Region region(java.util.Map<String, Object> attributes, String prefix) {
        if (attributes == null) {
            return null;
        }
        var x = number(attributes.get(prefix + "X"));
        var y = number(attributes.get(prefix + "Y"));
        var width = number(attributes.get(prefix + "Width"));
        var height = number(attributes.get(prefix + "Height"));
        if (x == null || y == null || width == null || height == null) {
            return null;
        }
        return new Region(x, y, width, height);
    }

    private Double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value instanceof String text && Boolean.parseBoolean(text);
    }

    private String formatNumber(Object value) {
        var number = number(value);
        if (number == null) {
            return "-";
        }
        if (Math.abs(number - Math.rint(number)) < 0.0001) {
            return String.valueOf(Math.round(number));
        }
        return String.format(java.util.Locale.ROOT, "%.4f", number);
    }

    private boolean isIdentificationConditionTrace(java.util.Map<String, Object> attributes) {
        return attributes != null
            && attributes.containsKey("categoryId")
            && attributes.containsKey("group")
            && attributes.containsKey("condition");
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

    public record AnchorTraceSelection(Region detectedRegion, Region searchRegion) {
    }

    public record IdentificationTraceRow(String categoryId, String group, String condition, String matched, String detectorId,
                                         String matcherId, String matcherStatus, String expectedText, String searchRegionText,
                                         Region searchRegion, String ocrText) {
        public String getCategoryId() {
            return categoryId;
        }

        public String getGroup() {
            return group;
        }

        public String getCondition() {
            return condition;
        }

        public String getMatched() {
            return matched;
        }

        public String getDetectorId() {
            return detectorId;
        }

        public String getMatcherId() {
            return matcherId;
        }

        public String getMatcherStatus() {
            return matcherStatus;
        }

        public String getExpectedText() {
            return expectedText;
        }

        public String getSearchRegionText() {
            return searchRegionText;
        }

        public String conditionPath() {
            return categoryId + " / group " + group + " / condition " + condition;
        }
    }

    public record AnchorTraceRow(String anchorId, boolean matched, boolean used, boolean required, String confidence, Region bounds,
                                 Region searchRegion, String ocrText) {
        public String getUsedMark() {
            return used ? "✓" : "";
        }

        public String getAnchorId() {
            return anchorId;
        }

        public String getStatus() {
            return matched ? "MATCHED" : "MISSING";
        }

        public String getRequiredMark() {
            return required ? "true" : "";
        }

        public String getConfidence() {
            return confidence;
        }

        public String getX() {
            return coordinate(bounds == null ? null : bounds.x());
        }

        public String getY() {
            return coordinate(bounds == null ? null : bounds.y());
        }

        public String getWidth() {
            return coordinate(bounds == null ? null : bounds.width());
        }

        public String getHeight() {
            return coordinate(bounds == null ? null : bounds.height());
        }

        private String coordinate(Double value) {
            if (value == null) {
                return "";
            }
            return String.valueOf(Math.round(value));
        }
    }

    public record FieldRow(FieldResult source, String field, String status, String value, String issues) {
        static FieldRow from(FieldResult result) {
            return new FieldRow(
                result,
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

    public record DocumentRow(CategoryReferenceDocumentTestResult source, String reference, String path, String status,
                              String category, String issues) {
        static DocumentRow from(CategoryReferenceDocumentTestResult result) {
            var documentResult = result.result();
            return new DocumentRow(
                result,
                result.referenceDocumentId(),
                result.referenceDocumentPath(),
                documentResult.status().name(),
                documentResult.categoryId() == null ? "" : documentResult.categoryId().value(),
                String.valueOf(issueCount(documentResult))
            );
        }

        private static int issueCount(DocumentResult result) {
            return result.issues().size() + result.fields().stream().mapToInt(field -> field.issues().size()).sum();
        }

        public String getReference() {
            return reference;
        }

        public String getPath() {
            return path;
        }

        public String getStatus() {
            return status;
        }

        public String getCategory() {
            return category;
        }

        public String getIssues() {
            return issues;
        }
    }
}
