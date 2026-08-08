package pl.sk.ocr.configurator;

import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.ConfiguratorServices;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.app.RunPageOcrUseCase;
import pl.sk.ocr.configurator.viewer.ScaledCoordinateMapper;
import pl.sk.ocr.configurator.viewer.ViewerPoint;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
import pl.sk.ocr.domain.identifier.PageNumber;

public final class ConfiguratorApplication extends Application {
    private ConfiguratorServices services;
    private CategoryEditorViewModel viewModel;
    private final TreeView<String> configurationTree = new TreeView<>();
    private final ImageView pageImage = new ImageView();
    private final PaneOverlay viewer = new PaneOverlay(pageImage);
    private final TextArea details = new TextArea();
    private final ListView<String> validationList = new ListView<>();
    private final Label status = new Label("Ready");
    private final Label pageLabel = new Label("Page 0/0");
    private double zoom = 1.0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        services = ConfiguratorServices.production();
        viewModel = new CategoryEditorViewModel(
            new ConfigurationFileService(services.mapper()),
            new OpenReferenceDocumentUseCase(services.documentReader()),
            new RunPageOcrUseCase(services.ocrEngine()),
            services.validationService(),
            services.backgroundExecutor()
        );
        stage.setTitle("OCR Configurator");
        stage.setScene(new Scene(layout(stage), 1280, 820));
        stage.show();
    }

    @Override
    public void stop() {
        if (services != null) {
            services.backgroundExecutor().close();
        }
    }

    private BorderPane layout(Stage stage) {
        var root = new BorderPane();
        root.setTop(toolbar(stage));
        root.setCenter(center());
        root.setBottom(statusBar());
        refreshTree();
        refreshDetails();
        return root;
    }

    private ToolBar toolbar(Stage stage) {
        var newCategory = button("New Category", () -> {
            viewModel.newCategory("new-category", "New Category");
            refreshAll();
        });
        var openConfig = button("Open Configuration", () -> chooseCategory(stage));
        var save = button("Save", () -> saveCategory(stage, false));
        var saveAs = button("Save As", () -> saveCategory(stage, true));
        var openDocument = button("Open Document", () -> chooseDocument(stage));
        var previous = button("Previous Page", () -> changePage(-1));
        var next = button("Next Page", () -> changePage(1));
        var zoomIn = button("Zoom In", () -> setZoom(zoom * 1.25));
        var zoomOut = button("Zoom Out", () -> setZoom(zoom / 1.25));
        var fitPage = button("Fit Page", () -> setZoom(1.0));
        var runOcr = button("Run OCR", this::runOcr);
        var testCategory = button("Test Category", this::validate);
        var validate = button("Validate", this::validate);
        return new ToolBar(newCategory, openConfig, save, saveAs, new Separator(), openDocument,
            previous, next, zoomIn, zoomOut, fitPage, new Separator(), runOcr, testCategory, validate);
    }

    private SplitPane center() {
        configurationTree.setPrefWidth(280);
        configurationTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> refreshDetails());
        details.setEditable(false);
        details.setWrapText(true);
        validationList.setPrefHeight(180);
        var right = new VBox(8, new Label("Properties / Details"), details, new Label("Validation"), validationList);
        right.setPadding(new Insets(8));
        VBox.setVgrow(details, Priority.ALWAYS);
        var documentScroll = new ScrollPane(viewer);
        documentScroll.setFitToWidth(false);
        documentScroll.setFitToHeight(false);
        documentScroll.setPannable(true);
        documentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.addEventFilter(ScrollEvent.SCROLL, this::handleScrollZoom);
        documentScroll.addEventFilter(ZoomEvent.ZOOM, this::handleTouchpadZoom);
        var split = new SplitPane(configurationTree, documentScroll, right);
        split.setDividerPositions(0.22, 0.72);
        return split;
    }

    private HBox statusBar() {
        var bar = new HBox(16, status, pageLabel);
        bar.setPadding(new Insets(6, 10, 6, 10));
        return bar;
    }

    private Button button(String text, Runnable action) {
        var button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void chooseCategory(Stage stage) {
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Category JSON", "*.json"));
        var file = chooser.showOpenDialog(stage);
        if (file != null) {
            runUiSafe(() -> {
                viewModel.loadCategory(file.toPath());
                refreshAll();
            });
        }
    }

    private void saveCategory(Stage stage, boolean saveAs) {
        var path = viewModel.session().categoryPath();
        if (saveAs || path == null) {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Category JSON", "*.json"));
            var file = chooser.showSaveDialog(stage);
            path = file == null ? null : file.toPath();
        }
        if (path != null) {
            Path savePath = path;
            runUiSafe(() -> {
                viewModel.saveCategory(savePath);
                refreshAll();
            });
        }
    }

    private void chooseDocument(Stage stage) {
        var chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff"),
            new FileChooser.ExtensionFilter("All files", "*.*")
        );
        var file = chooser.showOpenDialog(stage);
        if (file != null) {
            status.setText("Opening document...");
            viewModel.openReferenceDocument(file.toPath())
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showError(error);
                    } else {
                        renderPage();
                        refreshAll();
                    }
                }));
        }
    }

    private void runOcr() {
        status.setText("Running OCR...");
        viewModel.runCurrentPageOcr()
            .whenComplete((ocr, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error);
                } else {
                    renderOcrOverlay();
                    refreshAll();
                }
            }));
    }

    private void validate() {
        viewModel.validate();
        refreshAll();
    }

    private void changePage(int delta) {
        var pages = viewModel.session().pageCache().size();
        if (pages == 0) {
            return;
        }
        viewModel.session().currentPage(Math.max(1, Math.min(pages, viewModel.session().currentPage() + delta)));
        renderPage();
        refreshAll();
    }

    private void setZoom(double value) {
        zoom = Math.max(0.2, Math.min(5.0, value));
        pageImage.setScaleX(zoom);
        pageImage.setScaleY(zoom);
        viewer.setMapper(new ScaledCoordinateMapper(zoom, 0, 0));
        renderOcrOverlay();
        refreshAll();
    }

    private void handleScrollZoom(ScrollEvent event) {
        if (!event.isControlDown()) {
            return;
        }
        var factor = event.getDeltaY() > 0 ? 1.10 : 1.0 / 1.10;
        setZoom(zoom * factor);
        event.consume();
    }

    private void handleTouchpadZoom(ZoomEvent event) {
        setZoom(zoom * event.getZoomFactor());
        event.consume();
    }

    private void renderPage() {
        var page = viewModel.session().pageCache().get(new PageNumber(viewModel.session().currentPage()));
        if (page != null) {
            pageImage.setImage(SwingFXUtils.toFXImage(page.asBufferedImage(), null));
        }
        renderOcrOverlay();
    }

    private void renderOcrOverlay() {
        viewer.clearOverlay();
        var ocr = viewModel.session().ocrCache().get(new PageNumber(viewModel.session().currentPage()));
        if (ocr == null) {
            return;
        }
        for (var word : ocr.words()) {
            var screen = viewer.mapper().imageToScreen(word.boundingBox().region());
            var rectangle = new Rectangle(screen.x(), screen.y(), screen.width(), screen.height());
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setStroke(Color.web("#1f7aec"));
            rectangle.setStrokeWidth(1.0);
            rectangle.setOnMouseClicked(event -> {
                details.setText("OCR word\ntext=" + word.text() + "\nconfidence=" + word.confidence().value()
                    + "\nbounds=" + word.boundingBox().region());
                event.consume();
            });
            viewer.addOverlay(rectangle);
        }
    }

    private void refreshAll() {
        refreshTree();
        refreshDetails();
        status.setText(viewModel.status());
        pageLabel.setText("Page " + viewModel.session().currentPage() + "/" + viewModel.session().pageCache().size()
            + " | Zoom " + Math.round(zoom * 100) + "%");
        validationList.getItems().setAll(viewModel.validationProblems().stream()
            .map(problem -> problem.code() + " " + problem.path() + " " + problem.message())
            .toList());
    }

    private void refreshTree() {
        var draft = viewModel.draft();
        var root = new TreeItem<>(draft == null ? "No category" : "Category: " + draft.id());
        root.setExpanded(true);
        if (draft != null) {
            root.getChildren().add(new TreeItem<>("Identification"));
            root.getChildren().add(new TreeItem<>("Anchors (" + (draft.anchors() == null ? 0 : draft.anchors().size()) + ")"));
            root.getChildren().add(new TreeItem<>("Geometry"));
            root.getChildren().add(new TreeItem<>("Fields (" + (draft.fields() == null ? 0 : draft.fields().size()) + ")"));
        }
        configurationTree.setRoot(root);
    }

    private void refreshDetails() {
        var draft = viewModel.draft();
        if (draft == null) {
            details.setText("Create or open a category configuration.");
            return;
        }
        details.setText("id=" + draft.id()
            + "\ndisplayName=" + draft.displayName()
            + "\nversion=" + draft.version()
            + "\ndirty=" + viewModel.session().dirty()
            + "\nreferenceDocument=" + viewModel.session().referenceDocument());
    }

    private void runUiSafe(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void showError(Throwable error) {
        var cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null ? error.getCause() : error;
        status.setText("Error: " + cause.getMessage());
        var alert = new Alert(Alert.AlertType.ERROR, cause.getMessage(), ButtonType.OK);
        alert.setHeaderText("Operation failed");
        alert.showAndWait();
    }

    private static final class PaneOverlay extends javafx.scene.layout.Pane {
        private final ImageView imageView;
        private ScaledCoordinateMapper mapper = new ScaledCoordinateMapper(1, 0, 0);

        PaneOverlay(ImageView imageView) {
            this.imageView = imageView;
            getChildren().add(imageView);
            setPadding(new Insets(12));
            imageView.setPreserveRatio(true);
            setOnMouseClicked(event -> {
                var point = mapper.screenToImage(new ViewerPoint(event.getX(), event.getY()));
                setUserData(point);
            });
        }

        ScaledCoordinateMapper mapper() {
            return mapper;
        }

        void setMapper(ScaledCoordinateMapper mapper) {
            this.mapper = mapper;
        }

        void addOverlay(Rectangle rectangle) {
            getChildren().add(rectangle);
        }

        void clearOverlay() {
            getChildren().removeIf(node -> node != imageView);
        }
    }
}
