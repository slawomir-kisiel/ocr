package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.ConditionDto;
import pl.sk.ocr.config.dto.ConditionGroupDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
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
    private boolean refreshing;

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
        installTooltip(conditionExpectedText, "Text expected by TEXT condition.");
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
        conditionType.valueProperty().addListener((obs, old, value) -> applySelectedCondition());
        addDraftListener(conditionPage, this::applySelectedCondition);
        addDraftListener(conditionExpectedText, this::applySelectedCondition);
        addDraftListener(conditionMatcherId, this::applySelectedCondition);
        addDraftListener(conditionDetectorId, this::applySelectedCondition);
        pickMatcher.setOnAction(event -> chooseExtension(ExtensionType.MATCHER, conditionMatcherId));
        pickDetector.setOnAction(event -> chooseExtension(ExtensionType.DETECTOR, conditionDetectorId));
        addSpinnerListener(searchRegionX, this::applySelectedCondition);
        addSpinnerListener(searchRegionY, this::applySelectedCondition);
        addSpinnerListener(searchRegionWidth, this::applySelectedCondition);
        addSpinnerListener(searchRegionHeight, this::applySelectedCondition);
        drawSearchRegion.setOnAction(event -> activateConditionSearchRegionDrawing.run());
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
        addFormRow(identificationContent, "Expected Text", conditionExpectedText);
        addFormRow(identificationContent, "Matcher ID", extensionInput(conditionMatcherId, pickMatcher));
        addFormRow(identificationContent, "Detector ID", extensionInput(conditionDetectorId, pickDetector));
        section.getChildren().add(titledPane("Identification", identificationContent));

        var condition = condition(groupIndex, conditionIndex);
        if (condition != null && condition.matcher() != null) {
            section.getChildren().add(parametersForm.view(condition.matcher(), ref -> replaceConditionMatcher(groupIndex, conditionIndex, ref)));
        }
        if (condition != null && condition.detector() != null) {
            section.getChildren().add(parametersForm.view(condition.detector(), ref -> replaceConditionDetector(groupIndex, conditionIndex, ref)));
        }

        var searchRegionContent = new VBox(8);
        addFormRow(searchRegionContent, "X", searchRegionX);
        addFormRow(searchRegionContent, "Y", searchRegionY);
        addFormRow(searchRegionContent, "Width", searchRegionWidth);
        addFormRow(searchRegionContent, "Height", searchRegionHeight);
        section.getChildren().add(titledPane("Search Region", searchRegionContent));

        detachFromParent(drawSearchRegion);
        section.getChildren().add(drawSearchRegion);
        var addCondition = button("Add Condition", () -> {
            var newIndex = conditions(groupIndex).size();
            viewModel.addCondition(groupIndex, new ConditionDto("TEXT", viewModel.session().currentPage(), "", null, null, null));
            pendingSelection.accept("identification.group." + groupIndex + ".condition." + newIndex);
            refreshAll.run();
        });
        var moveUp = button("Move Up", () -> {
            if (conditionIndex > 0) {
                viewModel.moveCondition(groupIndex, conditionIndex, conditionIndex - 1);
                pendingSelection.accept("identification.group." + groupIndex + ".condition." + (conditionIndex - 1));
                refreshAll.run();
            }
        });
        var moveDown = button("Move Down", () -> {
            if (conditionIndex < conditions(groupIndex).size() - 1) {
                viewModel.moveCondition(groupIndex, conditionIndex, conditionIndex + 1);
                pendingSelection.accept("identification.group." + groupIndex + ".condition." + (conditionIndex + 1));
                refreshAll.run();
            }
        });
        var remove = button("Remove Condition", () -> {
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
        viewModel.replaceCondition(selected.groupIndex(), selected.conditionIndex(), new ConditionDto(
            nullToDefault(conditionType.getValue(), "TEXT"), parseInteger(conditionPage.getText()),
            blankToNull(conditionExpectedText.getText()), extensionRef(conditionMatcherId.getText(), selectedCondition() == null ? null : selectedCondition().matcher()),
            extensionRef(conditionDetectorId.getText(), selectedCondition() == null ? null : selectedCondition().detector()), conditionSearchRegion()));
        afterChange.run();
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

    public record Selection(SelectionType type, int groupIndex, int conditionIndex) {
    }
}
