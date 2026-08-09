package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.FieldDto;
import pl.sk.ocr.config.dto.OutputDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

public final class FieldPropertiesPanel implements DetailsPanel {
    private final CategoryEditorViewModel viewModel;
    private final Supplier<List<FieldDto>> fields;
    private final Supplier<Selection> selection;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final Runnable refreshAll;
    private final Consumer<String> pendingSelection;
    private final Runnable activateRegionDrawing;
    private final Function<String, Node> iconFactory;
    private final ExtensionPicker extensionPicker;
    private final ExtensionParametersForm parametersForm;
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
                                Supplier<Selection> selection, Label detailsInfo, Runnable afterChange,
                                Runnable refreshAll, Consumer<String> pendingSelection,
                                Runnable activateRegionDrawing, Function<String, Node> iconFactory,
                                ExtensionRegistry extensionRegistry) {
        this.viewModel = viewModel;
        this.fields = fields;
        this.selection = selection;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        this.refreshAll = refreshAll;
        this.pendingSelection = pendingSelection;
        this.activateRegionDrawing = activateRegionDrawing;
        this.iconFactory = iconFactory;
        this.extensionPicker = new ExtensionPicker(extensionRegistry);
        this.parametersForm = new ExtensionParametersForm(extensionRegistry);
        configure();
    }

    @Override
    public Node view() {
        var section = section("Fields");
        var selected = selection.get();
        if (selected.type() == SelectionType.FIELD && selectedField() != null) {
            fieldControls(section, selected.fieldIndex());
        } else if (selected.type().isPipeline()) {
            pipelineControls(section, selected);
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

    private void pipelineControls(VBox section, Selection selected) {
        var field = field(selected.fieldIndex());
        if (field == null) {
            addFormRow(section, "Fields", fieldsCount);
            return;
        }
        var pipeline = selected.type() == SelectionType.PIPELINE_STEP ? selected.pipeline() : Pipeline.from(selected.type());
        var steps = pipeline.steps(field);
        addFormRow(section, pipeline.title(), textLabel(String.valueOf(steps.size())));
        for (int i = 0; i < steps.size(); i++) {
            var stepIndex = i;
            var step = steps.get(i);
            var selectedStep = selected.type() == SelectionType.PIPELINE_STEP && selected.stepIndex() == stepIndex;
            var label = textLabel(step == null ? "" : nullToEmpty(step.id()));
            var choose = button("Choose", () -> choosePipelineStep(pipeline, selected.fieldIndex(), stepIndex, step == null ? null : step.id()));
            var moveUp = button("Move Up", () -> movePipelineStep(pipeline, selected.fieldIndex(), stepIndex, stepIndex - 1));
            var moveDown = button("Move Down", () -> movePipelineStep(pipeline, selected.fieldIndex(), stepIndex, stepIndex + 1));
            var remove = button("Remove", () -> removePipelineStep(pipeline, selected.fieldIndex(), stepIndex));
            moveUp.setDisable(stepIndex <= 0);
            moveDown.setDisable(stepIndex >= steps.size() - 1);
            var row = new HBox(8, label, choose, moveUp, moveDown, remove);
            row.setStyle(selectedStep ? "-fx-background-color: #dbeafe; -fx-border-color: #1f7aec; -fx-border-radius: 4; -fx-padding: 4;" : "-fx-padding: 4;");
            section.getChildren().add(row);
            if (selectedStep && step != null) {
                section.getChildren().add(parametersForm.view(step, ref -> replacePipelineStep(pipeline, selected.fieldIndex(), stepIndex, ref)));
            }
        }
        section.getChildren().add(button("Add " + pipeline.singular(), () -> addPipelineStep(pipeline, selected.fieldIndex())));
    }

    private Label textLabel(String text) {
        var label = new Label(text);
        label.setStyle("-fx-text-fill: #111827;");
        return label;
    }

    private FieldDto selectedField() {
        var index = selection.get().fieldIndex();
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

    private void addPipelineStep(Pipeline pipeline, int fieldIndex) {
        extensionPicker.chooseRef(pipeline.extensionType(), null).ifPresent(ref -> {
            pipeline.add(viewModel, fieldIndex, ref);
            pendingSelection.accept(pipeline.nodeId(fieldIndex, pipeline.steps(field(fieldIndex)).size() - 1));
            refreshAll.run();
        });
    }

    private void choosePipelineStep(Pipeline pipeline, int fieldIndex, int stepIndex, String currentId) {
        extensionPicker.chooseRef(pipeline.extensionType(), currentId).ifPresent(ref -> {
            var field = field(fieldIndex);
            if (field == null) {
                return;
            }
            var updated = new java.util.ArrayList<>(pipeline.steps(field));
            updated.set(stepIndex, ref);
            replacePipeline(fieldIndex, field, pipeline, updated);
            pendingSelection.accept(pipeline.nodeId(fieldIndex, stepIndex));
            refreshAll.run();
        });
    }

    private void replacePipelineStep(Pipeline pipeline, int fieldIndex, int stepIndex, ExtensionRefDto ref) {
        var field = field(fieldIndex);
        if (field == null) {
            return;
        }
        var updated = new java.util.ArrayList<>(pipeline.steps(field));
        updated.set(stepIndex, ref);
        replacePipeline(fieldIndex, field, pipeline, updated);
        pendingSelection.accept(pipeline.nodeId(fieldIndex, stepIndex));
        afterChange.run();
    }

    private void movePipelineStep(Pipeline pipeline, int fieldIndex, int fromIndex, int toIndex) {
        var field = field(fieldIndex);
        if (field == null || toIndex < 0 || toIndex >= pipeline.steps(field).size()) {
            return;
        }
        pipeline.move(viewModel, fieldIndex, fromIndex, toIndex);
        pendingSelection.accept(pipeline.nodeId(fieldIndex, toIndex));
        refreshAll.run();
    }

    private void removePipelineStep(Pipeline pipeline, int fieldIndex, int stepIndex) {
        pipeline.remove(viewModel, fieldIndex, stepIndex);
        pendingSelection.accept(pipeline.parentId(fieldIndex));
        refreshAll.run();
    }

    private void replacePipeline(int fieldIndex, FieldDto field, Pipeline pipeline, List<ExtensionRefDto> steps) {
        viewModel.replaceField(fieldIndex, switch (pipeline) {
            case IMAGE_PROCESSORS -> new FieldDto(field.id(), field.displayName(), field.page(), field.region(), field.required(),
                field.ocr(), field.output(), steps, list(field.transformers()), list(field.validators()));
            case TRANSFORMERS -> new FieldDto(field.id(), field.displayName(), field.page(), field.region(), field.required(),
                field.ocr(), field.output(), list(field.imageProcessors()), steps, list(field.validators()));
            case VALIDATORS -> new FieldDto(field.id(), field.displayName(), field.page(), field.region(), field.required(),
                field.ocr(), field.output(), list(field.imageProcessors()), list(field.transformers()), steps);
        });
    }

    private void applySelectedField() {
        if (refreshing || viewModel.draft() == null || selectedField() == null) {
            return;
        }
        var index = selection.get().fieldIndex();
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

    public enum SelectionType {
        FIELDS,
        FIELD,
        FIELD_OCR,
        FIELD_OUTPUT,
        IMAGE_PROCESSORS,
        TRANSFORMERS,
        VALIDATORS,
        PIPELINE_STEP;

        boolean isPipeline() {
            return this == IMAGE_PROCESSORS || this == TRANSFORMERS || this == VALIDATORS || this == PIPELINE_STEP;
        }
    }

    public record Selection(SelectionType type, int fieldIndex, int stepIndex, Pipeline pipeline) {
    }

    public enum Pipeline {
        IMAGE_PROCESSORS("Image Processors", "Image Processor", ExtensionType.IMAGE_PROCESSOR),
        TRANSFORMERS("Transformers", "Transformer", ExtensionType.VALUE_TRANSFORMER),
        VALIDATORS("Validators", "Validator", ExtensionType.VALIDATOR);

        private final String title;
        private final String singular;
        private final ExtensionType extensionType;

        Pipeline(String title, String singular, ExtensionType extensionType) {
            this.title = title;
            this.singular = singular;
            this.extensionType = extensionType;
        }

        static Pipeline from(SelectionType type) {
            return switch (type) {
                case IMAGE_PROCESSORS -> IMAGE_PROCESSORS;
                case TRANSFORMERS -> TRANSFORMERS;
                case VALIDATORS -> VALIDATORS;
                case PIPELINE_STEP -> throw new IllegalStateException("Pipeline step requires explicit pipeline");
                default -> throw new IllegalArgumentException("Unsupported pipeline selection: " + type);
            };
        }

        List<ExtensionRefDto> steps(FieldDto field) {
            return switch (this) {
                case IMAGE_PROCESSORS -> field.imageProcessors() == null ? List.of() : field.imageProcessors();
                case TRANSFORMERS -> field.transformers() == null ? List.of() : field.transformers();
                case VALIDATORS -> field.validators() == null ? List.of() : field.validators();
            };
        }

        void add(CategoryEditorViewModel viewModel, int fieldIndex, ExtensionRefDto ref) {
            switch (this) {
                case IMAGE_PROCESSORS -> viewModel.addImageProcessor(fieldIndex, ref);
                case TRANSFORMERS -> viewModel.addTransformer(fieldIndex, ref);
                case VALIDATORS -> viewModel.addValidator(fieldIndex, ref);
            }
        }

        void move(CategoryEditorViewModel viewModel, int fieldIndex, int fromIndex, int toIndex) {
            switch (this) {
                case IMAGE_PROCESSORS -> viewModel.moveImageProcessor(fieldIndex, fromIndex, toIndex);
                case TRANSFORMERS -> viewModel.moveTransformer(fieldIndex, fromIndex, toIndex);
                case VALIDATORS -> viewModel.moveValidator(fieldIndex, fromIndex, toIndex);
            }
        }

        void remove(CategoryEditorViewModel viewModel, int fieldIndex, int stepIndex) {
            switch (this) {
                case IMAGE_PROCESSORS -> viewModel.removeImageProcessor(fieldIndex, stepIndex);
                case TRANSFORMERS -> viewModel.removeTransformer(fieldIndex, stepIndex);
                case VALIDATORS -> viewModel.removeValidator(fieldIndex, stepIndex);
            }
        }

        String title() {
            return title;
        }

        String singular() {
            return singular;
        }

        ExtensionType extensionType() {
            return extensionType;
        }

        String parentId(int fieldIndex) {
            return switch (this) {
                case IMAGE_PROCESSORS -> "field." + fieldIndex + ".imageProcessors";
                case TRANSFORMERS -> "field." + fieldIndex + ".transformers";
                case VALIDATORS -> "field." + fieldIndex + ".validators";
            };
        }

        String nodeId(int fieldIndex, int stepIndex) {
            return parentId(fieldIndex) + "." + stepIndex;
        }
    }
}
