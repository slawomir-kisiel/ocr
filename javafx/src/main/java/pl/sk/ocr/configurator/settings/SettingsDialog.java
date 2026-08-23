package pl.sk.ocr.configurator.settings;

import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import pl.sk.ocr.configurator.app.ApplicationPreferences;

import static pl.sk.ocr.configurator.ui.FormControls.addFormRow;
import static pl.sk.ocr.configurator.ui.FormControls.installTooltip;

public final class SettingsDialog {
    private static final String CUSTOM_DPI = "Custom";

    public Optional<ApplicationPreferences.Settings> show(ApplicationPreferences.Settings current) {
        var dialog = new Dialog<ApplicationPreferences.Settings>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Runtime settings");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        var datapath = new TextField(nullToBlank(current.tesseractDatapath()));
        installTooltip(datapath, "Directory containing Tesseract traineddata files.");
        var language = new TextField(nullToBlank(current.defaultOcrLanguage()));
        installTooltip(language, "Default language used by ad-hoc OCR operations.");
        var dpiPreset = new ComboBox<String>();
        dpiPreset.getItems().addAll("72", "100", "150", "200", "300", CUSTOM_DPI);
        dpiPreset.getSelectionModel().select(standardDpiValue(current.defaultPdfDpi()).orElse(CUSTOM_DPI));
        installTooltip(dpiPreset, "Default DPI used when opening reference PDF documents.");
        var customDpi = new Spinner<Integer>(1, 1200, current.defaultPdfDpi());
        customDpi.setEditable(true);
        installTooltip(customDpi, "Custom default DPI used when opening reference PDF documents.");
        var customDpiRow = new VBox();
        customDpiRow.visibleProperty().bind(dpiPreset.valueProperty().isEqualTo(CUSTOM_DPI));
        customDpiRow.managedProperty().bind(customDpiRow.visibleProperty());
        dpiPreset.valueProperty().addListener((obs, old, value) -> Platform.runLater(() -> {
            var window = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
            if (window != null) {
                window.sizeToScene();
            }
        }));
        var lastDirectory = new TextField(nullToBlank(current.lastOpenedDirectory()));
        installTooltip(lastDirectory, "Generic last opened directory remembered by the UI.");
        var cacheLimit = new Spinner<Integer>(1, 10000, current.cacheLimit());
        cacheLimit.setEditable(true);
        installTooltip(cacheLimit, "Preferred UI cache limit for rendered/OCR artifacts.");

        var form = new VBox(8);
        form.setPrefWidth(520);
        addFormRow(form, "Tesseract datapath", datapath);
        addFormRow(form, "Default OCR language", language);
        addFormRow(form, "Default PDF DPI", dpiPreset);
        addFormRow(form, "Custom PDF DPI", customDpi, customDpiRow);
        addFormRow(form, "Last opened directory", lastDirectory);
        addFormRow(form, "Cache limit", cacheLimit);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return new ApplicationPreferences.Settings(
                blankToNull(datapath.getText()),
                blankToDefault(language.getText(), "pol"),
                selectedDpi(dpiPreset, customDpi),
                blankToNull(lastDirectory.getText()),
                cacheLimit.getValue()
            );
        });
        return dialog.showAndWait();
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static String blankToDefault(String value, String defaultValue) {
        var normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Optional<String> standardDpiValue(int dpi) {
        return switch (dpi) {
            case 72, 100, 150, 200, 300 -> Optional.of(Integer.toString(dpi));
            default -> Optional.empty();
        };
    }

    private static int selectedDpi(ComboBox<String> preset, Spinner<Integer> customDpi) {
        var selected = preset.getValue();
        if (CUSTOM_DPI.equals(selected)) {
            return customDpi.getValue();
        }
        return Integer.parseInt(selected);
    }
}
