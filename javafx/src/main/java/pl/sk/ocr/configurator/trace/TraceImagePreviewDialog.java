package pl.sk.ocr.configurator.trace;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pl.sk.ocr.extension.api.image.ProcessingImage;

final class TraceImagePreviewDialog {
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 8.0;

    private final ImageView imageView = new ImageView();
    private final ScrollPane scroll = new ScrollPane(imageView);
    private final Label zoomLabel = new Label("100%");
    private double zoom = 1.0;

    void show(String title, ProcessingImage image) {
        var dialog = new Dialog<Void>();
        dialog.setTitle(title == null || title.isBlank() ? "Trace Image" : title);
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(900, 700);
        dialog.getDialogPane().setContent(content(title, image));
        render(image);
        dialog.showAndWait();
    }

    private BorderPane content(String title, ProcessingImage image) {
        configureScroll();
        var header = new VBox(4, label(title), zoomLabel);
        header.setPadding(new Insets(8));
        zoomLabel.setStyle("-fx-text-fill: #111827;");
        var root = new BorderPane(scroll);
        root.setTop(header);
        if (image == null) {
            root.setCenter(new Label("Image not available"));
        }
        return root;
    }

    private void configureScroll() {
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

    private void render(ProcessingImage image) {
        if (image == null) {
            return;
        }
        imageView.setImage(SwingFXUtils.toFXImage(image.asBufferedImage(), null));
        applyImageSize();
    }

    private void setZoom(double value) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        applyImageSize();
    }

    private void applyImageSize() {
        if (imageView.getImage() == null) {
            return;
        }
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(imageView.getImage().getWidth() * zoom);
        imageView.setFitHeight(imageView.getImage().getHeight() * zoom);
        zoomLabel.setText(Math.round(zoom * 100) + "%");
    }

    private Label label(String text) {
        var label = new Label(text == null || text.isBlank() ? "Trace image" : text);
        label.setStyle("-fx-text-fill: #111827; -fx-font-weight: bold;");
        return label;
    }
}
