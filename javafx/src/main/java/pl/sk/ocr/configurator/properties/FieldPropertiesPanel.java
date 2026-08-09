package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.FieldDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;

public final class FieldPropertiesPanel implements DetailsPanel {
    private final CategoryEditorViewModel viewModel;
    private final Supplier<List<FieldDto>> fields;
    private final IntSupplier selectedFieldIndex;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final Runnable activateRegionDrawing;
    private final Supplier<Node> drawIcon;
    private final Label fieldsCount = new Label();
    private final TextField fieldRegionX = new TextField();
    private final TextField fieldRegionY = new TextField();
    private final TextField fieldRegionWidth = new TextField();
    private final TextField fieldRegionHeight = new TextField();
    private final Button drawFieldRegion = new Button();
    private boolean refreshing;

    public FieldPropertiesPanel(CategoryEditorViewModel viewModel, Supplier<List<FieldDto>> fields,
                                IntSupplier selectedFieldIndex, Label detailsInfo, Runnable afterChange,
                                Runnable activateRegionDrawing, Supplier<Node> drawIcon) {
        this.viewModel = viewModel;
        this.fields = fields;
        this.selectedFieldIndex = selectedFieldIndex;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        this.activateRegionDrawing = activateRegionDrawing;
        this.drawIcon = drawIcon;
        configure();
    }

    @Override
    public Node view() {
        var section = section("Fields");
        if (selectedField() != null) {
            addFormRow(section, "Region X", fieldRegionX);
            addFormRow(section, "Region Y", fieldRegionY);
            addFormRow(section, "Region Width", fieldRegionWidth);
            addFormRow(section, "Region Height", fieldRegionHeight);
            detachFromParent(drawFieldRegion);
            section.getChildren().add(drawFieldRegion);
        } else {
            addFormRow(section, "Fields", fieldsCount);
        }
        return new VBox(10, section, detailsInfo);
    }

    @Override
    public void refresh() {
        refreshing = true;
        try {
            fieldsCount.setText(String.valueOf(fields.get().size()));
            var field = selectedField();
            var region = field == null ? null : field.region();
            fieldRegionX.setText(region == null ? "" : formatRegionNumber(region.x()));
            fieldRegionY.setText(region == null ? "" : formatRegionNumber(region.y()));
            fieldRegionWidth.setText(region == null ? "" : formatRegionNumber(region.width()));
            fieldRegionHeight.setText(region == null ? "" : formatRegionNumber(region.height()));
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void commit() {
        applySelectedFieldRegion();
    }

    public void replaceFieldRegion(int index, RegionDto region) {
        viewModel.updateFieldRegion(index, region);
    }

    public void updateFieldRegionFromViewer(RegionDto region, boolean commit) {
        setRegionText(fieldRegionX, formatRegionNumber(region.x()));
        setRegionText(fieldRegionY, formatRegionNumber(region.y()));
        setRegionText(fieldRegionWidth, formatRegionNumber(region.width()));
        setRegionText(fieldRegionHeight, formatRegionNumber(region.height()));
        if (commit) {
            applySelectedFieldRegion();
        }
    }

    private void configure() {
        fieldsCount.setStyle("-fx-text-fill: #111827;");
        installTooltip(fieldsCount, "Number of fields configured for extraction.");
        installTooltip(fieldRegionX, "Field region X coordinate in image/reference coordinates.");
        installTooltip(fieldRegionY, "Field region Y coordinate in image/reference coordinates.");
        installTooltip(fieldRegionWidth, "Field region width in image/reference coordinates.");
        installTooltip(fieldRegionHeight, "Field region height in image/reference coordinates.");
        drawFieldRegion.setGraphic(drawIcon.get());
        drawFieldRegion.setTooltip(new Tooltip("Draw field region on document preview."));
        drawFieldRegion.setMinSize(36, 32);
        drawFieldRegion.setPrefSize(36, 32);
        drawFieldRegion.setMaxSize(36, 32);

        addDraftListener(fieldRegionX, this::applySelectedFieldRegion);
        addDraftListener(fieldRegionY, this::applySelectedFieldRegion);
        addDraftListener(fieldRegionWidth, this::applySelectedFieldRegion);
        addDraftListener(fieldRegionHeight, this::applySelectedFieldRegion);
        drawFieldRegion.setOnAction(event -> activateRegionDrawing.run());
    }

    private FieldDto selectedField() {
        var index = selectedFieldIndex.getAsInt();
        var fieldList = fields.get();
        if (index < 0 || index >= fieldList.size()) {
            return null;
        }
        return fieldList.get(index);
    }

    private void applySelectedFieldRegion() {
        if (refreshing || viewModel.draft() == null || selectedField() == null) {
            return;
        }
        viewModel.updateFieldRegion(selectedFieldIndex.getAsInt(), new RegionDto(
            parseDouble(fieldRegionX.getText()),
            parseDouble(fieldRegionY.getText()),
            parseDouble(fieldRegionWidth.getText()),
            parseDouble(fieldRegionHeight.getText())
        ));
        afterChange.run();
    }

    private double parseDouble(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return 0.0;
        }
        return Double.parseDouble(text);
    }

    private String formatRegionNumber(double value) {
        return String.valueOf(Math.round(value));
    }

    private void setRegionText(TextField textField, String value) {
        refreshing = true;
        try {
            textField.setText(value);
        } finally {
            refreshing = false;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
