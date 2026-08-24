package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pl.sk.ocr.config.dto.ConditionDto;
import pl.sk.ocr.config.dto.ConditionGroupDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

public final class IdentificationPropertiesPanel implements DetailsPanel {
    private final CategoryEditorViewModel viewModel;
    private final Supplier<Selection> selection;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final Runnable refreshAll;
    private final Consumer<String> pendingSelection;
    private final Runnable activateConditionSearchRegionDrawing;
    private final Function<String, Node> iconFactory;
    private final ExtensionRegistry extensionRegistry;
    private final ExtensionPicker extensionPicker;
    private final ExtensionParametersForm parametersForm;
    private final Label groupsCount = new Label();
    private final Button addGroup = new Button("Add Group");
    private final Button removeLastGroup = new Button("Remove Last Group");
    private final ComboBox<String> conditionType = new ComboBox<>();
    private final TextField conditionPage = new TextField();
    private final TextField conditionExpectedText = new TextField();
    private final TextField conditionMatcherId = new TextField();
    private final TextField conditionDetectorId = new TextField();
    private final Button pickMatcher = new Button("...");
    private final Button pickDetector = new Button("...");
    private final Spinner<Integer> searchRegionX = regionSpinner();
    private final Spinner<Integer> searchRegionY = regionSpinner();
    private final Spinner<Integer> searchRegionWidth = regionSpinner();
    private final Spinner<Integer> searchRegionHeight = regionSpinner();
    private final Button drawSearchRegion = new Button();
    private final Button symmetricResize = new Button();
    private boolean refreshing;
    private boolean symmetricResizeEnabled;
    private boolean adjustingRegionSpinners;

    public IdentificationPropertiesPanel(CategoryEditorViewModel viewModel, Supplier<Selection> selection,
                                         Label detailsInfo, Runnable afterChange, Runnable refreshAll,
                                         Consumer<String> pendingSelection,
                                         Runnable activateConditionSearchRegionDrawing,
                                         Function<String, Node> iconFactory,
                                         ExtensionRegistry extensionRegistry) {
        this.viewModel = viewModel;
        this.selection = selection;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        this.refreshAll = refreshAll;
        this.pendingSelection = pendingSelection;
        this.activateConditionSearchRegionDrawing = activateConditionSearchRegionDrawing;
        this.iconFactory = iconFactory;
        this.extensionRegistry = extensionRegistry;
        this.extensionPicker = new ExtensionPicker(extensionRegistry);
        this.parametersForm = new ExtensionParametersForm(extensionRegistry);
        configure();
    }

    @Override
    public Node view() {
        var section = section("Identification");
        var selected = selection.get();
        if (selected.type() == SelectionType.ROOT) {
            addFormRow(section, "Groups", groupsCount);
            detachFromParent(addGroup);
            detachFromParent(removeLastGroup);
            section.getChildren().add(new HBox(8, addGroup, removeLastGroup));
        } else if (selected.type() == SelectionType.GROUP) {
            groupControls(section, selected.groupIndex());
        } else if (selected.type() == SelectionType.CONDITION) {
            conditionControls(section, selected.groupIndex(), selected.conditionIndex());
        }
        return new VBox(10, section, detailsInfo);
    }

    @Override
    public void refresh() {
        refreshing = true;
        try {
            groupsCount.setText(String.valueOf(groups().size()));
            var condition = selectedCondition();
            conditionType.setValue(condition == null || condition.type() == null ? "TEXT" : condition.type());
            conditionPage.setText(condition == null || condition.page() == null ? "" : condition.page().toString());
            conditionExpectedText.setText(condition == null ? "" : nullToEmpty(condition.expectedText()));
            conditionMatcherId.setText(condition == null || condition.matcher() == null ? "" : nullToEmpty(condition.matcher().id()));
            conditionDetectorId.setText(condition == null || condition.detector() == null ? "" : nullToEmpty(condition.detector().id()));
            var region = condition == null ? null : condition.searchRegion();
            setRegionSpinnerText(searchRegionX, region == null ? "" : formatRegionNumber(region.x()));
            setRegionSpinnerText(searchRegionY, region == null ? "" : formatRegionNumber(region.y()));
            setRegionSpinnerText(searchRegionWidth, region == null ? "" : formatRegionNumber(region.width()));
            setRegionSpinnerText(searchRegionHeight, region == null ? "" : formatRegionNumber(region.height()));
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void commit() {
        applySelectedCondition();
    }

    public RegionDto conditionSearchRegion() {
        if (!hasCompleteConditionSearchRegion()) {
            return null;
        }
        return new RegionDto(parseInteger(searchRegionX.getEditor().getText()), parseInteger(searchRegionY.getEditor().getText()),
            parseInteger(searchRegionWidth.getEditor().getText()), parseInteger(searchRegionHeight.getEditor().getText()));
    }

    public boolean hasValidConditionSearchRegion() {
        try {
            return conditionSearchRegion() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void updateConditionSearchRegionFromViewer(RegionDto region, boolean commit) {
        refreshing = true;
        try {
            setRegionSpinnerText(searchRegionX, formatRegionNumber(region.x()));
            setRegionSpinnerText(searchRegionY, formatRegionNumber(region.y()));
            setRegionSpinnerText(searchRegionWidth, formatRegionNumber(region.width()));
            setRegionSpinnerText(searchRegionHeight, formatRegionNumber(region.height()));
        } finally {
            refreshing = false;
        }
        if (commit) {
            applySelectedCondition();
        }
    }

    public void replaceConditionSearchRegion(int groupIndex, int conditionIndex, RegionDto region) {
        var current = condition(groupIndex, conditionIndex);
        if (current != null) {
            viewModel.replaceCondition(groupIndex, conditionIndex, new ConditionDto(current.type(), current.page(),
                current.expectedText(), current.matcher(), current.detector(), region));
        }
    }

    private void configure() {
        installTooltip(groupsCount, "Number of OR groups in category identification.");
        installTooltip(addGroup, "Add a new OR group.");
        installTooltip(removeLastGroup, "Remove the last OR group.");
        conditionType.getItems().setAll("TEXT", "QR", "BARCODE");
        installTooltip(conditionType, "Condition type.");
        installTooltip(conditionPage, "Page where this condition should be evaluated. Empty means default behavior.");
        installTooltip(conditionExpectedText, "Text expected in OCR text or detector payload.");
        installTooltip(conditionMatcherId, "Matcher extension id used by this condition.");
        installTooltip(conditionDetectorId, "Detector extension id used by this condition.");
        installTooltip(pickMatcher, "Choose matcher extension from registry.");
        installTooltip(pickDetector, "Choose detector extension from registry.");
        installTooltip(searchRegionX, "Condition search region X coordinate.");
        installTooltip(searchRegionY, "Condition search region Y coordinate.");
        installTooltip(searchRegionWidth, "Condition search region width.");
        installTooltip(searchRegionHeight, "Condition search region height.");
        drawSearchRegion.setGraphic(iconFactory.apply("mode-draw-region.svg"));
        drawSearchRegion.setTooltip(new Tooltip("Draw condition search region on document preview."));
        drawSearchRegion.setMinSize(36, 32);
        drawSearchRegion.setPrefSize(36, 32);
        drawSearchRegion.setMaxSize(36, 32);
        symmetricResize.setGraphic(iconFactory.apply("lock-open.svg"));
        symmetricResize.setTooltip(new Tooltip("Enable symmetric region resize."));
        symmetricResize.setMinSize(36, 32);
        symmetricResize.setPrefSize(36, 32);
        symmetricResize.setMaxSize(36, 32);
        groupsCount.setStyle("-fx-text-fill: #111827;");
        addGroup.setOnAction(event -> {
            var newIndex = groups().size();
            viewModel.addIdentificationGroup(new ConditionGroupDto(List.of()));
            pendingSelection.accept("identification.group." + newIndex);
            refreshAll.run();
        });
        removeLastGroup.setOnAction(event -> {
            if (!groups().isEmpty()) {
                viewModel.removeIdentificationGroup(groups().size() - 1);
                pendingSelection.accept("identification");
                refreshAll.run();
            }
        });
        conditionType.valueProperty().addListener((obs, old, value) -> {
            applySelectedCondition();
            if (!refreshing) {
                refreshAll.run();
            }
        });
        addDraftListener(conditionPage, this::applySelectedCondition);
        addDraftListener(conditionExpectedText, this::applySelectedCondition);
        addDraftListener(conditionMatcherId, this::applySelectedCondition);
        addDraftListener(conditionDetectorId, this::applySelectedCondition);
        pickMatcher.setOnAction(event -> chooseExtension(ExtensionType.MATCHER, conditionMatcherId));
        pickDetector.setOnAction(event -> chooseExtension(ExtensionType.DETECTOR, conditionDetectorId));
        addRegionSpinnerListeners();
        drawSearchRegion.setOnAction(event -> activateConditionSearchRegionDrawing.run());
        symmetricResize.setOnAction(event -> toggleSymmetricResize());
    }

    private void addRegionSpinnerListeners() {
        searchRegionX.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.X, old, value));
        searchRegionY.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.Y, old, value));
        searchRegionWidth.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.WIDTH, old, value));
        searchRegionHeight.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.HEIGHT, old, value));
        searchRegionX.getEditor().textProperty().addListener((obs, old, value) -> applySelectedCondition());
        searchRegionY.getEditor().textProperty().addListener((obs, old, value) -> applySelectedCondition());
        searchRegionWidth.getEditor().textProperty().addListener((obs, old, value) -> applySelectedCondition());
        searchRegionHeight.getEditor().textProperty().addListener((obs, old, value) -> applySelectedCondition());
    }

    private void applyRegionSpinnerChange(RegionPart part, Integer oldValue, Integer newValue) {
        if (refreshing || adjustingRegionSpinners) {
            applySelectedCondition();
            return;
        }
        if (!symmetricResizeEnabled || oldValue == null || newValue == null || oldValue.equals(newValue) || !hasCompleteConditionSearchRegion()) {
            applySelectedCondition();
            return;
        }
        var delta = newValue - oldValue;
        adjustingRegionSpinners = true;
        try {
            switch (part) {
                case X -> setRegionSpinnerValue(searchRegionWidth, Math.max(1, parseInteger(searchRegionWidth.getEditor().getText()) - delta * 2));
                case Y -> setRegionSpinnerValue(searchRegionHeight, Math.max(1, parseInteger(searchRegionHeight.getEditor().getText()) - delta * 2));
                case WIDTH -> {
                    var widthDelta = normalizeSizeDelta(searchRegionWidth, oldValue, newValue);
                    setRegionSpinnerValue(searchRegionX, parseInteger(searchRegionX.getEditor().getText()) - widthDelta / 2);
                }
                case HEIGHT -> {
                    var heightDelta = normalizeSizeDelta(searchRegionHeight, oldValue, newValue);
                    setRegionSpinnerValue(searchRegionY, parseInteger(searchRegionY.getEditor().getText()) - heightDelta / 2);
                }
            }
        } finally {
            adjustingRegionSpinners = false;
        }
        applySelectedCondition();
    }

    private int normalizeSizeDelta(Spinner<Integer> spinner, int oldValue, int newValue) {
        var delta = newValue - oldValue;
        if (Math.abs(delta) == 1) {
            var normalized = delta > 0 ? 2 : -2;
            setRegionSpinnerValue(spinner, oldValue + normalized);
            return normalized;
        }
        if (Math.abs(delta) % 2 != 0) {
            var normalized = delta > 0 ? delta + 1 : delta - 1;
            setRegionSpinnerValue(spinner, oldValue + normalized);
            return normalized;
        }
        return delta;
    }

    private void setRegionSpinnerValue(Spinner<Integer> spinner, int value) {
        spinner.getValueFactory().setValue(value);
        spinner.getEditor().setText(String.valueOf(value));
    }

    private void toggleSymmetricResize() {
        symmetricResizeEnabled = !symmetricResizeEnabled;
        symmetricResize.setGraphic(iconFactory.apply(symmetricResizeEnabled ? "lock.svg" : "lock-open.svg"));
        symmetricResize.setTooltip(new Tooltip(symmetricResizeEnabled ? "Disable symmetric region resize." : "Enable symmetric region resize."));
    }

    private void groupControls(VBox section, int groupIndex) {
        var conditionsCount = groupIndex >= 0 && groupIndex < groups().size() && groups().get(groupIndex).conditions() != null
            ? groups().get(groupIndex).conditions().size()
            : 0;
        addFormRow(section, "Group", new Label(String.valueOf(groupIndex + 1)));
        addFormRow(section, "Conditions", new Label(String.valueOf(conditionsCount)));
        var addCondition = button("Add Condition", () -> {
            var newIndex = conditions(groupIndex).size();
            viewModel.addCondition(groupIndex, new ConditionDto("TEXT", viewModel.session().currentPage(), "", null, null, null));
            pendingSelection.accept("identification.group." + groupIndex + ".condition." + newIndex);
            refreshAll.run();
        });
        var moveUp = button("Move Up", () -> {
            if (groupIndex > 0) {
                viewModel.moveIdentificationGroup(groupIndex, groupIndex - 1);
                pendingSelection.accept("identification.group." + (groupIndex - 1));
                refreshAll.run();
            }
        });
        var moveDown = button("Move Down", () -> {
            if (groupIndex < groups().size() - 1) {
                viewModel.moveIdentificationGroup(groupIndex, groupIndex + 1);
                pendingSelection.accept("identification.group." + (groupIndex + 1));
                refreshAll.run();
            }
        });
        var remove = button("Remove Group", () -> {
            viewModel.removeIdentificationGroup(groupIndex);
            pendingSelection.accept("identification");
            refreshAll.run();
        });
        moveUp.setDisable(groupIndex <= 0);
        moveDown.setDisable(groupIndex >= groups().size() - 1);
        section.getChildren().add(new HBox(8, addCondition, moveUp, moveDown, remove));
    }

    private void conditionControls(VBox section, int groupIndex, int conditionIndex) {
        var identificationContent = new VBox(8);
        addFormRow(identificationContent, "Group", new Label(String.valueOf(groupIndex + 1)));
        addFormRow(identificationContent, "Condition", new Label(String.valueOf(conditionIndex + 1)));
        addFormRow(identificationContent, "Type", conditionType);
        addFormRow(identificationContent, "Page", conditionPage);
        var type = nullToDefault(conditionType.getValue(), "TEXT");
        var condition = condition(groupIndex, conditionIndex);
        if (!isTextCondition(type) && !hasSingleDetectorOption(type)) {
            addExtensionRow(identificationContent, "Detector", extensionInput(conditionDetectorId, pickDetector),
                condition == null ? null : condition.detector(), ExtensionType.DETECTOR, ref -> replaceConditionDetector(groupIndex, conditionIndex, ref));
        }
        addExtensionRow(identificationContent, "Matcher", extensionInput(conditionMatcherId, pickMatcher),
            condition == null ? null : condition.matcher(), ExtensionType.MATCHER, ref -> replaceConditionMatcher(groupIndex, conditionIndex, ref));
        addFormRow(identificationContent, "Expected Text", conditionExpectedText);
        section.getChildren().add(titledPane("Identification", identificationContent));

        var searchRegionContent = new VBox(8);
        detachFromParent(drawSearchRegion);
        detachFromParent(symmetricResize);
        var regionActions = new VBox(6, drawSearchRegion, symmetricResize);
        searchRegionContent.getChildren().add(regionRowsWithActions(searchRegionX, searchRegionY, searchRegionWidth, searchRegionHeight, regionActions));
        section.getChildren().add(titledPane("Search Region", searchRegionContent));
        var addCondition = iconButton("plus.svg", "Add condition", () -> {
            var newIndex = conditions(groupIndex).size();
            viewModel.addCondition(groupIndex, new ConditionDto("TEXT", viewModel.session().currentPage(), "", null, null, null));
            pendingSelection.accept("identification.group." + groupIndex + ".condition." + newIndex);
            refreshAll.run();
        });
        var moveUp = iconButton("angle-up.svg", "Move condition up", () -> {
            if (conditionIndex > 0) {
                viewModel.moveCondition(groupIndex, conditionIndex, conditionIndex - 1);
                pendingSelection.accept("identification.group." + groupIndex + ".condition." + (conditionIndex - 1));
                refreshAll.run();
            }
        });
        var moveDown = iconButton("angle-down.svg", "Move condition down", () -> {
            if (conditionIndex < conditions(groupIndex).size() - 1) {
                viewModel.moveCondition(groupIndex, conditionIndex, conditionIndex + 1);
                pendingSelection.accept("identification.group." + groupIndex + ".condition." + (conditionIndex + 1));
                refreshAll.run();
            }
        });
        var remove = iconButton("eraser.svg", "Remove condition", () -> {
            viewModel.removeCondition(groupIndex, conditionIndex);
            pendingSelection.accept("identification.group." + groupIndex);
            refreshAll.run();
        });
        moveUp.setDisable(conditionIndex <= 0);
        moveDown.setDisable(conditionIndex >= conditions(groupIndex).size() - 1);
        section.getChildren().add(new HBox(8, addCondition, moveUp, moveDown, remove));
    }

    private Button button(String text, Runnable action) {
        var button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button iconButton(String iconName, String tooltip, Runnable action) {
        var button = button("", action);
        button.setGraphic(svgIcon(iconName));
        button.setTooltip(new Tooltip(tooltip));
        button.setMinSize(36, 32);
        button.setPrefSize(36, 32);
        button.setMaxSize(36, 32);
        return button;
    }

    private SVGPath svgIcon(String iconName) {
        var path = new SVGPath();
        path.setContent(svgPathContent(iconName));
        path.setFill(Color.web("#2f3742"));
        path.setScaleX(0.032);
        path.setScaleY(0.032);
        return path;
    }

    private String svgPathContent(String iconName) {
        var resource = "/icons/" + iconName;
        try (var input = IdentificationPropertiesPanel.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing icon resource: " + resource);
            }
            var svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = Pattern.compile("<path\\s+[^>]*d=\"([^\"]+)\"").matcher(svg);
            if (!matcher.find()) {
                throw new IllegalStateException("Icon has no path data: " + resource);
            }
            return matcher.group(1);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot read icon resource: " + resource, ex);
        }
    }

    private HBox extensionInput(TextField field, Button picker) {
        detachFromParent(field);
        detachFromParent(picker);
        field.setMaxWidth(Double.MAX_VALUE);
        picker.setMinWidth(34);
        picker.setMaxWidth(34);
        var box = new HBox(6, field, picker);
        HBox.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private void addExtensionRow(VBox form, String labelText, Node input, ExtensionRefDto ref, ExtensionType type, Consumer<ExtensionRefDto> onChange) {
        detachFromParent(input);
        var label = new Label(labelText);
        label.setStyle("-fx-text-fill: #111827;");
        var field = new VBox(3, label, input);
        field.setMaxWidth(Double.MAX_VALUE);
        if (parametersForm.hasParameters(ref, type)) {
            var parameters = parametersForm.inlineView(ref, type, onChange);
            var indented = new HBox(8, parameterGuideLine(), parameters);
            indented.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(parameters, Priority.ALWAYS);
            field.getChildren().add(indented);
        }
        form.getChildren().add(field);
    }

    private Node parameterGuideLine() {
        var line = new javafx.scene.layout.Region();
        line.setMinWidth(2);
        line.setPrefWidth(2);
        line.setMaxWidth(2);
        line.setStyle("-fx-background-color: #94a3b8;");
        return line;
    }

    private void chooseExtension(ExtensionType type, TextField target) {
        extensionPicker.chooseId(type, target.getText()).ifPresent(id -> {
            target.setText(id);
            applySelectedCondition();
        });
    }

    private void replaceConditionMatcher(int groupIndex, int conditionIndex, ExtensionRefDto matcher) {
        var current = condition(groupIndex, conditionIndex);
        if (current != null) {
            viewModel.replaceCondition(groupIndex, conditionIndex, new ConditionDto(current.type(), current.page(),
                current.expectedText(), matcher, current.detector(), current.searchRegion()));
            afterChange.run();
        }
    }

    private void replaceConditionDetector(int groupIndex, int conditionIndex, ExtensionRefDto detector) {
        var current = condition(groupIndex, conditionIndex);
        if (current != null) {
            viewModel.replaceCondition(groupIndex, conditionIndex, new ConditionDto(current.type(), current.page(),
                current.expectedText(), current.matcher(), detector, current.searchRegion()));
            afterChange.run();
        }
    }

    private void applySelectedCondition() {
        var selected = selection.get();
        if (refreshing || viewModel.draft() == null || selected.type() != SelectionType.CONDITION) {
            return;
        }
        var type = nullToDefault(conditionType.getValue(), "TEXT");
        var textCondition = isTextCondition(type);
        var detectorId = detectorIdForType(type);
        var detector = textCondition
            ? null
            : extensionRef(detectorId == null ? conditionDetectorId.getText() : detectorId, selectedCondition() == null ? null : selectedCondition().detector());
        viewModel.replaceCondition(selected.groupIndex(), selected.conditionIndex(), new ConditionDto(
            type, parseInteger(conditionPage.getText()),
            blankToNull(conditionExpectedText.getText()),
            extensionRef(conditionMatcherId.getText(), selectedCondition() == null ? null : selectedCondition().matcher()),
            detector,
            conditionSearchRegion()));
        afterChange.run();
    }

    private boolean isTextCondition(String type) {
        return "TEXT".equals(type) || "TEXT_FUZZY".equals(type);
    }

    private boolean hasSingleDetectorOption(String type) {
        return detectorIdForType(type) != null;
    }

    private String detectorIdForType(String type) {
        if ("QR".equals(type) && extensionRegistry.find(new ExtensionId("qr")).isPresent()) {
            return "qr";
        }
        if ("BARCODE".equals(type) && extensionRegistry.find(new ExtensionId("barcode")).isPresent()) {
            return "barcode";
        }
        return null;
    }

    private boolean hasCompleteConditionSearchRegion() {
        return blankToNull(searchRegionX.getEditor().getText()) != null
            && blankToNull(searchRegionY.getEditor().getText()) != null
            && blankToNull(searchRegionWidth.getEditor().getText()) != null
            && blankToNull(searchRegionHeight.getEditor().getText()) != null;
    }

    private List<ConditionGroupDto> groups() {
        return viewModel.draft() == null || viewModel.draft().identification() == null || viewModel.draft().identification().groups() == null
            ? List.of()
            : viewModel.draft().identification().groups();
    }

    private List<ConditionDto> conditions(int groupIndex) {
        var groups = groups();
        if (groupIndex < 0 || groupIndex >= groups.size() || groups.get(groupIndex).conditions() == null) {
            return List.of();
        }
        return groups.get(groupIndex).conditions();
    }

    private ConditionDto selectedCondition() {
        var selected = selection.get();
        return selected.type() == SelectionType.CONDITION ? condition(selected.groupIndex(), selected.conditionIndex()) : null;
    }

    private ConditionDto condition(int groupIndex, int conditionIndex) {
        var conditions = conditions(groupIndex);
        return conditionIndex < 0 || conditionIndex >= conditions.size() ? null : conditions.get(conditionIndex);
    }

    private ExtensionRefDto extensionRef(String id, ExtensionRefDto current) {
        var normalized = blankToNull(id);
        if (normalized == null) {
            return null;
        }
        var parameters = current != null && normalized.equals(current.id()) && current.parameters() != null ? current.parameters() : Map.<String, Object>of();
        return new ExtensionRefDto(normalized, parameters);
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        return text == null ? null : Integer.parseInt(text);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToDefault(String value, String defaultValue) {
        var normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatRegionNumber(double value) {
        return String.valueOf(Math.round(value));
    }

    public enum SelectionType {
        ROOT,
        GROUP,
        CONDITION
    }

    private enum RegionPart {
        X,
        Y,
        WIDTH,
        HEIGHT
    }

    public record Selection(SelectionType type, int groupIndex, int conditionIndex) {
    }
}
