package pl.sk.ocr.configurator.settings;

import java.util.Comparator;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

public final class LoadedExtensionsDialog {
    public void show(ExtensionRegistry registry) {
        var dialog = new Dialog<Void>();
        dialog.setTitle("Loaded Extensions");
        dialog.setHeaderText("Loaded extensions");
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        var typeFilter = new ComboBox<String>();
        typeFilter.getItems().add("All");
        for (var type : ExtensionType.values()) {
            typeFilter.getItems().add(type.name());
        }
        typeFilter.getSelectionModel().select("All");

        var rows = FXCollections.observableArrayList(registry.extensions().stream()
            .map(extension -> {
                var descriptor = extension.descriptor();
                return new ExtensionRow(
                    descriptor.type().name(),
                    descriptor.id().value(),
                    descriptor.displayName(),
                    descriptor.description(),
                    descriptor.version(),
                    extension.getClass().getName()
                );
            })
            .sorted(Comparator.comparing(ExtensionRow::type).thenComparing(ExtensionRow::id))
            .toList());
        var filtered = new FilteredList<>(rows, row -> true);
        typeFilter.valueProperty().addListener((obs, old, value) ->
            filtered.setPredicate(row -> value == null || value.equals("All") || row.type().equals(value)));

        var table = new TableView<ExtensionRow>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefSize(900, 420);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().add(column("Type", ExtensionRow::type, 120));
        table.getColumns().add(column("ID", ExtensionRow::id, 140));
        table.getColumns().add(column("Display Name", ExtensionRow::displayName, 180));
        table.getColumns().add(column("Description", ExtensionRow::description, 280));
        table.getColumns().add(column("Version", ExtensionRow::version, 90));
        table.getColumns().add(column("Provider", ExtensionRow::provider, 260));

        var content = new VBox(8, typeFilter, table);
        content.setPrefSize(900, 420);
        dialog.getDialogPane().setContent(content);
        dialog.setOnShown(event -> {
            if (dialog.getDialogPane().getScene().getWindow() instanceof Stage stage) {
                stage.setMinWidth(760);
                stage.setMinHeight(420);
                stage.setMaxWidth(Double.MAX_VALUE);
                stage.setMaxHeight(Double.MAX_VALUE);
            }
        });
        dialog.showAndWait();
    }

    private TableColumn<ExtensionRow, String> column(String title, Function<ExtensionRow, String> value, double width) {
        var column = new TableColumn<ExtensionRow, String>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    public record ExtensionRow(String type, String id, String displayName, String description, String version, String provider) {
    }
}
