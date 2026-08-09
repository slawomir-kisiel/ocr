package pl.sk.ocr.configurator.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class FormControls {
    private static final String TEXT_COLOR_STYLE = "-fx-text-fill: #111827;";

    private FormControls() {
    }

    public static VBox section(String title) {
        var label = new Label(title);
        label.getStyleClass().add("details-section-title");
        label.setStyle(TEXT_COLOR_STYLE);
        var content = new VBox(8);
        content.setPadding(new Insets(8));
        content.setStyle("-fx-border-color: #c8cdd4; -fx-border-radius: 4; -fx-background-radius: 4;");
        content.getChildren().add(label);
        return content;
    }

    public static TitledPane titledPane(String title, javafx.scene.Node content) {
        var pane = new TitledPane(title, content);
        pane.setExpanded(true);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.setStyle(TEXT_COLOR_STYLE);
        return pane;
    }

    public static void addFormRow(VBox form, String labelText, Control control) {
        addFormRow(form, labelText, control, new VBox());
    }

    public static void addFormRow(VBox form, String labelText, javafx.scene.Node control) {
        addFormRow(form, labelText, control, new VBox());
    }

    public static void addFormRow(VBox form, String labelText, javafx.scene.Node control, VBox field) {
        detachFromParent(control);
        detachFromParent(field);
        var label = new Label(labelText);
        if (control instanceof Control fxControl) {
            label.setLabelFor(fxControl);
            label.setTooltip(fxControl.getTooltip());
            fxControl.setMaxWidth(Double.MAX_VALUE);
        }
        if (control instanceof Label valueLabel) {
            valueLabel.setStyle(TEXT_COLOR_STYLE);
        }
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle(TEXT_COLOR_STYLE);
        field.setSpacing(2);
        field.getChildren().setAll(label, control);
        field.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().add(field);
        VBox.setVgrow(control, Priority.NEVER);
    }

    public static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public static void detachFromParent(javafx.scene.Node node) {
        if (node.getParent() instanceof javafx.scene.layout.Pane pane) {
            pane.getChildren().remove(node);
        }
    }

    public static void installTooltip(Control control, String text) {
        control.setTooltip(new Tooltip(text));
    }

    public static void addDraftListener(TextInputControl control, Runnable action) {
        control.textProperty().addListener((obs, old, value) -> action.run());
    }

    public static void addSpinnerListener(Spinner<Integer> spinner, Runnable action) {
        spinner.valueProperty().addListener((obs, old, value) -> action.run());
        spinner.getEditor().textProperty().addListener((obs, old, value) -> action.run());
    }

    public static Spinner<Integer> regionSpinner() {
        var spinner = new Spinner<Integer>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        return spinner;
    }

    public static void setRegionSpinnerText(Spinner<Integer> spinner, String value) {
        spinner.getEditor().setText(value);
        if (value != null && !value.isBlank()) {
            spinner.getValueFactory().setValue(Integer.parseInt(value.trim()));
        }
    }
}
