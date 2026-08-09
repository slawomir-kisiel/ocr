package pl.sk.ocr.configurator.properties;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        var list = new ListView<ExtensionOption>();
        list.getItems().setAll(options(type, currentId));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ExtensionOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label());
            }
        });
        list.getSelectionModel().selectFirst();
        list.setPrefHeight(260);
        dialog.getDialogPane().setContent(new VBox(8, new Label("Available extensions"), list));
        dialog.setResultConverter(button -> button == ButtonType.OK ? list.getSelectionModel().getSelectedItem() : null);
        return dialog.showAndWait();
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
    }
}
