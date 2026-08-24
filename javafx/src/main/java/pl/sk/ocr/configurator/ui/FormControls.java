package pl.sk.ocr.configurator.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
        field.setSpacing(1);
        field.getChildren().setAll(label, control);
        field.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().add(field);
        VBox.setVgrow(control, Priority.NEVER);
    }

    public static void addRegionRows(VBox form, Control x, Control y, Control width, Control height) {
        form.getChildren().add(regionRows(x, y, width, height));
    }

    public static HBox regionRowsWithActions(Control x, Control y, Control width, Control height, javafx.scene.Node actions) {
        detachFromParent(actions);
        var box = new HBox(8, regionRows(x, y, width, height), actions);
        HBox.setHgrow(box.getChildren().get(0), Priority.ALWAYS);
        return box;
    }

    private static GridPane regionRows(Control x, Control y, Control width, Control height) {
        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        addRegionCell(grid, "X", x, 0, 0);
        addRegionCell(grid, "Y", y, 1, 0);
        addRegionCell(grid, "W", width, 0, 1);
        addRegionCell(grid, "H", height, 1, 1);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private static void addRegionCell(GridPane grid, String labelText, Control control, int column, int row) {
        detachFromParent(control);
        var field = new VBox(2);
        var label = new Label(labelText);
        label.setLabelFor(control);
        label.setTooltip(control.getTooltip());
        label.setStyle(TEXT_COLOR_STYLE);
        control.setMaxWidth(Double.MAX_VALUE);
        field.getChildren().addAll(label, control);
        field.setMaxWidth(Double.MAX_VALUE);
        grid.add(field, column, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
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
        var valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE, 0) {
            @Override
            public void decrement(int steps) {
                if (getValue() != null) {
                    super.decrement(steps);
                }
            }

            @Override
            public void increment(int steps) {
                if (getValue() != null) {
                    super.increment(steps);
                }
            }
        };
        var spinner = new Spinner<Integer>();
        spinner.setValueFactory(valueFactory);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        return spinner;
    }

    public static void setRegionSpinnerText(Spinner<Integer> spinner, String value) {
        spinner.getEditor().setText(value);
        if (value != null && !value.isBlank()) {
            spinner.getValueFactory().setValue(Integer.parseInt(value.trim()));
        } else {
            spinner.getValueFactory().setValue(null);
        }
    }
}
