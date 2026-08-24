package pl.sk.ocr.configurator.properties;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

final class ExtensionPicker {
    private final ExtensionRegistry registry;

    ExtensionPicker(ExtensionRegistry registry) {
        this.registry = registry;
    }

    Optional<String> chooseId(ExtensionType type, String currentId) {
        return choose(type, currentId).map(ExtensionOption::id);
    }

    Optional<ExtensionRefDto> chooseRef(ExtensionType type, String currentId) {
        return choose(type, currentId).map(option -> new ExtensionRefDto(option.id(), java.util.Map.of()));
    }

    private Optional<ExtensionOption> choose(ExtensionType type, String currentId) {
        var dialog = new Dialog<ExtensionOption>();
        dialog.setTitle("Select Extension");
        dialog.setHeaderText(type.name());
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(920, 620);
        dialog.getDialogPane().setMinSize(640, 420);

        var search = new TextField();
        search.setPromptText("Search by name, id or description");
        search.setMaxWidth(Double.MAX_VALUE);

        var allOptions = options(type, currentId);
        var list = new ListView<ExtensionOption>();
        list.getItems().setAll(allOptions);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ExtensionOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label());
            }
        });
        selectCurrent(list, currentId);
        search.textProperty().addListener((obs, old, text) -> {
            var selected = list.getSelectionModel().getSelectedItem();
            var filtered = allOptions.stream()
                .filter(option -> option.matches(text))
                .toList();
            list.getItems().setAll(filtered);
            if (selected != null && filtered.stream().anyMatch(option -> option.id().equals(selected.id()))) {
                list.getSelectionModel().select(selected);
            } else {
                list.getSelectionModel().clearSelection();
            }
        });
        list.setPrefHeight(500);
        list.setMaxWidth(Double.MAX_VALUE);
        list.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(list, Priority.ALWAYS);
        var content = new VBox(8, new Label("Available extensions"), search, list);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMaxHeight(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(content);
        var okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        dialog.setResultConverter(button -> button == ButtonType.OK ? list.getSelectionModel().getSelectedItem() : null);
        return dialog.showAndWait();
    }

    private void selectCurrent(ListView<ExtensionOption> list, String currentId) {
        if (currentId == null || currentId.isBlank()) {
            list.getSelectionModel().clearSelection();
            return;
        }
        var normalized = currentId.trim();
        for (int i = 0; i < list.getItems().size(); i++) {
            if (list.getItems().get(i).id().equals(normalized)) {
                list.getSelectionModel().select(i);
                list.scrollTo(i);
                return;
            }
        }
        list.getSelectionModel().clearSelection();
    }

    private List<ExtensionOption> options(ExtensionType type, String currentId) {
        var items = registry.extensions().stream()
            .map(extension -> extension.descriptor())
            .filter(descriptor -> descriptor.type() == type)
            .sorted(Comparator.comparing(descriptor -> descriptor.id().value()))
            .map(ExtensionOption::resolved)
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (currentId != null && !currentId.isBlank()
            && items.stream().noneMatch(option -> option.id().equals(currentId.trim()))) {
            items.add(0, ExtensionOption.unresolved(currentId.trim()));
        }
        return items;
    }

    private record ExtensionOption(String id, String displayName, String description, String version, boolean resolved) {
        static ExtensionOption resolved(ExtensionDescriptor descriptor) {
            return new ExtensionOption(descriptor.id().value(), descriptor.displayName(), descriptor.description(), descriptor.version(), true);
        }

        static ExtensionOption unresolved(String id) {
            return new ExtensionOption(id, id, "Unresolved extension from opened JSON", "", false);
        }

        String label() {
            var state = resolved ? "" : " (unresolved)";
            var details = description == null || description.isBlank() ? "" : " - " + description;
            var versionText = version == null || version.isBlank() ? "" : " [" + version + "]";
            return displayName + state + " | " + id + versionText + details;
        }

        boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            var normalized = query.trim().toLowerCase(java.util.Locale.ROOT);
            return contains(id, normalized)
                || contains(displayName, normalized)
                || contains(description, normalized)
                || contains(version, normalized);
        }

        private boolean contains(String value, String query) {
            return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
        }
    }
}
