package pl.sk.ocr.configurator.result;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.result.FieldResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;

public final class FieldResultPanel {
    private static final String TEXT_COLOR_STYLE = "-fx-text-fill: #111827;";

    private final Supplier<FieldResult> resultSupplier;
    private final Supplier<ProcessingTrace> traceSupplier;
    private final Label field = label("Field: -");
    private final Label status = label("Status: -");
    private final TextArea rawOcr = textArea();
    private final TextArea value = textArea();
    private final ListView<String> issues = new ListView<>();
    private final VBox root;

    public FieldResultPanel(Supplier<FieldResult> resultSupplier, Supplier<ProcessingTrace> traceSupplier) {
        this.resultSupplier = resultSupplier;
        this.traceSupplier = traceSupplier;
        issues.setPrefHeight(90);
        issues.setCellFactory(list -> textCell());
        root = new VBox(6,
            field,
            status,
            label("Raw OCR"),
            rawOcr,
            label("Value after transformers"),
            value,
            label("Issues"),
            issues
        );
        VBox.setVgrow(value, Priority.ALWAYS);
        refresh();
    }

    public Node view() {
        return root;
    }

    public void refresh() {
        var result = resultSupplier.get();
        if (result == null) {
            field.setText("Field: -");
            status.setText("Status: -");
            rawOcr.setText("");
            value.setText("");
            issues.getItems().setAll("No field preview result");
            return;
        }
        field.setText("Field: " + result.fieldId().value());
        status.setText("Status: " + result.status());
        rawOcr.setText(rawOcr());
        value.setText(result.value() == null ? "" : result.value());
        if (result.issues().isEmpty()) {
            issues.getItems().setAll("No issues");
        } else {
            issues.getItems().setAll(result.issues().stream()
                .map(this::issueText)
                .toList());
        }
    }

    private String rawOcr() {
        var trace = traceSupplier.get();
        if (trace == null) {
            return "";
        }
        return trace.entries().stream()
            .flatMap(entry -> entry.attributes().entrySet().stream())
            .filter(entry -> "rawOcr".equals(entry.getKey()))
            .map(Map.Entry::getValue)
            .filter(value -> value != null)
            .map(Object::toString)
            .max(Comparator.comparingInt(String::length))
            .orElse("");
    }

    private String issueText(ProcessingIssue issue) {
        var target = issue.extensionId() != null ? " | extension=" + issue.extensionId().value() : "";
        return issue.severity() + " " + issue.code().value() + target + " | " + issue.message();
    }

    private static Label label(String text) {
        var label = new Label(text);
        label.setStyle(TEXT_COLOR_STYLE);
        return label;
    }

    private static TextArea textArea() {
        var area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(2);
        area.setStyle(TEXT_COLOR_STYLE);
        return area;
    }

    private static ListCell<String> textCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(TEXT_COLOR_STYLE);
            }
        };
    }
}
