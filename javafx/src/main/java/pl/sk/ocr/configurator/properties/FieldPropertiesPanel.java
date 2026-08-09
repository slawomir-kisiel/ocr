package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.FieldDto;
import pl.sk.ocr.config.dto.OutputDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;

public final class FieldPropertiesPanel implements DetailsPanel {
    private final CategoryEditorViewModel viewModel;
    private final Supplier<List<FieldDto>> fields;
    private final IntSupplier selectedFieldIndex;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final Runnable refreshAll;
    private final Consumer<String> pendingSelection;
    private final Runnable activateRegionDrawing;
    private final Function<String, Node> iconFactory;
    private final Label fieldsCount = new Label();
    private final Button addField = new Button("Add Field");
    private final TextField fieldId = new TextField();
    private final TextField fieldDisplayName = new TextField();
    private final TextField fieldPage = new TextField();
    private final CheckBox fieldRequired = new CheckBox();
    private final CheckBox outputExported = new CheckBox();
    private final TextField outputColumnName = new TextField();
    private final Spinner<Integer> fieldRegionX = regionSpinner();
    private final Spinner<Integer> fieldRegionY = regionSpinner();
    private final Spinner<Integer> fieldRegionWidth = regionSpinner();
    private final Spinner<Integer> fieldRegionHeight = regionSpinner();
    private final Button drawFieldRegion = new Button();
    private boolean refreshing;

    public FieldPropertiesPanel(CategoryEditorViewModel viewModel, Supplier<List<FieldDto>> fields,
                                IntSupplier selectedFieldIndex, Label detailsInfo, Runnable afterChange,
                                Runnable refreshAll, Consumer<String> pendingSelection,
                                Runnable activateRegionDrawing, Function<String, Node> iconFactory) {
        this.viewModel = viewModel;
        this.fields = fields;
        this.selectedFieldIndex = selectedFieldIndex;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        this.refreshAll = refreshAll;
        this.pendingSelection = pendingSelection;
        this.activateRegionDrawing = activateRegionDrawing;
        this.iconFactory = iconFactory;
        configure();
    }

    @Override
    public Node view() {
        var section = section("Fields");
        var index = selectedFieldIndex.getAsInt();
        if (selectedField() != null) {
            fieldControls(section, index);
        } else {
            addFormRow(section, "Fields", fieldsCount);
            detachFromParent(addField);
            section.getChildren().add(addField);
        }
        return new VBox(10, section, detailsInfo);
    }

    @Override
    public void refresh() {
        refreshing = true;
        try {
            fieldsCount.setText(String.valueOf(fields.get().size()));
            var field = selectedField();
            fieldId.setText(field == null ? "" : nullToEmpty(field.id()));
            fieldDisplayName.setText(field == null ? "" : nullToEmpty(field.displayName()));
            fieldPage.setText(field == null || field.page() == null ? "" : field.page().toString());
            fieldRequired.setSelected(field != null && Boolean.TRUE.equals(field.required()));
            var output = field == null ? null : field.output();
            outputExported.setSelected(output != null && Boolean.TRUE.equals(output.exported()));
            outputColumnName.setText(output == null ? "" : nullToEmpty(output.columnName()));
            var region = field == null ? null : field.region();
            setRegionSpinnerText(fieldRegionX, region == null ? "" : formatRegionNumber(region.x()));
            setRegionSpinnerText(fieldRegionY, region == null ? "" : formatRegionNumber(region.y()));
            setRegionSpinnerText(fieldRegionWidth, region == null ? "" : formatRegionNumber(region.width()));
            setRegionSpinnerText(fieldRegionHeight, region == null ? "" : formatRegionNumber(region.height()));
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void commit() {
        applySelectedField();
    }

    public void replaceFieldRegion(int index, RegionDto region) {
        viewModel.updateFieldRegion(index, region);
    }

    public void updateFieldRegionFromViewer(RegionDto region, boolean commit) {
        updateRegionControls(region);
        if (commit) {
            applySelectedField();
        }
    }

    private void configure() {
        fieldsCount.setStyle("-fx-text-fill: #111827;");
        installTooltip(fieldsCount, "Number of fields configured for extraction.");
        installTooltip(addField, "Add a new field.");
        installTooltip(fieldId, "Field id used by output and processing diagnostics.");
        installTooltip(fieldDisplayName, "Human-readable field name.");
        installTooltip(fieldPage, "Page where this field should be extracted.");
        installTooltip(fieldRequired, "Whether missing or invalid field value should fail processing.");
        installTooltip(outputExported, "Whether this field should be included in exported output.");
        installTooltip(outputColumnName, "Output column name used when the field is exported.");
        installTooltip(fieldRegionX, "Field region X coordinate in image/reference coordinates.");
        installTooltip(fieldRegionY, "Field region Y coordinate in image/reference coordinates.");
        installTooltip(fieldRegionWidth, "Field region width in image/reference coordinates.");
        installTooltip(fieldRegionHeight, "Field region height in image/reference coordinates.");
        drawFieldRegion.setGraphic(iconFactory.apply("mode-draw-region.svg"));
        drawFieldRegion.setTooltip(new Tooltip("Draw field region on document preview."));
        drawFieldRegion.setMinSize(36, 32);
        drawFieldRegion.setPrefSize(36, 32);
        drawFieldRegion.setMaxSize(36, 32);

        addField.setOnAction(event -> addField());
        addDraftListener(fieldId, this::applySelectedField);
        addDraftListener(fieldDisplayName, this::applySelectedField);
        addDraftListener(fieldPage, this::applySelectedField);
        fieldRequired.selectedProperty().addListener((obs, old, value) -> applySelectedField());
        outputExported.selectedProperty().addListener((obs, old, value) -> applySelectedField());
        addDraftListener(outputColumnName, this::applySelectedField);
        addSpinnerListener(fieldRegionX, this::applySelectedField);
        addSpinnerListener(fieldRegionY, this::applySelectedField);
        addSpinnerListener(fieldRegionWidth, this::applySelectedField);
        addSpinnerListener(fieldRegionHeight, this::applySelectedField);
        drawFieldRegion.setOnAction(event -> activateRegionDrawing.run());
    }

    private void fieldControls(VBox section, int fieldIndex) {
        var fieldContent = new VBox(8);
        addFormRow(fieldContent, "ID", fieldId);
        addFormRow(fieldContent, "Display Name", fieldDisplayName);
        addFormRow(fieldContent, "Page", fieldPage);
        addFormRow(fieldContent, "Required", fieldRequired);
        section.getChildren().add(titledPane("Field", fieldContent));

        var regionContent = new VBox(8);
        addFormRow(regionContent, "X", fieldRegionX);
        addFormRow(regionContent, "Y", fieldRegionY);
        addFormRow(regionContent, "Width", fieldRegionWidth);
        addFormRow(regionContent, "Height", fieldRegionHeight);
        detachFromParent(drawFieldRegion);
        regionContent.getChildren().add(drawFieldRegion);
        section.getChildren().add(titledPane("Region", regionContent));

        var outputContent = new VBox(8);
        addFormRow(outputContent, "Exported", outputExported);
        addFormRow(outputContent, "Column Name", outputColumnName);
        section.getChildren().add(titledPane("Output", outputContent));

        var add = button("Add Field", this::addField);
        var copy = button("Copy Field", () -> copyField(fieldIndex));
        var moveUp = button("Move Up", () -> moveField(fieldIndex, fieldIndex - 1));
        var moveDown = button("Move Down", () -> moveField(fieldIndex, fieldIndex + 1));
        var remove = button("Remove Field", () -> removeField(fieldIndex));
        moveUp.setDisable(fieldIndex <= 0);
        moveDown.setDisable(fieldIndex >= fields.get().size() - 1);
        section.getChildren().add(new HBox(8, add, copy, moveUp, moveDown, remove));
    }

    private Button button(String text, Runnable action) {
        var button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private FieldDto selectedField() {
        var index = selectedFieldIndex.getAsInt();
        var fieldList = fields.get();
        if (index < 0 || index >= fieldList.size()) {
            return null;
        }
        return fieldList.get(index);
    }

    private void addField() {
        if (viewModel.draft() == null) {
            return;
        }
        var index = fields.get().size();
        var id = uniqueFieldId(index + 1);
        viewModel.addField(new FieldDto(id, id, viewModel.session().currentPage(), null, true, null,
            new OutputDto(true, id), List.of(), List.of(), List.of()));
        pendingSelection.accept("field." + index);
        refreshAll.run();
    }

    private void copyField(int sourceIndex) {
        var source = field(sourceIndex);
        if (source == null) {
            return;
        }
        var newIndex = fields.get().size();
        var newId = uniqueFieldId(newIndex + 1);
        viewModel.addField(new FieldDto(newId, copyLabel(source.displayName(), newId), source.page(),
            source.region(), source.required(), source.ocr(), copyOutput(source.output(), newId),
            list(source.imageProcessors()), list(source.transformers()), list(source.validators())));
        pendingSelection.accept("field." + newIndex);
        refreshAll.run();
    }

    private void moveField(int fromIndex, int toIndex) {
        if (toIndex < 0 || toIndex >= fields.get().size()) {
            return;
        }
        viewModel.moveField(fromIndex, toIndex);
        pendingSelection.accept("field." + toIndex);
        refreshAll.run();
    }

    private void removeField(int index) {
        viewModel.removeField(index);
        pendingSelection.accept("fields");
        refreshAll.run();
    }

    private void applySelectedField() {
        if (refreshing || viewModel.draft() == null || selectedField() == null) {
            return;
        }
        var index = selectedFieldIndex.getAsInt();
        var current = selectedField();
        viewModel.replaceField(index, new FieldDto(blankToNull(fieldId.getText()), blankToNull(fieldDisplayName.getText()),
            parseInteger(fieldPage.getText()), fieldRegion(), fieldRequired.isSelected(), current.ocr(), output(),
            list(current.imageProcessors()), list(current.transformers()), list(current.validators())));
        pendingSelection.accept("field." + index);
        afterChange.run();
    }

    private FieldDto field(int index) {
        var fieldList = fields.get();
        return index < 0 || index >= fieldList.size() ? null : fieldList.get(index);
    }

    private RegionDto fieldRegion() {
        if (blankToNull(fieldRegionX.getEditor().getText()) == null
            || blankToNull(fieldRegionY.getEditor().getText()) == null
            || blankToNull(fieldRegionWidth.getEditor().getText()) == null
            || blankToNull(fieldRegionHeight.getEditor().getText()) == null) {
            return null;
        }
        return new RegionDto(parseInteger(fieldRegionX.getEditor().getText()), parseInteger(fieldRegionY.getEditor().getText()),
            parseInteger(fieldRegionWidth.getEditor().getText()), parseInteger(fieldRegionHeight.getEditor().getText()));
    }

    private OutputDto output() {
        return new OutputDto(outputExported.isSelected(), blankToNull(outputColumnName.getText()));
    }

    private OutputDto copyOutput(OutputDto output, String newId) {
        if (output == null) {
            return new OutputDto(true, newId);
        }
        return new OutputDto(output.exported(), output.columnName() == null || output.columnName().isBlank() ? newId : uniqueFieldId(newId));
    }

    private void updateRegionControls(RegionDto region) {
        refreshing = true;
        try {
            setRegionSpinnerText(fieldRegionX, formatRegionNumber(region.x()));
            setRegionSpinnerText(fieldRegionY, formatRegionNumber(region.y()));
            setRegionSpinnerText(fieldRegionWidth, formatRegionNumber(region.width()));
            setRegionSpinnerText(fieldRegionHeight, formatRegionNumber(region.height()));
        } finally {
            refreshing = false;
        }
    }

    private String uniqueFieldId(int seed) {
        return uniqueFieldId("field-" + seed);
    }

    private String uniqueFieldId(String seed) {
        var existing = fields.get().stream().map(FieldDto::id).collect(java.util.stream.Collectors.toSet());
        var candidate = seed;
        var suffix = 2;
        while (existing.contains(candidate)) {
            candidate = seed + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String copyLabel(String value, String fallback) {
        var base = value == null || value.isBlank() ? fallback : value + " Copy";
        var existing = fields.get().stream().map(FieldDto::displayName).collect(java.util.stream.Collectors.toSet());
        var candidate = base;
        var suffix = 2;
        while (existing.contains(candidate)) {
            candidate = base + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        return text == null ? null : Integer.parseInt(text);
    }

    private String formatRegionNumber(double value) {
        return String.valueOf(Math.round(value));
    }

    private <T> List<T> list(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
