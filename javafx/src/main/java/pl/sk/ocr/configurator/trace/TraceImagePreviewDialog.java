package pl.sk.ocr.configurator.trace;

import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
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
import javafx.scene.layout.HBox;
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
    private final Label leftTitle = label("Input");
    private final Label rightTitle = label("Output");
    private final Button previous = new Button("Poprzedni");
    private final Button next = new Button("Następny");
    private List<ImagePreview> images = List.of();
    private int currentIndex;
    private double zoom = 1.0;

    void show(String title, List<ImagePreview> images) {
        show(title, images, 0);
    }

    void show(String title, List<ImagePreview> images, int selectedIndex) {
        this.images = images == null ? List.of() : List.copyOf(images);
        this.currentIndex = Math.max(0, Math.min(selectedIndex, this.images.size() - 1));
        var dialog = new Dialog<Void>();
        dialog.setTitle(title == null || title.isBlank() ? "Trace Image" : title);
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(1100, 720);
        dialog.getDialogPane().setContent(content(title));
        renderCurrent();
        dialog.showAndWait();
    }

    private BorderPane content(String title) {
        configureScroll(leftScroll);
        configureScroll(rightScroll);
        previous.setOnAction(event -> {
            currentIndex = Math.max(0, currentIndex - 1);
            renderCurrent();
        });
        next.setOnAction(event -> {
            currentIndex = Math.min(images.size() - 1, currentIndex + 1);
            renderCurrent();
        });
        var navigation = new HBox(6, previous, next, zoomLabel);
        var header = new VBox(4, label(title), navigation);
        header.setPadding(new Insets(8));
        zoomLabel.setStyle("-fx-text-fill: #111827;");
        var root = new BorderPane(previews());
        root.setTop(header);
        if (images == null || images.isEmpty()) {
            root.setCenter(new Label("Image not available"));
        }
        return root;
    }

    private GridPane previews() {
        var grid = new GridPane();
        grid.setHgap(8);
        var left = preview(leftTitle, leftScroll);
        var right = preview(rightTitle, rightScroll);
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

    private VBox preview(Label title, ScrollPane scroll) {
        var box = new VBox(6, title, scroll);
        box.setPadding(new Insets(8));
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return box;
    }

    private String title(ImagePreview image, String fallback) {
        if (image == null || image.ref() == null) {
            return fallback;
        }
        return image.ref().label();
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

    private void renderCurrent() {
        var pair = currentPair();
        leftTitle.setText(title(pair.left(), "Input"));
        rightTitle.setText(title(pair.right(), "Output"));
        render(leftView, pair.left() == null ? null : pair.left().image());
        render(rightView, pair.right() == null ? null : pair.right().image());
        applyImageSize(leftView);
        applyImageSize(rightView);
        previous.setDisable(images.size() <= 1 || currentIndex == 0);
        next.setDisable(images.size() <= 1 || currentIndex >= images.size() - 1);
    }

    private PreviewPair currentPair() {
        if (images.isEmpty()) {
            return new PreviewPair(null, null);
        }
        if (images.size() == 1) {
            return new PreviewPair(images.get(0), null);
        }
        if (currentIndex < images.size() - 1) {
            return new PreviewPair(images.get(currentIndex), images.get(currentIndex + 1));
        }
        return new PreviewPair(images.get(currentIndex - 1), images.get(currentIndex));
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

    private record PreviewPair(ImagePreview left, ImagePreview right) {
    }
}
