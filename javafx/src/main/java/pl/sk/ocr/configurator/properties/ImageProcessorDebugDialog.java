package pl.sk.ocr.configurator.properties;

import java.util.Optional;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;
import pl.sk.ocr.extension.api.trace.TraceSink;

final class ImageProcessorDebugDialog {
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 8.0;

    private final ExtensionRegistry registry;
    private final ExtensionParametersForm parametersForm;
    private final ImageView sourceView = new ImageView();
    private final ImageView resultView = new ImageView();
    private final ScrollPane sourceScroll = new ScrollPane(sourceView);
    private final ScrollPane resultScroll = new ScrollPane(resultView);
    private final VBox parameters = new VBox(8);
    private final Label status = new Label();
    private ExtensionRefDto currentRef;
    private ProcessingImage sourceImage;
    private double zoom = 1.0;

    ImageProcessorDebugDialog(ExtensionRegistry registry) {
        this.registry = registry;
        this.parametersForm = new ExtensionParametersForm(registry);
    }

    Optional<ExtensionRefDto> show(ExtensionRefDto ref, ProcessingImage sourceImage) {
        this.currentRef = ref;
        this.sourceImage = sourceImage;
        var dialog = new Dialog<ExtensionRefDto>();
        dialog.setTitle("Image Processor Debug");
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(1100, 720);
        dialog.getDialogPane().setContent(content());
        dialog.setResultConverter(button -> button == ButtonType.OK ? currentRef : null);
        refreshParameters();
        renderSource();
        apply();
        return dialog.showAndWait();
    }

    private Node content() {
        configureScroll(sourceScroll);
        configureScroll(resultScroll);
        var previews = previews();
        var apply = new Button("Apply");
        apply.setOnAction(event -> apply());
        var side = new VBox(8, new Label("Parameters"), parameters, apply, status);
        side.setPadding(new Insets(8));
        side.setPrefWidth(320);
        status.setWrapText(true);
        var root = new BorderPane(previews);
        root.setRight(side);
        return root;
    }

    private GridPane previews() {
        var grid = new GridPane();
        grid.setHgap(8);
        var left = preview("Source", sourceScroll);
        var right = preview("Result", resultScroll);
        var half = new ColumnConstraints();
        half.setPercentWidth(50);
        half.setHgrow(Priority.ALWAYS);
        var otherHalf = new ColumnConstraints();
        otherHalf.setPercentWidth(50);
        otherHalf.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(half, otherHalf);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        GridPane.setHgrow(left, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.ALWAYS);
        GridPane.setVgrow(left, Priority.ALWAYS);
        GridPane.setVgrow(right, Priority.ALWAYS);
        return grid;
    }

    private VBox preview(String title, ScrollPane scroll) {
        var label = new Label(title);
        label.setStyle("-fx-text-fill: #111827; -fx-font-weight: bold;");
        var box = new VBox(6, label, scroll);
        box.setPadding(new Insets(8));
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return box;
    }

    private void configureScroll(ScrollPane scroll) {
        scroll.setPannable(true);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!event.isControlDown()) {
                return;
            }
            event.consume();
            setZoom(zoom * (event.getDeltaY() > 0 ? 1.15 : 1 / 1.15));
        });
    }

    private void refreshParameters() {
        parameters.getChildren().setAll(parametersForm.view(currentRef, ExtensionType.IMAGE_PROCESSOR, ref -> currentRef = ref));
    }

    private void renderSource() {
        if (sourceImage != null) {
            sourceView.setImage(SwingFXUtils.toFXImage(sourceImage.asBufferedImage(), null));
        }
        applyImageSize(sourceView);
    }

    private void apply() {
        if (sourceImage == null) {
            status.setText("No source image available for current page.");
            resultView.setImage(null);
            return;
        }
        if (currentRef == null || currentRef.id() == null || currentRef.id().isBlank()) {
            status.setText("Choose image processor first.");
            resultView.setImage(null);
            return;
        }
        try {
            var extension = registry.require(new ExtensionId(currentRef.id()));
            if (!(extension instanceof ImageProcessor processor)) {
                throw new IllegalArgumentException("Selected extension is not an ImageProcessor: " + currentRef.id());
            }
            var output = processor.process(
                new ImageProcessingRequest(sourceImage, ExtensionParameters.of(currentRef.parameters())),
                () -> TraceSink.NOOP
            );
            resultView.setImage(SwingFXUtils.toFXImage(output.asBufferedImage(), null));
            applyImageSize(resultView);
            status.setText("Debug output ready.");
        } catch (RuntimeException e) {
            resultView.setImage(null);
            status.setText(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Image Processor Debug");
            alert.setHeaderText("Debug step failed");
            alert.setContentText(status.getText());
            alert.showAndWait();
        }
    }

    private void setZoom(double value) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        applyImageSize(sourceView);
        applyImageSize(resultView);
    }

    private void applyImageSize(ImageView view) {
        if (view.getImage() == null) {
            return;
        }
        view.setPreserveRatio(true);
        view.setFitWidth(view.getImage().getWidth() * zoom);
        view.setFitHeight(view.getImage().getHeight() * zoom);
    }
}
