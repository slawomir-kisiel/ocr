package pl.sk.ocr.configurator;

import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import pl.sk.ocr.configurator.properties.CategoryPropertiesPanel;
import pl.sk.ocr.configurator.properties.DetailsPanel;

final class PropertiesPanel {
    private final VBox container;
    private final CategoryPropertiesPanel categoryPanel;
    private final DetailsPanel identificationPanel;
    private final DetailsPanel anchorPanel;
    private final DetailsPanel geometryPanel;
    private final DetailsPanel fieldPanel;
    private final Supplier<ConfiguratorApplication.TreeNodeType> selectedNodeType;
    private final Supplier<Node> emptyPanel;

    PropertiesPanel(VBox container, CategoryPropertiesPanel categoryPanel, DetailsPanel identificationPanel,
                    DetailsPanel anchorPanel, DetailsPanel geometryPanel, DetailsPanel fieldPanel,
                    Supplier<ConfiguratorApplication.TreeNodeType> selectedNodeType, Supplier<Node> emptyPanel) {
        this.container = container;
        this.categoryPanel = categoryPanel;
        this.identificationPanel = identificationPanel;
        this.anchorPanel = anchorPanel;
        this.geometryPanel = geometryPanel;
        this.fieldPanel = fieldPanel;
        this.selectedNodeType = selectedNodeType;
        this.emptyPanel = emptyPanel;
    }

    void showEmpty() {
        categoryPanel.clear();
        container.setDisable(true);
        container.getChildren().setAll(emptyPanel.get());
    }

    void refreshActive() {
        container.setDisable(false);
        categoryPanel.refresh();
        identificationPanel.refresh();
        anchorPanel.refresh();
        fieldPanel.refresh();
        geometryPanel.refresh();
        container.getChildren().setAll(activePanel().view());
    }

    void commitActive() {
        activePanel().commit();
    }

    private DetailsPanel activePanel() {
        return switch (selectedNodeType.get()) {
            case IDENTIFICATION, IDENTIFICATION_GROUP, CONDITION -> identificationPanel;
            case ANCHORS, ANCHOR -> anchorPanel;
            case GEOMETRY, GEOMETRY_STRATEGY -> geometryPanel;
            case FIELDS, FIELD, FIELD_OCR, FIELD_OUTPUT, FIELD_IMAGE_PROCESSORS, FIELD_TRANSFORMERS, FIELD_VALIDATORS, PIPELINE_STEP -> fieldPanel;
            case ROOT -> categoryPanel;
        };
    }
}
