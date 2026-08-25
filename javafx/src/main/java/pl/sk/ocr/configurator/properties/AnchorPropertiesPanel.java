package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.AnchorDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.ReferenceFeatureDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

public final class AnchorPropertiesPanel implements DetailsPanel {
    private final CategoryEditorViewModel viewModel;
    private final Supplier<List<AnchorDto>> anchors;
    private final IntSupplier selectedIndex;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final Runnable refreshAll;
    private final Consumer<String> pendingSelection;
    private final Runnable activateSearchRegionDrawing;
    private final Runnable activateReferenceBoundsDrawing;
    private final Function<String, Node> iconFactory;
    private final ExtensionRegistry extensionRegistry;
    private final ExtensionPicker extensionPicker;
    private final ExtensionParametersForm parametersForm;
    private final Label anchorsCount = new Label();
    private final Button addAnchor = new Button("Add Anchor");
    private final TextField anchorId = new TextField();
    private final TextField anchorPage = new TextField();
    private final ComboBox<String> anchorDetectorId = new ComboBox<>();
    private final TextField anchorExpectedText = new TextField();
    private final TextField anchorMatcherId = new TextField();
    private final Button pickMatcher = new Button("...");
    private final CheckBox anchorRequired = new CheckBox();
    private final Spinner<Integer> searchRegionX = regionSpinner();
    private final Spinner<Integer> searchRegionY = regionSpinner();
    private final Spinner<Integer> searchRegionWidth = regionSpinner();
    private final Spinner<Integer> searchRegionHeight = regionSpinner();
    private final Spinner<Integer> referenceBoundsX = regionSpinner();
    private final Spinner<Integer> referenceBoundsY = regionSpinner();
    private final Spinner<Integer> referenceBoundsWidth = regionSpinner();
    private final Spinner<Integer> referenceBoundsHeight = regionSpinner();
    private final Button drawSearchRegion = new Button();
    private final Button clearSearchRegion = new Button();
    private final Button drawReferenceBounds = new Button();
    private final Button symmetricSearchRegionResize = new Button();
    private final Button symmetricReferenceBoundsResize = new Button();
    private boolean refreshing;
    private boolean adjustingRegionSpinners;
    private boolean symmetricSearchRegionResizeEnabled;
    private boolean symmetricReferenceBoundsResizeEnabled;

    public AnchorPropertiesPanel(CategoryEditorViewModel viewModel, Supplier<List<AnchorDto>> anchors,
                                 IntSupplier selectedIndex, Label detailsInfo, Runnable afterChange,
                                 Runnable refreshAll, Consumer<String> pendingSelection,
                                 Runnable activateSearchRegionDrawing, Runnable activateReferenceBoundsDrawing,
                                 Function<String, Node> iconFactory, ExtensionRegistry extensionRegistry) {
        this.viewModel = viewModel;
        this.anchors = anchors;
        this.selectedIndex = selectedIndex;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        this.refreshAll = refreshAll;
        this.pendingSelection = pendingSelection;
        this.activateSearchRegionDrawing = activateSearchRegionDrawing;
        this.activateReferenceBoundsDrawing = activateReferenceBoundsDrawing;
        this.iconFactory = iconFactory;
        this.extensionRegistry = extensionRegistry;
        this.extensionPicker = new ExtensionPicker(extensionRegistry);
        this.parametersForm = new ExtensionParametersForm(extensionRegistry);
        configure();
    }

    @Override
    public Node view() {
        var section = section("Anchors");
        var index = selectedIndex.getAsInt();
        if (index >= 0) {
            anchorControls(section, index);
        } else {
            addFormRow(section, "Anchors", anchorsCount);
            detachFromParent(addAnchor);
            section.getChildren().add(addAnchor);
        }
        return new VBox(10, section, detailsInfo);
    }

    @Override
    public void refresh() {
        refreshing = true;
        try {
            anchorsCount.setText(String.valueOf(anchors.get().size()));
            var anchor = selectedAnchor();
            anchorId.setText(anchor == null ? "" : nullToEmpty(anchor.id()));
            anchorPage.setText(anchor == null || anchor.page() == null ? "" : anchor.page().toString());
            anchorDetectorId.setValue(anchor == null || anchor.detector() == null ? defaultDetectorId() : nullToEmpty(anchor.detector().id()));
            anchorExpectedText.setText(anchor == null ? "" : nullToEmpty(anchor.expectedText()));
            anchorMatcherId.setText(anchor == null || anchor.matcher() == null ? "" : nullToEmpty(anchor.matcher().id()));
            anchorRequired.setSelected(anchor != null && Boolean.TRUE.equals(anchor.required()));
            var searchRegion = anchor == null ? null : anchor.searchRegion();
            setRegionSpinnerText(searchRegionX, searchRegion == null ? "" : formatRegionNumber(searchRegion.x()));
            setRegionSpinnerText(searchRegionY, searchRegion == null ? "" : formatRegionNumber(searchRegion.y()));
            setRegionSpinnerText(searchRegionWidth, searchRegion == null ? "" : formatRegionNumber(searchRegion.width()));
            setRegionSpinnerText(searchRegionHeight, searchRegion == null ? "" : formatRegionNumber(searchRegion.height()));
            var referenceBounds = anchor == null || anchor.referenceFeature() == null ? null : anchor.referenceFeature().bounds();
            setRegionSpinnerText(referenceBoundsX, referenceBounds == null ? "" : formatRegionNumber(referenceBounds.x()));
            setRegionSpinnerText(referenceBoundsY, referenceBounds == null ? "" : formatRegionNumber(referenceBounds.y()));
            setRegionSpinnerText(referenceBoundsWidth, referenceBounds == null ? "" : formatRegionNumber(referenceBounds.width()));
            setRegionSpinnerText(referenceBoundsHeight, referenceBounds == null ? "" : formatRegionNumber(referenceBounds.height()));
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void commit() {
        applySelectedAnchor();
    }

    public void updateSearchRegionFromViewer(RegionDto region, boolean commit) {
        updateRegionControls(region, searchRegionX, searchRegionY, searchRegionWidth, searchRegionHeight);
        if (commit) {
            applySelectedAnchor();
        }
    }

    public void updateReferenceBoundsFromViewer(RegionDto region, boolean commit) {
        updateRegionControls(region, referenceBoundsX, referenceBoundsY, referenceBoundsWidth, referenceBoundsHeight);
        if (commit) {
            applySelectedAnchor();
        }
    }

    public void replaceSearchRegion(int index, RegionDto region) {
        var current = anchor(index);
        if (current != null) {
            viewModel.replaceAnchor(index, new AnchorDto(current.id(), current.page(), current.detector(),
                current.expectedText(), current.matcher(), current.required(), current.referenceFeature(), region));
        }
    }

    public void replaceReferenceBounds(int index, RegionDto region) {
        var current = anchor(index);
        if (current != null) {
            viewModel.replaceAnchor(index, new AnchorDto(current.id(), current.page(), current.detector(),
                current.expectedText(), current.matcher(), current.required(), new ReferenceFeatureDto(region), current.searchRegion()));
        }
    }

    private void configure() {
        installTooltip(anchorsCount, "Number of anchors configured for geometry detection.");
        installTooltip(addAnchor, "Add a new anchor.");
        installTooltip(anchorId, "Anchor id used by geometry strategy references.");
        installTooltip(anchorPage, "Page where this anchor should be detected.");
        installTooltip(anchorDetectorId, "Detector extension id used by this anchor.");
        installTooltip(anchorExpectedText, "Text expected in detector output.");
        installTooltip(anchorMatcherId, "Matcher extension id used by this anchor.");
        installTooltip(pickMatcher, "Choose matcher extension from registry.");
        installTooltip(anchorRequired, "Whether missing anchor should fail geometry detection.");
        installTooltip(searchRegionX, "Anchor search region X coordinate.");
        installTooltip(searchRegionY, "Anchor search region Y coordinate.");
        installTooltip(searchRegionWidth, "Anchor search region width.");
        installTooltip(searchRegionHeight, "Anchor search region height.");
        installTooltip(referenceBoundsX, "Reference feature bounds X coordinate.");
        installTooltip(referenceBoundsY, "Reference feature bounds Y coordinate.");
        installTooltip(referenceBoundsWidth, "Reference feature bounds width.");
        installTooltip(referenceBoundsHeight, "Reference feature bounds height.");
        configureDrawButton(drawSearchRegion, "Draw anchor search region on document preview.");
        configureIconButton(clearSearchRegion, "eraser.svg", "Clear anchor search region. Empty values mean searching the whole page.");
        configureDrawButton(drawReferenceBounds, "Draw anchor reference feature bounds on document preview.");
        configureSymmetricResizeButton(symmetricSearchRegionResize);
        configureSymmetricResizeButton(symmetricReferenceBoundsResize);
        anchorDetectorId.getItems().setAll(detectorIds());
        anchorDetectorId.setEditable(false);
        addAnchor.setOnAction(event -> addAnchor());
        addDraftListener(anchorId, this::applySelectedAnchor);
        addDraftListener(anchorPage, this::applySelectedAnchor);
        addDraftListener(anchorExpectedText, this::applySelectedAnchor);
        addDraftListener(anchorMatcherId, this::applySelectedAnchor);
        anchorDetectorId.valueProperty().addListener((obs, old, value) -> applySelectedAnchor(!java.util.Objects.equals(old, value)));
        pickMatcher.setOnAction(event -> chooseMatcher());
        anchorRequired.selectedProperty().addListener((obs, old, value) -> applySelectedAnchor());
        addRegionSpinnerListeners(searchRegionX, searchRegionY, searchRegionWidth, searchRegionHeight,
            () -> symmetricSearchRegionResizeEnabled);
        addRegionSpinnerListeners(referenceBoundsX, referenceBoundsY, referenceBoundsWidth, referenceBoundsHeight,
            () -> symmetricReferenceBoundsResizeEnabled);
        drawSearchRegion.setOnAction(event -> activateSearchRegionDrawing.run());
        clearSearchRegion.setOnAction(event -> clearSearchRegion());
        drawReferenceBounds.setOnAction(event -> activateReferenceBoundsDrawing.run());
        symmetricSearchRegionResize.setOnAction(event -> {
            symmetricSearchRegionResizeEnabled = !symmetricSearchRegionResizeEnabled;
            updateSymmetricResizeButton(symmetricSearchRegionResize, symmetricSearchRegionResizeEnabled);
        });
        symmetricReferenceBoundsResize.setOnAction(event -> {
            symmetricReferenceBoundsResizeEnabled = !symmetricReferenceBoundsResizeEnabled;
            updateSymmetricResizeButton(symmetricReferenceBoundsResize, symmetricReferenceBoundsResizeEnabled);
        });
    }

    private void configureDrawButton(Button button, String tooltip) {
        configureIconButton(button, "mode-draw-region.svg", tooltip);
    }

    private void configureIconButton(Button button, String icon, String tooltip) {
        button.setGraphic(iconFactory.apply(icon));
        button.setTooltip(new Tooltip(tooltip));
        button.setMinSize(36, 32);
        button.setPrefSize(36, 32);
        button.setMaxSize(36, 32);
    }

    private void configureSymmetricResizeButton(Button button) {
        updateSymmetricResizeButton(button, false);
        button.setMinSize(36, 32);
        button.setPrefSize(36, 32);
        button.setMaxSize(36, 32);
    }

    private void updateSymmetricResizeButton(Button button, boolean enabled) {
        button.setGraphic(iconFactory.apply(enabled ? "lock.svg" : "lock-open.svg"));
        button.setTooltip(new Tooltip(enabled ? "Disable symmetric region resize." : "Enable symmetric region resize."));
    }

    private void anchorControls(VBox section, int anchorIndex) {
        var anchorContent = new VBox(8);
        addFormRow(anchorContent, "ID", anchorId);
        addFormRow(anchorContent, "Page", anchorPage);
        var anchor = anchor(anchorIndex);
        addExtensionRow(anchorContent, "Detector", anchorDetectorId,
            anchor == null ? null : anchor.detector(), ExtensionType.DETECTOR, ref -> replaceDetector(anchorIndex, ref));
        addFormRow(anchorContent, "Expected Text", anchorExpectedText);
        addExtensionRow(anchorContent, "Matcher", extensionInput(anchorMatcherId, pickMatcher),
            anchor == null ? null : anchor.matcher(), ExtensionType.MATCHER, ref -> replaceMatcher(anchorIndex, ref));
        addFormRow(anchorContent, "Required", anchorRequired);
        section.getChildren().add(titledPane("Anchor", anchorContent));

        var searchRegionContent = new VBox(8);
        detachFromParent(drawSearchRegion);
        detachFromParent(clearSearchRegion);
        detachFromParent(symmetricSearchRegionResize);
        searchRegionContent.getChildren().add(regionRowsWithActions(searchRegionX, searchRegionY, searchRegionWidth, searchRegionHeight,
            new VBox(6, drawSearchRegion, symmetricSearchRegionResize, clearSearchRegion)));
        section.getChildren().add(titledPane("Search Region", searchRegionContent));

        var referenceContent = new VBox(8);
        detachFromParent(drawReferenceBounds);
        detachFromParent(symmetricReferenceBoundsResize);
        referenceContent.getChildren().add(regionRowsWithActions(referenceBoundsX, referenceBoundsY, referenceBoundsWidth, referenceBoundsHeight,
            new VBox(6, drawReferenceBounds, symmetricReferenceBoundsResize)));
        section.getChildren().add(titledPane("Reference Feature", referenceContent));

        var add = button("Add Anchor", this::addAnchor);
        var moveUp = button("Move Up", () -> {
            if (anchorIndex > 0) {
                viewModel.moveAnchor(anchorIndex, anchorIndex - 1);
                pendingSelection.accept("anchor." + (anchorIndex - 1));
                refreshAll.run();
            }
        });
        var moveDown = button("Move Down", () -> {
            if (anchorIndex < anchors.get().size() - 1) {
                viewModel.moveAnchor(anchorIndex, anchorIndex + 1);
                pendingSelection.accept("anchor." + (anchorIndex + 1));
                refreshAll.run();
            }
        });
        var remove = button("Remove Anchor", () -> {
            viewModel.removeAnchor(anchorIndex);
            pendingSelection.accept("anchors");
            refreshAll.run();
        });
        moveUp.setDisable(anchorIndex <= 0);
        moveDown.setDisable(anchorIndex >= anchors.get().size() - 1);
        section.getChildren().add(new HBox(8, add, moveUp, moveDown, remove));
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

    private void addRegionSpinnerListeners(Spinner<Integer> x, Spinner<Integer> y, Spinner<Integer> width,
                                           Spinner<Integer> height, BooleanSupplier symmetricResizeEnabled) {
        x.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.X, old, value, x, y, width, height,
            symmetricResizeEnabled));
        y.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.Y, old, value, x, y, width, height,
            symmetricResizeEnabled));
        width.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.WIDTH, old, value, x, y, width, height,
            symmetricResizeEnabled));
        height.valueProperty().addListener((obs, old, value) -> applyRegionSpinnerChange(RegionPart.HEIGHT, old, value, x, y, width, height,
            symmetricResizeEnabled));
        x.getEditor().textProperty().addListener((obs, old, value) -> applySelectedAnchorAfterRegionTextChange());
        y.getEditor().textProperty().addListener((obs, old, value) -> applySelectedAnchorAfterRegionTextChange());
        width.getEditor().textProperty().addListener((obs, old, value) -> applySelectedAnchorAfterRegionTextChange());
        height.getEditor().textProperty().addListener((obs, old, value) -> applySelectedAnchorAfterRegionTextChange());
    }

    private void applySelectedAnchorAfterRegionTextChange() {
        if (!adjustingRegionSpinners) {
            applySelectedAnchor();
        }
    }

    private void applyRegionSpinnerChange(RegionPart part, Integer oldValue, Integer newValue,
                                          Spinner<Integer> x, Spinner<Integer> y, Spinner<Integer> width,
                                          Spinner<Integer> height, BooleanSupplier symmetricResizeEnabled) {
        if (refreshing || adjustingRegionSpinners) {
            return;
        }
        if (!symmetricResizeEnabled.getAsBoolean() || oldValue == null || newValue == null || oldValue.equals(newValue)
            || !hasCompleteRegion(x, y, width, height)) {
            applySelectedAnchor();
            return;
        }
        var delta = newValue - oldValue;
        adjustingRegionSpinners = true;
        try {
            switch (part) {
                case X -> setRegionSpinnerValue(width, Math.max(1, width.getValue() - delta * 2));
                case Y -> setRegionSpinnerValue(height, Math.max(1, height.getValue() - delta * 2));
                case WIDTH -> {
                    var widthDelta = normalizeSizeDelta(width, oldValue, newValue);
                    setRegionSpinnerValue(x, x.getValue() - widthDelta / 2);
                }
                case HEIGHT -> {
                    var heightDelta = normalizeSizeDelta(height, oldValue, newValue);
                    setRegionSpinnerValue(y, y.getValue() - heightDelta / 2);
                }
            }
        } finally {
            adjustingRegionSpinners = false;
        }
        applySelectedAnchor();
    }

    private int normalizeSizeDelta(Spinner<Integer> spinner, int oldValue, int newValue) {
        var delta = newValue - oldValue;
        if (delta == 0) {
            return 0;
        }
        var normalized = delta % 2 == 0 ? delta : delta + Integer.signum(delta);
        setRegionSpinnerValue(spinner, Math.max(1, oldValue + normalized));
        return spinner.getValue() - oldValue;
    }

    private void setRegionSpinnerValue(Spinner<Integer> spinner, int value) {
        spinner.getValueFactory().setValue(value);
        spinner.getEditor().setText(String.valueOf(value));
    }

    private boolean hasCompleteRegion(Spinner<Integer> x, Spinner<Integer> y, Spinner<Integer> width, Spinner<Integer> height) {
        return blankToNull(x.getEditor().getText()) != null
            && blankToNull(y.getEditor().getText()) != null
            && blankToNull(width.getEditor().getText()) != null
            && blankToNull(height.getEditor().getText()) != null;
    }

    private void chooseMatcher() {
        extensionPicker.chooseId(ExtensionType.MATCHER, anchorMatcherId.getText()).ifPresent(id -> {
            anchorMatcherId.setText(id);
            applySelectedAnchor();
        });
    }

    private void replaceDetector(int anchorIndex, ExtensionRefDto detector) {
        var current = anchor(anchorIndex);
        if (current != null) {
            viewModel.replaceAnchor(anchorIndex, new AnchorDto(current.id(), current.page(), detector,
                current.expectedText(), current.matcher(), current.required(), current.referenceFeature(), current.searchRegion()));
            afterChange.run();
        }
    }

    private void replaceMatcher(int anchorIndex, ExtensionRefDto matcher) {
        var current = anchor(anchorIndex);
        if (current != null) {
            viewModel.replaceAnchor(anchorIndex, new AnchorDto(current.id(), current.page(), current.detector(),
                current.expectedText(), matcher, current.required(), current.referenceFeature(), current.searchRegion()));
            afterChange.run();
        }
    }

    private void addAnchor() {
        if (viewModel.draft() == null) {
            return;
        }
        var index = anchors.get().size();
        viewModel.addAnchor(new AnchorDto(uniqueAnchorId(index + 1), viewModel.session().currentPage(),
            extensionRef("text"), "", null, true, null, null));
        pendingSelection.accept("anchor." + index);
        refreshAll.run();
    }

    private void applySelectedAnchor() {
        applySelectedAnchor(false);
    }

    private void applySelectedAnchor(boolean refreshParameters) {
        if (refreshing || viewModel.draft() == null || selectedIndex.getAsInt() < 0) {
            return;
        }
        var index = selectedIndex.getAsInt();
        var current = selectedAnchor();
        viewModel.replaceAnchor(index, new AnchorDto(blankToNull(anchorId.getText()), parseInteger(anchorPage.getText()),
            extensionRef(anchorDetectorId.getValue(), current == null ? null : current.detector()), blankToNull(anchorExpectedText.getText()),
            extensionRef(anchorMatcherId.getText(), current == null ? null : current.matcher()), anchorRequired.isSelected(), referenceFeature(), searchRegion()));
        pendingSelection.accept("anchor." + index);
        if (refreshParameters) {
            refreshAll.run();
        } else {
            afterChange.run();
        }
    }

    private AnchorDto selectedAnchor() {
        return anchor(selectedIndex.getAsInt());
    }

    private AnchorDto anchor(int index) {
        var list = anchors.get();
        return index < 0 || index >= list.size() ? null : list.get(index);
    }

    private void updateRegionControls(RegionDto region, Spinner<Integer> x, Spinner<Integer> y,
                                      Spinner<Integer> width, Spinner<Integer> height) {
        refreshing = true;
        try {
            setRegionSpinnerText(x, formatRegionNumber(region.x()));
            setRegionSpinnerText(y, formatRegionNumber(region.y()));
            setRegionSpinnerText(width, formatRegionNumber(region.width()));
            setRegionSpinnerText(height, formatRegionNumber(region.height()));
        } finally {
            refreshing = false;
        }
    }

    private void clearSearchRegion() {
        refreshing = true;
        try {
            setRegionSpinnerText(searchRegionX, "");
            setRegionSpinnerText(searchRegionY, "");
            setRegionSpinnerText(searchRegionWidth, "");
            setRegionSpinnerText(searchRegionHeight, "");
        } finally {
            refreshing = false;
        }
        applySelectedAnchor();
    }

    private RegionDto searchRegion() {
        return spinnerRegion(searchRegionX, searchRegionY, searchRegionWidth, searchRegionHeight);
    }

    private ReferenceFeatureDto referenceFeature() {
        var bounds = spinnerRegion(referenceBoundsX, referenceBoundsY, referenceBoundsWidth, referenceBoundsHeight);
        return bounds == null ? null : new ReferenceFeatureDto(bounds);
    }

    private RegionDto spinnerRegion(Spinner<Integer> x, Spinner<Integer> y, Spinner<Integer> width, Spinner<Integer> height) {
        if (blankToNull(x.getEditor().getText()) == null || blankToNull(y.getEditor().getText()) == null
            || blankToNull(width.getEditor().getText()) == null || blankToNull(height.getEditor().getText()) == null) {
            return null;
        }
        return new RegionDto(parseInteger(x.getEditor().getText()), parseInteger(y.getEditor().getText()),
            parseInteger(width.getEditor().getText()), parseInteger(height.getEditor().getText()));
    }

    private ExtensionRefDto extensionRef(String id) {
        return extensionRef(id, null);
    }

    private ExtensionRefDto extensionRef(String id, ExtensionRefDto current) {
        var normalized = blankToNull(id);
        if (normalized == null) {
            return null;
        }
        var parameters = current != null && normalized.equals(current.id()) && current.parameters() != null ? current.parameters() : Map.<String, Object>of();
        return new ExtensionRefDto(normalized, parameters);
    }

    private List<String> detectorIds() {
        return extensionRegistry.extensions().stream()
            .filter(extension -> extension.descriptor().type() == ExtensionType.DETECTOR)
            .map(extension -> extension.descriptor().id().value())
            .sorted()
            .toList();
    }

    private String defaultDetectorId() {
        var ids = detectorIds();
        if (ids.contains("text")) {
            return "text";
        }
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private String uniqueAnchorId(int seed) {
        var existing = anchors.get().stream().map(AnchorDto::id).collect(java.util.stream.Collectors.toSet());
        var candidate = "anchor-" + seed;
        var suffix = seed;
        while (existing.contains(candidate)) {
            suffix++;
            candidate = "anchor-" + suffix;
        }
        return candidate;
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        return text == null ? null : Integer.parseInt(text);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatRegionNumber(double value) {
        return String.valueOf(Math.round(value));
    }

    private enum RegionPart {
        X,
        Y,
        WIDTH,
        HEIGHT
    }
}
