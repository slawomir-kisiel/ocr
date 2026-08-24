package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.titledPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.ProfileDto;
import pl.sk.ocr.config.dto.ProfilePreprocessingDto;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class ProfilePreprocessingPanel {
    private final Supplier<ProfileDto> profile;
    private final Consumer<ProfileDto> updateProfile;
    private final Runnable afterChange;
    private final Runnable applyPreprocessing;
    private final ExtensionPicker extensionPicker;
    private final ExtensionParametersForm parametersForm;
    private final ExtensionRegistry extensionRegistry;
    private final Function<Integer, ProcessingImage> debugSourceImage;
    private final ListView<ExtensionRefDto> steps = new ListView<>();
    private final VBox parameters = new VBox(4);

    public ProfilePreprocessingPanel(Supplier<ProfileDto> profile, Consumer<ProfileDto> updateProfile,
                                     Runnable afterChange, Runnable applyPreprocessing,
                                     ExtensionRegistry extensionRegistry,
                                     Function<Integer, ProcessingImage> debugSourceImage) {
        this.profile = profile;
        this.updateProfile = updateProfile;
        this.afterChange = afterChange;
        this.applyPreprocessing = applyPreprocessing;
        this.extensionRegistry = extensionRegistry;
        this.debugSourceImage = debugSourceImage;
        this.extensionPicker = new ExtensionPicker(extensionRegistry);
        this.parametersForm = new ExtensionParametersForm(extensionRegistry);
        configure();
    }

    public Node view() {
        var add = iconButton("plus.svg", "Dodaj", this::addStep);
        var choose = iconButton("edit.svg", "Wybierz rozszerzenie", this::chooseStep);
        var duplicate = iconButton("copy.svg", "Duplikuj", this::duplicateStep);
        var moveUp = iconButton("angle-up.svg", "Przenieś wyżej", () -> moveSelected(-1));
        var moveDown = iconButton("angle-down.svg", "Przenieś niżej", () -> moveSelected(1));
        var debug = iconButton("debug.svg", "Debug selected step", this::debugSelected);
        var remove = iconButton("eraser.svg", "Usuń", this::removeSelected);
        var apply = button("Apply preprocessing", applyPreprocessing);
        var actions = new HBox(6, add, choose, duplicate, moveUp, moveDown, debug, remove);
        var content = new VBox(6, new Label("Workspace image processors"), steps, actions, parameters, apply);
        content.setPadding(new javafx.geometry.Insets(8));
        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return titledPane("Preprocessing", scroll);
    }

    public void refresh() {
        steps.getItems().setAll(currentSteps());
        refreshParameters();
    }

    private void configure() {
        steps.setPrefHeight(260);
        steps.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ExtensionRefDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.id());
            }
        });
        steps.getSelectionModel().selectedIndexProperty().addListener((obs, old, selected) -> refreshParameters());
    }

    private void refreshParameters() {
        parameters.getChildren().clear();
        var index = selectedIndex();
        var current = currentSteps();
        if (index >= 0 && index < current.size() && parametersForm.hasParameters(current.get(index), ExtensionType.IMAGE_PROCESSOR)) {
            parameters.getChildren().add(indentedParameters(current.get(index), ref -> replaceStep(index, ref)));
        }
    }

    private Node indentedParameters(ExtensionRefDto ref, Consumer<ExtensionRefDto> onChange) {
        var content = parametersForm.inlineView(ref, ExtensionType.IMAGE_PROCESSOR, onChange);
        var box = new HBox(8, parameterGuideLine(), content);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);
        return box;
    }

    private Node parameterGuideLine() {
        var line = new javafx.scene.layout.Region();
        line.setMinWidth(2);
        line.setPrefWidth(2);
        line.setMaxWidth(2);
        line.setStyle("-fx-background-color: #94a3b8;");
        return line;
    }

    private void addStep() {
        extensionPicker.chooseRef(ExtensionType.IMAGE_PROCESSOR, null).ifPresent(ref -> {
            var updated = new ArrayList<>(currentSteps());
            updated.add(ref);
            updateSteps(updated);
            steps.getSelectionModel().select(updated.size() - 1);
        });
    }

    private void chooseStep() {
        var index = selectedIndex();
        var current = currentSteps();
        if (index < 0 || index >= current.size()) {
            return;
        }
        var previous = current.get(index);
        extensionPicker.chooseRef(ExtensionType.IMAGE_PROCESSOR, previous.id()).ifPresent(ref -> replaceStep(index, ref));
    }

    private void duplicateStep() {
        var index = selectedIndex();
        var current = currentSteps();
        if (index < 0 || index >= current.size()) {
            return;
        }
        var updated = new ArrayList<>(current);
        updated.add(index + 1, current.get(index));
        updateSteps(updated);
        steps.getSelectionModel().select(index + 1);
    }

    private void removeSelected() {
        var index = selectedIndex();
        var current = currentSteps();
        if (index < 0 || index >= current.size()) {
            return;
        }
        var updated = new ArrayList<>(current);
        updated.remove(index);
        updateSteps(updated);
        steps.getSelectionModel().select(Math.min(index, updated.size() - 1));
    }

    private void moveSelected(int delta) {
        var index = selectedIndex();
        var target = index + delta;
        var current = currentSteps();
        if (index < 0 || target < 0 || index >= current.size() || target >= current.size()) {
            return;
        }
        var updated = new ArrayList<>(current);
        var item = updated.remove(index);
        updated.add(target, item);
        updateSteps(updated);
        steps.getSelectionModel().select(target);
    }

    private void replaceStep(int index, ExtensionRefDto ref) {
        var updated = new ArrayList<>(currentSteps());
        updated.set(index, ref);
        updateSteps(updated);
        steps.getSelectionModel().select(index);
    }

    private void debugSelected() {
        var index = selectedIndex();
        var current = currentSteps();
        if (index < 0 || index >= current.size()) {
            return;
        }
        var source = debugSourceImage == null ? null : debugSourceImage.apply(index);
        new ImageProcessorDebugDialog(extensionRegistry).show(current.get(index), source)
            .ifPresent(ref -> replaceStep(index, ref));
    }

    private void updateSteps(List<ExtensionRefDto> updated) {
        updateProfile.accept(profileWithSteps(updated));
        afterChange.run();
        refresh();
    }

    private ProfileDto profileWithSteps(List<ExtensionRefDto> updated) {
        var base = profile.get();
        return new ProfileDto(
            base.schemaVersion(),
            base.id(),
            base.version(),
            base.displayName(),
            base.description(),
            base.categories(),
            new ProfilePreprocessingDto(List.copyOf(updated)),
            base.directories(),
            base.processing(),
            base.ocr(),
            base.trace(),
            base.output()
        );
    }

    private List<ExtensionRefDto> currentSteps() {
        var current = profile.get();
        if (current == null || current.preprocessing() == null || current.preprocessing().imageProcessors() == null) {
            return List.of();
        }
        return current.preprocessing().imageProcessors();
    }

    private int selectedIndex() {
        return steps.getSelectionModel().getSelectedIndex();
    }

    private Button button(String text, Runnable action) {
        var button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button iconButton(String iconName, String tooltip, Runnable action) {
        var button = button("", action);
        button.setGraphic(svgIcon(iconName));
        button.setTooltip(new Tooltip(tooltip));
        button.setMinSize(36, 32);
        button.setPrefSize(36, 32);
        button.setMaxSize(36, 32);
        return button;
    }

    private SVGPath svgIcon(String iconName) {
        var path = new SVGPath();
        path.setContent(svgPathContent(iconName));
        path.setFill(Color.web("#2f3742"));
        path.setScaleX(0.032);
        path.setScaleY(0.032);
        return path;
    }

    private String svgPathContent(String iconName) {
        var resource = "/icons/" + iconName;
        try (var input = ProfilePreprocessingPanel.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing icon resource: " + resource);
            }
            var svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = Pattern.compile("<path\\s+[^>]*d=\"([^\"]+)\"").matcher(svg);
            if (!matcher.find()) {
                throw new IllegalStateException("Icon has no path data: " + resource);
            }
            return matcher.group(1);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot read icon resource: " + resource, ex);
        }
    }
}
