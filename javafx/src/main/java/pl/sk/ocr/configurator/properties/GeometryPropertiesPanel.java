package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.AnchorDto;
import pl.sk.ocr.config.dto.FieldDto;
import pl.sk.ocr.config.dto.GeometryDto;
import pl.sk.ocr.config.dto.GeometryStrategyDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;

public final class GeometryPropertiesPanel implements DetailsPanel {
    private final CategoryEditorViewModel viewModel;
    private final Supplier<List<AnchorDto>> anchors;
    private final Supplier<List<FieldDto>> fields;
    private final Supplier<Image> currentImage;
    private final Label status;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final TextField referenceWidth = new TextField();
    private final TextField referenceHeight = new TextField();
    private final ComboBox<String> strategyType = new ComboBox<>();
    private final TextField strategyAnchors = new TextField();
    private final VBox anchorOptions = new VBox(4);
    private final Button useDocumentDimensions = new Button("Use Document Dimensions");
    private final Label warning = new Label();
    private boolean refreshing;

    public GeometryPropertiesPanel(CategoryEditorViewModel viewModel, Supplier<List<AnchorDto>> anchors,
                                   Supplier<List<FieldDto>> fields, Supplier<Image> currentImage, Label status,
                                   Label detailsInfo, Runnable afterChange) {
        this.viewModel = viewModel;
        this.anchors = anchors;
        this.fields = fields;
        this.currentImage = currentImage;
        this.status = status;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        configure();
    }

    @Override
    public Node view() {
        var section = section("Geometry");
        var dimensions = new VBox(8);
        addFormRow(dimensions, "Reference Width", referenceWidth);
        addFormRow(dimensions, "Reference Height", referenceHeight);
        detachFromParent(useDocumentDimensions);
        dimensions.getChildren().add(useDocumentDimensions);
        updateWarning();
        detachFromParent(warning);
        dimensions.getChildren().add(warning);
        section.getChildren().add(titledPane("Reference Dimensions", dimensions));

        var strategy = new VBox(8);
        addFormRow(strategy, "Strategy Type", strategyType);
        section.getChildren().add(titledPane("Strategy", strategy));

        var anchorSection = new VBox(8);
        rebuildAnchorOptions();
        detachFromParent(anchorOptions);
        anchorSection.getChildren().add(anchorOptions);
        addFormRow(anchorSection, "Anchor IDs", strategyAnchors);
        section.getChildren().add(titledPane("Geometry Anchors", anchorSection));
        return new VBox(10, section, detailsInfo);
    }

    @Override
    public void refresh() {
        refreshing = true;
        try {
            var geometry = viewModel.draft() == null ? null : viewModel.draft().geometry();
            referenceWidth.setText(geometry == null || geometry.referenceWidth() == null ? "" : geometry.referenceWidth().toString());
            referenceHeight.setText(geometry == null || geometry.referenceHeight() == null ? "" : geometry.referenceHeight().toString());
            var strategy = geometry == null ? null : geometry.strategy();
            strategyType.setValue(strategy == null || strategy.type() == null ? "NONE" : strategy.type());
            strategyAnchors.setText(strategy == null || strategy.anchors() == null ? "" : String.join(", ", strategy.anchors()));
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void commit() {
        applyGeometry();
    }

    private void configure() {
        installTooltip(referenceWidth, "Reference document width used by geometry normalization.");
        installTooltip(referenceHeight, "Reference document height used by geometry normalization.");
        installTooltip(strategyType, "Geometry strategy type.");
        installTooltip(strategyAnchors, "Comma-separated anchor IDs used by geometry strategy.");
        installTooltip(useDocumentDimensions, "Copy current document page dimensions to reference width and height.");
        strategyType.getItems().setAll("NONE", "ANCHOR_TRANSLATION", "TWO_POINT_SCALE_TRANSLATE", "AFFINE", "ROBUST_AFFINE");
        strategyType.setEditable(false);
        warning.setWrapText(true);
        warning.setStyle("-fx-text-fill: #92400e;");
        addDraftListener(referenceWidth, this::applyGeometry);
        addDraftListener(referenceHeight, this::applyGeometry);
        strategyType.valueProperty().addListener((obs, old, value) -> applyGeometry());
        addDraftListener(strategyAnchors, this::applyGeometry);
        useDocumentDimensions.setOnAction(event -> useCurrentDocumentDimensions());
    }

    private void rebuildAnchorOptions() {
        anchorOptions.getChildren().clear();
        var selectedIds = new HashSet<>(parseStringList(strategyAnchors.getText()));
        if (anchors.get().isEmpty()) {
            var empty = new Label("No anchors configured.");
            empty.setStyle("-fx-text-fill: #111827;");
            anchorOptions.getChildren().add(empty);
            return;
        }
        for (var anchor : anchors.get()) {
            var id = blankToNull(anchor.id());
            if (id == null) {
                continue;
            }
            var checkBox = new CheckBox(id);
            checkBox.setSelected(selectedIds.contains(id));
            checkBox.setTooltip(new Tooltip("Use anchor '" + id + "' in geometry strategy."));
            checkBox.setStyle("-fx-text-fill: #111827;");
            checkBox.selectedProperty().addListener((obs, old, value) -> updateAnchorsFromChecks());
            anchorOptions.getChildren().add(checkBox);
        }
    }

    private void updateAnchorsFromChecks() {
        if (refreshing) {
            return;
        }
        var selected = anchorOptions.getChildren().stream()
            .filter(CheckBox.class::isInstance)
            .map(CheckBox.class::cast)
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .toList();
        refreshing = true;
        try {
            strategyAnchors.setText(String.join(", ", selected));
        } finally {
            refreshing = false;
        }
        applyGeometry();
    }

    private void useCurrentDocumentDimensions() {
        var image = currentImage.get();
        if (image == null) {
            status.setText("Open a document before copying reference dimensions");
            return;
        }
        refreshing = true;
        try {
            referenceWidth.setText(formatRegionNumber(image.getWidth()));
            referenceHeight.setText(formatRegionNumber(image.getHeight()));
        } finally {
            refreshing = false;
        }
        applyGeometry();
    }

    private void updateWarning() {
        var visible = hasConfiguredRegions();
        warning.setText(visible
            ? "Changing reference dimensions after defining regions may require adjusting anchors and field regions."
            : "");
        setVisibleManaged(warning, visible);
    }

    private boolean hasConfiguredRegions() {
        return anchors.get().stream().anyMatch(anchor -> anchor.searchRegion() != null
            || anchor.referenceFeature() != null && anchor.referenceFeature().bounds() != null)
            || fields.get().stream().anyMatch(field -> field.region() != null);
    }

    private void applyGeometry() {
        if (refreshing || viewModel.draft() == null) {
            return;
        }
        var current = viewModel.draft().geometry();
        var newWidth = parseInteger(referenceWidth.getText());
        var newHeight = parseInteger(referenceHeight.getText());
        var dimensionsChanged = current != null
            && (!Objects.equals(current.referenceWidth(), newWidth)
                || !Objects.equals(current.referenceHeight(), newHeight));
        viewModel.updateGeometry(new GeometryDto(
            newWidth,
            newHeight,
            new GeometryStrategyDto(nullToDefault(strategyType.getValue(), "NONE"), parseStringList(strategyAnchors.getText()))
        ));
        afterChange.run();
        if (dimensionsChanged && hasConfiguredRegions()) {
            status.setText("Reference dimensions changed; existing regions may need adjustment");
        }
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        return text == null ? null : Integer.parseInt(text);
    }

    private List<String> parseStringList(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToDefault(String value, String defaultValue) {
        var normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String formatRegionNumber(double value) {
        return String.valueOf(Math.round(value));
    }
}
