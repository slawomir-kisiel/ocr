package pl.sk.ocr.configurator.trace;

import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
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
import pl.sk.ocr.domain.trace.TraceImageRef;
import pl.sk.ocr.extension.api.image.ProcessingImage;

final class TraceImagePreviewDialog {
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 8.0;

    private final ImageView leftView = new ImageView();
    private final ImageView rightView = new ImageView();
    private final ScrollPane leftScroll = new ScrollPane(leftView);
    private final ScrollPane rightScroll = new ScrollPane(rightView);
    private final Label zoomLabel = new Label("100%");
    private double zoom = 1.0;

    void show(String title, List<ImagePreview> images) {
        var dialog = new Dialog<Void>();
        dialog.setTitle(title == null || title.isBlank() ? "Trace Image" : title);
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(1100, 720);
        dialog.getDialogPane().setContent(content(title, images));
        render(images);
        dialog.showAndWait();
    }

    private BorderPane content(String title, List<ImagePreview> images) {
        configureScroll(leftScroll);
        configureScroll(rightScroll);
        var header = new VBox(4, label(title), zoomLabel);
        header.setPadding(new Insets(8));
        zoomLabel.setStyle("-fx-text-fill: #111827;");
        var root = new BorderPane(previews(images));
        root.setTop(header);
        if (images == null || images.isEmpty()) {
            root.setCenter(new Label("Image not available"));
        }
        return root;
    }

    private GridPane previews(List<ImagePreview> images) {
        var grid = new GridPane();
        grid.setHgap(8);
        var left = preview(title(images, 0, "Input"), leftScroll);
        var right = preview(title(images, 1, "Output"), rightScroll);
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
        var box = new VBox(6, label(title), scroll);
        box.setPadding(new Insets(8));
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return box;
    }

    private String title(List<ImagePreview> images, int index, String fallback) {
        if (images == null || index >= images.size() || images.get(index) == null || images.get(index).ref() == null) {
            return fallback;
        }
        return images.get(index).ref().label();
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

    private void render(List<ImagePreview> images) {
        render(leftView, image(images, 0));
        render(rightView, image(images, 1));
        applyImageSize(leftView);
        applyImageSize(rightView);
    }

    private ProcessingImage image(List<ImagePreview> images, int index) {
        if (images == null || index >= images.size() || images.get(index) == null) {
            return null;
        }
        return images.get(index).image();
    }

    private void render(ImageView view, ProcessingImage image) {
        if (image != null) {
            view.setImage(SwingFXUtils.toFXImage(image.asBufferedImage(), null));
        } else {
            view.setImage(null);
        }
    }

    private void setZoom(double value) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        applyImageSize(leftView);
        applyImageSize(rightView);
    }

    private void applyImageSize(ImageView view) {
        if (view.getImage() == null) {
            return;
        }
        view.setPreserveRatio(true);
        view.setFitWidth(view.getImage().getWidth() * zoom);
        view.setFitHeight(view.getImage().getHeight() * zoom);
        zoomLabel.setText(Math.round(zoom * 100) + "%");
    }

    private Label label(String text) {
        var label = new Label(text == null || text.isBlank() ? "Trace image" : text);
        label.setStyle("-fx-text-fill: #111827; -fx-font-weight: bold;");
        return label;
    }

    record ImagePreview(TraceImageRef ref, ProcessingImage image) {
    }
}
