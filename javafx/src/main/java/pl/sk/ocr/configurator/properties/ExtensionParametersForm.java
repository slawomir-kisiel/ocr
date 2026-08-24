package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterDescriptor;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

final class ExtensionParametersForm {
    private static final String ERROR_STYLE = "-fx-text-fill: #b91c1c;";

    private final ExtensionRegistry registry;

    ExtensionParametersForm(ExtensionRegistry registry) {
        this.registry = registry;
    }

    Node view(ExtensionRefDto ref, ExtensionType expectedType, Consumer<ExtensionRefDto> onChange) {
        var content = new VBox(4);
        if (ref == null || ref.id() == null || ref.id().isBlank()) {
            content.getChildren().add(message("Choose an extension to edit parameters."));
            return titledPane("Parameters", content);
        }
        var descriptor = descriptor(ref.id(), expectedType);
        if (descriptor == null) {
            content.getChildren().add(message("Extension is unresolved. Parameters are preserved in JSON."));
            return titledPane("Parameters", content);
        }
        if (descriptor.parameters().isEmpty()) {
            content.getChildren().add(message("This extension has no parameters."));
            return titledPane("Parameters", content);
        }
        addParameters(content, ref, descriptor, onChange);
        return titledPane("Parameters", content);
    }

    Node inlineView(ExtensionRefDto ref, ExtensionType expectedType, Consumer<ExtensionRefDto> onChange) {
        var content = new VBox(4);
        if (ref == null || ref.id() == null || ref.id().isBlank()) {
            return content;
        }
        var descriptor = descriptor(ref.id(), expectedType);
        if (descriptor == null || descriptor.parameters().isEmpty()) {
            return content;
        }
        addParameters(content, ref, descriptor, onChange);
        return content;
    }

    boolean hasParameters(ExtensionRefDto ref, ExtensionType expectedType) {
        if (ref == null || ref.id() == null || ref.id().isBlank()) {
            return false;
        }
        var descriptor = descriptor(ref.id(), expectedType);
        return descriptor != null && !descriptor.parameters().isEmpty();
    }

    private void addParameters(VBox content, ExtensionRefDto ref, ExtensionDescriptor descriptor, Consumer<ExtensionRefDto> onChange) {
        var values = new LinkedHashMap<String, Object>(ref.parameters() == null ? Map.of() : ref.parameters());
        for (var parameter : descriptor.parameters()) {
            if (!values.containsKey(parameter.name()) && parameter.defaultValue() != null) {
                values.put(parameter.name(), parameter.defaultValue());
            }
        }
        for (var parameter : descriptor.parameters()) {
            addParameter(content, ref.id(), values, parameter, onChange);
        }
    }

    private ExtensionDescriptor descriptor(String id, ExtensionType expectedType) {
        try {
            return registry.find(new ExtensionId(id))
                .map(extension -> extension.descriptor())
                .filter(descriptor -> expectedType == null || descriptor.type() == expectedType)
                .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void addParameter(VBox content, String extensionId, Map<String, Object> values,
                              ExtensionParameterDescriptor parameter, Consumer<ExtensionRefDto> onChange) {
        var error = new Label();
        error.setStyle(ERROR_STYLE);
        error.setWrapText(true);
        error.managedProperty().bind(error.textProperty().isNotEmpty());
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        var control = control(parameter, values.getOrDefault(parameter.name(), parameter.defaultValue()), value -> {
            if (value == null) {
                values.remove(parameter.name());
            } else {
                values.put(parameter.name(), value);
            }
            var problem = validate(parameter, value);
            error.setText(problem == null ? "" : problem);
            onChange.accept(new ExtensionRefDto(extensionId, Map.copyOf(values)));
        }, error);
        installTooltip(control, parameter.description());
        addFormRow(content, parameter.displayName(), control);
        content.getChildren().add(error);
        var initial = validate(parameter, values.getOrDefault(parameter.name(), parameter.defaultValue()));
        error.setText(initial == null ? "" : initial);
    }

    private javafx.scene.control.Control control(ExtensionParameterDescriptor parameter, Object value,
                                                 Consumer<Object> onChange, Label error) {
        return switch (parameter.type()) {
            case STRING, REGEX -> textControl(value, onChange);
            case INTEGER -> integerControl(value, onChange, error);
            case DECIMAL -> decimalControl(value, onChange, error);
            case BOOLEAN -> booleanControl(value, onChange);
            case ENUM -> enumControl(parameter, value, onChange);
        };
    }

    private TextField textControl(Object value, Consumer<Object> onChange) {
        var field = new TextField(value == null ? "" : value.toString());
        field.textProperty().addListener((obs, old, text) -> onChange.accept(text == null || text.isBlank() ? null : text));
        return field;
    }

    private TextField integerControl(Object value, Consumer<Object> onChange, Label error) {
        var field = new TextField(value == null ? "" : value.toString());
        field.textProperty().addListener((obs, old, text) -> {
            if (text == null || text.isBlank()) {
                onChange.accept(null);
                return;
            }
            try {
                onChange.accept(Integer.parseInt(text.trim()));
            } catch (NumberFormatException e) {
                error.setText("Value must be an integer.");
            }
        });
        return field;
    }

    private TextField decimalControl(Object value, Consumer<Object> onChange, Label error) {
        var field = new TextField(value == null ? "" : value.toString());
        field.textProperty().addListener((obs, old, text) -> {
            if (text == null || text.isBlank()) {
                onChange.accept(null);
                return;
            }
            try {
                onChange.accept(Double.parseDouble(text.trim()));
            } catch (NumberFormatException e) {
                error.setText("Value must be decimal.");
            }
        });
        return field;
    }

    private CheckBox booleanControl(Object value, Consumer<Object> onChange) {
        var box = new CheckBox();
        box.setSelected(value instanceof Boolean bool && bool);
        box.selectedProperty().addListener((obs, old, selected) -> onChange.accept(selected));
        return box;
    }

    private ComboBox<String> enumControl(ExtensionParameterDescriptor parameter, Object value, Consumer<Object> onChange) {
        var combo = new ComboBox<String>();
        var allowed = parameter.constraints() == null ? java.util.List.<String>of() : parameter.constraints().allowedValues();
        combo.getItems().setAll(allowed);
        combo.setEditable(allowed.isEmpty());
        combo.setValue(value == null ? null : value.toString());
        combo.valueProperty().addListener((obs, old, selected) -> onChange.accept(selected == null || selected.isBlank() ? null : selected));
        return combo;
    }

    private String validate(ExtensionParameterDescriptor parameter, Object value) {
        if (value == null) {
            return parameter.required() ? "Required parameter." : null;
        }
        var constraints = parameter.constraints();
        if (constraints == null) {
            return null;
        }
        if (value instanceof Number number) {
            if (constraints.min() != null && number.doubleValue() < constraints.min().doubleValue()) {
                return "Value must be >= " + constraints.min() + ".";
            }
            if (constraints.max() != null && number.doubleValue() > constraints.max().doubleValue()) {
                return "Value must be <= " + constraints.max() + ".";
            }
        }
        if (value instanceof String text) {
            if (parameter.type() == pl.sk.ocr.extension.api.ExtensionParameterType.REGEX) {
                try {
                    Pattern.compile(text);
                } catch (PatternSyntaxException e) {
                    return "Value must be a valid regular expression.";
                }
            }
            if (constraints.pattern() != null && !constraints.pattern().matcher(text).matches()) {
                return "Value does not match required pattern.";
            }
            if (!constraints.allowedValues().isEmpty() && !constraints.allowedValues().contains(text)) {
                return "Value must be one of: " + String.join(", ", constraints.allowedValues()) + ".";
            }
        }
        return null;
    }

    private Label message(String text) {
        var label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #111827;");
        return label;
    }
}
