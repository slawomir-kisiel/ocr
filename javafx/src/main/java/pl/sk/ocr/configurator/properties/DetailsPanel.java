package pl.sk.ocr.configurator.properties;

import javafx.scene.Node;

public interface DetailsPanel {
    Node view();

    void refresh();

    void commit();
}
