package pl.sk.ocr.configurator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static pl.sk.ocr.configurator.ui.FormControls.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pl.sk.ocr.config.dto.ConditionGroupDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.GeometryDto;
import pl.sk.ocr.config.dto.GeometryStrategyDto;
import pl.sk.ocr.config.dto.ReferenceFeatureDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.ConfiguratorServices;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.app.RunPageOcrUseCase;
import pl.sk.ocr.configurator.properties.AnchorPropertiesPanel;
import pl.sk.ocr.configurator.properties.GeometryPropertiesPanel;
import pl.sk.ocr.configurator.properties.IdentificationPropertiesPanel;
import pl.sk.ocr.configurator.properties.IdentificationPropertiesPanel.Selection;
import pl.sk.ocr.configurator.properties.IdentificationPropertiesPanel.SelectionType;
import pl.sk.ocr.configurator.viewer.ScaledCoordinateMapper;
import pl.sk.ocr.configurator.viewer.ViewerPoint;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
import pl.sk.ocr.domain.identifier.PageNumber;

public final class ConfiguratorApplication extends Application {
    private static final String PAGE_TYPE_SINGLE = "SINGLE";
    private static final String PAGE_TYPE_RANGE = "RANGE";
    private static final String PAGE_TYPE_LIST = "LIST";
    private static final String PAGE_TYPE_ALL = "ALL";
    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 5.0;

    private ConfiguratorServices services;
    private CategoryEditorViewModel viewModel;
    private final TreeView<ConfigurationTreeNode> configurationTree = new TreeView<>();
    private final ImageView pageImage = new ImageView();
    private final PaneOverlay viewer = new PaneOverlay(pageImage);
    private final ScrollPane documentScroll = new ScrollPane(viewer);
    private final VBox detailsPanel = new VBox(8);
    private final ScrollPane detailsScroll = new ScrollPane(detailsPanel);
    private final TextField categoryId = new TextField();
    private final TextField categoryDisplayName = new TextField();
    private final TextArea categoryDescription = new TextArea();
    private final TextField categoryVersion = new TextField();
    private final ToggleGroup pageType = new ToggleGroup();
    private final RadioButton pageTypeSingle = new RadioButton(PAGE_TYPE_SINGLE);
    private final RadioButton pageTypeRange = new RadioButton(PAGE_TYPE_RANGE);
    private final RadioButton pageTypeList = new RadioButton(PAGE_TYPE_LIST);
    private final RadioButton pageTypeAll = new RadioButton(PAGE_TYPE_ALL);
    private final HBox pageTypeControls = new HBox(8, pageTypeSingle, pageTypeRange, pageTypeList, pageTypeAll);
    private final TextField pageNumber = new TextField();
    private final TextField pageFrom = new TextField();
    private final TextField pageTo = new TextField();
    private final TextField pageList = new TextField();
    private final VBox pageNumberField = new VBox();
    private final VBox pageFromField = new VBox();
    private final VBox pageToField = new VBox();
    private final VBox pageListField = new VBox();
    private final TextField ocrLanguage = new TextField();
    private final TextField ocrDatapath = new TextField();
    private final Label detailsInfo = new Label();
    private final Label fieldsCount = new Label();
    private final TextField fieldRegionX = new TextField();
    private final TextField fieldRegionY = new TextField();
    private final TextField fieldRegionWidth = new TextField();
    private final TextField fieldRegionHeight = new TextField();
    private final Button drawFieldRegion = new Button();
    private final ListView<String> validationList = new ListView<>();
    private final Label status = new Label("Ready");
    private final Label pageLabel = new Label("Page 0/0");
    private final Button previousPage = compactButton("<", "Previous Page", () -> changePage(-1));
    private final Button nextPage = compactButton(">", "Next Page", () -> changePage(1));
    private final TextField viewerPageNumber = new TextField("1");
    private final Label viewerPageTotal = new Label("/0");
    private Button selectMode;
    private Button panMode;
    private Button drawRegionMode;
    private double zoom = 1.0;
    private boolean refreshingDetails;
    private ViewerMode viewerMode = ViewerMode.SELECT;
    private RegionEditTarget regionEditTarget;
    private String pendingTreeSelectionId;
    private GeometryPropertiesPanel geometryPropertiesPanel;
    private AnchorPropertiesPanel anchorPropertiesPanel;
    private IdentificationPropertiesPanel identificationPropertiesPanel;

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
        geometryPropertiesPanel = new GeometryPropertiesPanel(viewModel, this::anchors, this::fields, pageImage::getImage, status, detailsInfo, this::refreshAfterDraftEdit);
        anchorPropertiesPanel = new AnchorPropertiesPanel(viewModel, this::anchors, this::selectedAnchorIndex, detailsInfo,
            this::refreshAfterDraftEdit, this::refreshAll, selection -> pendingTreeSelectionId = selection,
            this::activateAnchorSearchRegionDrawing, this::activateAnchorReferenceBoundsDrawing, this::svgIcon);
        identificationPropertiesPanel = new IdentificationPropertiesPanel(viewModel, this::identificationSelection, detailsInfo,
            this::refreshAfterDraftEdit, this::refreshAll, selection -> pendingTreeSelectionId = selection,
            this::activateConditionSearchRegionDrawing, this::svgIcon);
        stage.setTitle("OCR Configurator");
        var scene = new Scene(layout(stage), 1280, 820);
        configureAccelerators(scene, stage);
        stage.setScene(scene);
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
        var runOcr = button("Run OCR", this::runOcr);
        var testCategory = button("Test Category", this::validate);
        var validate = button("Validate", this::validate);
        return new ToolBar(newCategory, openConfig, save, saveAs, new Separator(), openDocument,
            new Separator(), runOcr, testCategory, validate);
    }

    private void configureAccelerators(Scene scene, Stage stage) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> saveCategory(stage, false));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom * 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom * 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom * 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom / 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom / 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN), this::actualSize);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.CONTROL_DOWN), this::actualSize);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), this::fitPage);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::fitWidth);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S), () -> setViewerMode(ViewerMode.SELECT));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.P), () -> setViewerMode(ViewerMode.PAN));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R), () -> {
            ensureRegionEditTargetForSelection();
            if (regionEditTarget != null) {
                setViewerMode(ViewerMode.DRAW_REGION);
            }
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::cancelRegionEdit);
    }

    private SplitPane center() {
        configurationTree.setPrefWidth(280);
        configurationTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            switchToNodePage(selected == null ? null : selected.getValue());
            refreshDetails();
        });
        validationList.setPrefHeight(180);
        configureCategoryDetailsForm();
        configureOtherDetailsForms();
        detailsScroll.setFitToWidth(true);
        detailsScroll.setFitToHeight(false);
        detailsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        detailsPanel.setMaxWidth(Double.MAX_VALUE);
        var right = new VBox(8, new Label("Properties / Details"), detailsScroll, new Label("Validation"), validationList);
        right.setPadding(new Insets(8));
        VBox.setVgrow(detailsScroll, Priority.ALWAYS);
        documentScroll.setFitToWidth(false);
        documentScroll.setFitToHeight(false);
        documentScroll.setPannable(false);
        documentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.addEventFilter(ScrollEvent.SCROLL, this::handleScrollZoom);
        documentScroll.addEventFilter(ZoomEvent.ZOOM, this::handleTouchpadZoom);
        var split = new SplitPane(configurationTree, documentViewer(), right);
        split.setDividerPositions(0.22, 0.72);
        return split;
    }

    private HBox documentViewer() {
        selectMode = iconButton("mode-select.svg", "Select (S)", () -> setViewerMode(ViewerMode.SELECT));
        panMode = iconButton("mode-pan.svg", "Pan (P)", () -> setViewerMode(ViewerMode.PAN));
        drawRegionMode = iconButton("mode-draw-region.svg", "Draw Region (R)", () -> {
            ensureRegionEditTargetForSelection();
            if (regionEditTarget != null) {
                setViewerMode(ViewerMode.DRAW_REGION);
            }
        });
        var zoomToolbar = new VBox(6,
            selectMode,
            panMode,
            drawRegionMode,
            new Separator(),
            iconButton("zoom-in.svg", "Zoom In (Ctrl++)", () -> setZoom(zoom * 1.25)),
            iconButton("zoom-out.svg", "Zoom Out (Ctrl+-)", () -> setZoom(zoom / 1.25)),
            iconButton("zoom-fit.svg", "Fit Page (Ctrl+F)", this::fitPage),
            iconButton("zoom-width.svg", "Fit Width (Ctrl+Shift+W)", this::fitWidth),
            iconButton("zoom-100.svg", "100% (Ctrl+0)", this::actualSize)
        );
        zoomToolbar.setPadding(new Insets(8));
        zoomToolbar.setAlignment(Pos.TOP_CENTER);
        zoomToolbar.setMinWidth(52);
        zoomToolbar.setPrefWidth(52);
        zoomToolbar.setMaxWidth(52);
        zoomToolbar.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #c8cdd4; -fx-border-width: 0 1 0 0;");

        var content = new BorderPane(documentScroll);
        content.setBottom(pageNavigator());
        var pane = new HBox(zoomToolbar, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox.setHgrow(documentScroll, Priority.ALWAYS);
        refreshViewerModeButtons();
        return pane;
    }

    private HBox pageNavigator() {
        viewerPageNumber.setPrefColumnCount(4);
        viewerPageNumber.setMaxWidth(64);
        viewerPageNumber.setTooltip(new Tooltip("Current page. Enter page number and press Enter."));
        viewerPageNumber.setOnAction(event -> goToPageFromInput());
        viewerPageTotal.setMinWidth(36);
        var spacer = new Region();
        var navigator = new HBox(6, spacer, previousPage, viewerPageNumber, viewerPageTotal, nextPage);
        navigator.setAlignment(Pos.CENTER_RIGHT);
        navigator.setPadding(new Insets(6, 8, 6, 8));
        navigator.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #c8cdd4; -fx-border-width: 1 0 0 0;");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        refreshPageStatus();
        return navigator;
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

    private Button iconButton(String iconName, String tooltip, Runnable action) {
        var button = button("", action);
        button.setGraphic(svgIcon(iconName));
        button.setTooltip(new Tooltip(tooltip));
        button.setMinSize(36, 32);
        button.setPrefSize(36, 32);
        button.setMaxSize(36, 32);
        return button;
    }

    private Button compactButton(String text, String tooltip, Runnable action) {
        var button = button(text, action);
        button.setTooltip(new Tooltip(tooltip));
        button.setMinSize(36, 28);
        button.setPrefSize(36, 28);
        button.setMaxSize(36, 28);
        return button;
    }

    private static Spinner<Integer> regionSpinner() {
        var spinner = new Spinner<Integer>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        return spinner;
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
        try (var input = ConfiguratorApplication.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing icon resource: " + resource);
            }
            var svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = Pattern.compile("<path\\s+[^>]*d=\"([^\"]+)\"").matcher(svg);
            if (!matcher.find()) {
                throw new IllegalStateException("Icon has no path data: " + resource);
            }
            return matcher.group(1);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read icon resource: " + resource, e);
        }
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
        commitCurrentDetailsForm();
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

    private void commitCurrentDetailsForm() {
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        switch (selectedNodeType()) {
            case ROOT -> {
                applyCategoryMetadata();
                applyPages();
                applyOcrDefaults();
            }
            case CONDITION -> applySelectedCondition();
            case ANCHOR -> applySelectedAnchor();
            case GEOMETRY, GEOMETRY_STRATEGY -> applyGeometry();
            case FIELD -> applySelectedFieldRegion();
            default -> {
            }
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
        goToPage(Math.max(1, Math.min(pages, viewModel.session().currentPage() + delta)));
    }

    private void goToPageFromInput() {
        var pages = viewModel.session().pageCache().size();
        if (pages == 0) {
            refreshPageStatus();
            return;
        }
        try {
            var requested = Integer.parseInt(viewerPageNumber.getText().trim());
            if (requested < 1 || requested > pages) {
                status.setText("Page must be between 1 and " + pages);
                refreshPageStatus();
                return;
            }
            goToPage(requested);
        } catch (NumberFormatException e) {
            status.setText("Page must be a number");
            refreshPageStatus();
        }
    }

    private void goToPage(int page) {
        viewModel.session().currentPage(page);
        renderPage();
        refreshAll();
    }

    private void setZoom(double value) {
        zoom = boundedZoom(value);
        applyImageSize();
        updateViewerMapper();
        renderOcrOverlay();
        refreshAll();
    }

    private void fitPage() {
        var image = pageImage.getImage();
        if (image == null || documentScroll.getViewportBounds().isEmpty()) {
            setZoom(1.0);
            return;
        }
        var horizontalPadding = viewer.padding().getLeft() + viewer.padding().getRight();
        var verticalPadding = viewer.padding().getTop() + viewer.padding().getBottom();
        var viewport = documentScroll.getViewportBounds();
        var widthZoom = boundedZoom((viewport.getWidth() - horizontalPadding) / image.getWidth());
        var heightZoom = boundedZoom((viewport.getHeight() - verticalPadding) / image.getHeight());
        setZoom(Math.min(widthZoom, heightZoom));
    }

    private void fitWidth() {
        var image = pageImage.getImage();
        if (image == null || documentScroll.getViewportBounds().isEmpty()) {
            setZoom(1.0);
            return;
        }
        var horizontalPadding = viewer.padding().getLeft() + viewer.padding().getRight();
        var viewport = documentScroll.getViewportBounds();
        setZoom((viewport.getWidth() - horizontalPadding) / image.getWidth());
    }

    private void actualSize() {
        setZoom(1.0);
    }

    private double boundedZoom(double value) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }

    private void handleScrollZoom(ScrollEvent event) {
        if (!event.isControlDown()) {
            return;
        }
        var factor = event.getDeltaY() > 0 ? 1.10 : 1.0 / 1.10;
        setZoomAround(zoom * factor, event.getSceneX(), event.getSceneY());
        event.consume();
    }

    private void handleTouchpadZoom(ZoomEvent event) {
        setZoomAround(zoom * event.getZoomFactor(), event.getSceneX(), event.getSceneY());
        event.consume();
    }

    private void setZoomAround(double value, double sceneX, double sceneY) {
        updateViewerMapper();
        var viewport = documentScroll.getViewportBounds();
        var mouseInViewport = documentScroll.sceneToLocal(sceneX, sceneY);
        var mouseX = Math.max(0, Math.min(viewport.getWidth(), mouseInViewport.getX()));
        var mouseY = Math.max(0, Math.min(viewport.getHeight(), mouseInViewport.getY()));
        var mouseInViewer = viewer.sceneToLocal(sceneX, sceneY);
        var imagePoint = viewer.mapper().screenToImage(new ViewerPoint(mouseInViewer.getX(), mouseInViewer.getY()));

        setZoom(value);
        documentScroll.layout();
        viewer.layout();

        var currentViewport = documentScroll.getViewportBounds();
        var targetInViewer = viewer.mapper().imageToScreen(imagePoint);
        var newScrollX = targetInViewer.x() - mouseX;
        var newScrollY = targetInViewer.y() - mouseY;
        documentScroll.setHvalue(scrollValue(newScrollX, viewer.getBoundsInLocal().getWidth(), currentViewport.getWidth()));
        documentScroll.setVvalue(scrollValue(newScrollY, viewer.getBoundsInLocal().getHeight(), currentViewport.getHeight()));
        Platform.runLater(() -> {
            documentScroll.setHvalue(scrollValue(newScrollX, viewer.getBoundsInLocal().getWidth(), documentScroll.getViewportBounds().getWidth()));
            documentScroll.setVvalue(scrollValue(newScrollY, viewer.getBoundsInLocal().getHeight(), documentScroll.getViewportBounds().getHeight()));
        });
    }

    private double scrollValue(double offset, double contentSize, double viewportSize) {
        var scrollable = Math.max(0, contentSize - viewportSize);
        if (scrollable == 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, offset / scrollable));
    }

    private void renderPage() {
        var page = viewModel.session().pageCache().get(new PageNumber(viewModel.session().currentPage()));
        if (page != null) {
            pageImage.setImage(SwingFXUtils.toFXImage(page.asBufferedImage(), null));
            Platform.runLater(this::fitPage);
        }
        renderOcrOverlay();
    }

    private void applyImageSize() {
        var image = pageImage.getImage();
        if (image == null) {
            return;
        }
        var width = image.getWidth() * zoom;
        var height = image.getHeight() * zoom;
        pageImage.setFitWidth(width);
        pageImage.setFitHeight(height);
        viewer.setContentSize(width, height);
    }

    private void updateViewerMapper() {
        var padding = viewer.padding();
        viewer.setMapper(new ScaledCoordinateMapper(zoom, padding.getLeft(), padding.getTop()));
    }

    private void renderOcrOverlay() {
        viewer.clearOverlay();
        renderSelectedConditionRegionOverlay();
        renderSelectedAnchorOverlay();
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
                detailsInfo.setText("OCR word: " + word.text() + " | confidence=" + word.confidence().value()
                    + " | bounds=" + word.boundingBox().region());
                event.consume();
            });
            viewer.addOverlay(rectangle);
        }
    }

    private void renderSelectedConditionRegionOverlay() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.CONDITION || !identificationPropertiesPanel.hasValidConditionSearchRegion()) {
            viewer.clearEditableRegions();
            return;
        }
        var condition = selectedCondition();
        if (condition == null || condition.page() != null && condition.page() != viewModel.session().currentPage()) {
            viewer.clearEditableRegions();
            return;
        }
        var screen = viewer.mapper().imageToScreen(toDomainRegion(identificationPropertiesPanel.conditionSearchRegion()));
        var rectangle = new Rectangle(screen.x(), screen.y(), screen.width(), screen.height());
        rectangle.setFill(Color.color(0.12, 0.48, 0.93, 0.12));
        rectangle.setStroke(Color.web("#1f7aec"));
        rectangle.setStrokeWidth(2.0);
        viewer.addOverlay(rectangle);
        viewer.editableRegion(rectangle, RegionTargetType.CONDITION_SEARCH_REGION);
    }

    private void renderSelectedAnchorOverlay() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.ANCHOR) {
            return;
        }
        var anchor = selectedAnchor();
        if (anchor == null || anchor.page() != null && anchor.page() != viewModel.session().currentPage()) {
            return;
        }
        if (anchor.searchRegion() != null) {
            var rectangle = regionRectangle(anchor.searchRegion(), Color.color(0.93, 0.56, 0.12, 0.10), "#d97706", 1.5);
            viewer.addOverlay(rectangle);
            viewer.editableRegion(rectangle, RegionTargetType.ANCHOR_SEARCH_REGION);
        }
        if (anchor.referenceFeature() != null && anchor.referenceFeature().bounds() != null) {
            var rectangle = regionRectangle(anchor.referenceFeature().bounds(), Color.color(0.12, 0.65, 0.38, 0.12), "#059669", 2.0);
            viewer.addOverlay(rectangle);
            viewer.editableRegion(rectangle, RegionTargetType.ANCHOR_REFERENCE_BOUNDS);
        }
    }

    private Rectangle regionRectangle(RegionDto region, Color fill, String stroke, double strokeWidth) {
        var screen = viewer.mapper().imageToScreen(toDomainRegion(region));
        var rectangle = new Rectangle(screen.x(), screen.y(), screen.width(), screen.height());
        rectangle.setFill(fill);
        rectangle.setStroke(Color.web(stroke));
        rectangle.setStrokeWidth(strokeWidth);
        return rectangle;
    }

    private void refreshAll() {
        refreshTree();
        refreshDetails();
        status.setText(viewModel.status());
        refreshPageStatus();
        validationList.getItems().setAll(viewModel.validationProblems().stream()
            .map(problem -> problem.code() + " " + problem.path() + " " + problem.message())
            .toList());
    }

    private void refreshTree() {
        var draft = viewModel.draft();
        var selectedId = pendingTreeSelectionId != null ? pendingTreeSelectionId : selectedTreeNodeId();
        pendingTreeSelectionId = null;
        var expandedNodeIds = expandedTreeNodeIds(configurationTree.getRoot());
        var root = new TreeItem<>(draft == null
            ? ConfigurationTreeNode.root("No category")
            : ConfigurationTreeNode.root("Category: " + draft.id()));
        root.setExpanded(true);
        if (draft != null) {
            root.getChildren().add(identificationTree(draft.identification()));
            root.getChildren().add(anchorsTree(draft.anchors()));
            root.getChildren().add(geometryTree(draft.geometry()));
            root.getChildren().add(fieldsTree(draft.fields()));
        }
        configurationTree.setRoot(root);
        restoreExpandedTreeNodes(root, expandedNodeIds);
        selectTreeNodeAndExpandParents(root, selectedId);
    }

    private TreeItem<ConfigurationTreeNode> identificationTree(pl.sk.ocr.config.dto.IdentificationDto identification) {
        var groups = identification == null || identification.groups() == null ? List.<pl.sk.ocr.config.dto.ConditionGroupDto>of() : identification.groups();
        var root = new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.IDENTIFICATION, "Identification (" + groups.size() + ")", "identification", -1, -1, null));
        root.setExpanded(true);
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            var group = groups.get(groupIndex);
            var conditions = group.conditions() == null ? List.<pl.sk.ocr.config.dto.ConditionDto>of() : group.conditions();
            var groupItem = new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.IDENTIFICATION_GROUP, "Group " + (groupIndex + 1) + " (" + conditions.size() + ")", "identification.group." + groupIndex, groupIndex, -1, null));
            for (int conditionIndex = 0; conditionIndex < conditions.size(); conditionIndex++) {
                var condition = conditions.get(conditionIndex);
                groupItem.getChildren().add(new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.CONDITION,
                    "Condition " + (conditionIndex + 1) + ": " + labelOrDefault(condition.type(), "condition"),
                    "identification.group." + groupIndex + ".condition." + conditionIndex,
                    groupIndex,
                    conditionIndex,
                    condition.page())));
            }
            root.getChildren().add(groupItem);
        }
        return root;
    }

    private TreeItem<ConfigurationTreeNode> anchorsTree(List<pl.sk.ocr.config.dto.AnchorDto> anchors) {
        var anchorList = anchors == null ? List.<pl.sk.ocr.config.dto.AnchorDto>of() : anchors;
        var root = new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.ANCHORS, "Anchors (" + anchorList.size() + ")", "anchors", -1, -1, null));
        root.setExpanded(true);
        for (int i = 0; i < anchorList.size(); i++) {
            var anchor = anchorList.get(i);
            root.getChildren().add(new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.ANCHOR,
                labelOrDefault(anchor.id(), "Anchor " + (i + 1)),
                "anchor." + i,
                i,
                -1,
                anchor.page())));
        }
        return root;
    }

    private TreeItem<ConfigurationTreeNode> geometryTree(pl.sk.ocr.config.dto.GeometryDto geometry) {
        var root = new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.GEOMETRY, "Geometry", "geometry", -1, -1, null));
        if (geometry != null && geometry.strategy() != null) {
            root.getChildren().add(new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.GEOMETRY_STRATEGY,
                "Strategy: " + labelOrDefault(geometry.strategy().type(), "NONE"),
                "geometry.strategy",
                -1,
                -1,
                null)));
        }
        return root;
    }

    private TreeItem<ConfigurationTreeNode> fieldsTree(List<pl.sk.ocr.config.dto.FieldDto> fields) {
        var fieldList = fields == null ? List.<pl.sk.ocr.config.dto.FieldDto>of() : fields;
        var root = new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.FIELDS, "Fields (" + fieldList.size() + ")", "fields", -1, -1, null));
        root.setExpanded(true);
        for (int i = 0; i < fieldList.size(); i++) {
            var field = fieldList.get(i);
            var fieldItem = new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.FIELD,
                labelOrDefault(field.id(), "Field " + (i + 1)),
                "field." + i,
                i,
                -1,
                field.page()));
            fieldItem.getChildren().add(new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.FIELD_OCR, "OCR", "field." + i + ".ocr", i, -1, field.page())));
            fieldItem.getChildren().add(new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.FIELD_OUTPUT, "Output", "field." + i + ".output", i, -1, field.page())));
            fieldItem.getChildren().add(pipelineTree(TreeNodeType.FIELD_IMAGE_PROCESSORS, "Image Processors", "field." + i + ".imageProcessors", i, field.page(), field.imageProcessors()));
            fieldItem.getChildren().add(pipelineTree(TreeNodeType.FIELD_TRANSFORMERS, "Transformers", "field." + i + ".transformers", i, field.page(), field.transformers()));
            fieldItem.getChildren().add(pipelineTree(TreeNodeType.FIELD_VALIDATORS, "Validators", "field." + i + ".validators", i, field.page(), field.validators()));
            root.getChildren().add(fieldItem);
        }
        return root;
    }

    private TreeItem<ConfigurationTreeNode> pipelineTree(TreeNodeType type, String label, String id, int fieldIndex, Integer page,
                                                         List<pl.sk.ocr.config.dto.ExtensionRefDto> steps) {
        var stepList = steps == null ? List.<pl.sk.ocr.config.dto.ExtensionRefDto>of() : steps;
        var root = new TreeItem<>(new ConfigurationTreeNode(type, label + " (" + stepList.size() + ")", id, fieldIndex, -1, page));
        for (int i = 0; i < stepList.size(); i++) {
            var step = stepList.get(i);
            root.getChildren().add(new TreeItem<>(new ConfigurationTreeNode(TreeNodeType.PIPELINE_STEP,
                labelOrDefault(step.id(), "Step " + (i + 1)),
                id + "." + i,
                fieldIndex,
                i,
                page)));
        }
        return root;
    }

    private void refreshDetails() {
        var draft = viewModel.draft();
        refreshingDetails = true;
        if (draft == null) {
            detailsPanel.setDisable(true);
            detailsInfo.setText("Create or open a category configuration.");
            clearCategoryDetailsForm();
            detailsPanel.getChildren().setAll(emptyDetailsForm());
            refreshingDetails = false;
            return;
        }
        detailsPanel.setDisable(false);
        categoryId.setText(nullToEmpty(draft.id()));
        categoryDisplayName.setText(nullToEmpty(draft.displayName()));
        categoryDescription.setText(nullToEmpty(draft.description()));
        categoryVersion.setText(nullToEmpty(draft.version()));
        var pages = draft.pages();
        selectPageType(pages == null || pages.type() == null ? PAGE_TYPE_SINGLE : pages.type());
        pageNumber.setText(pages == null || pages.page() == null ? "" : pages.page().toString());
        pageFrom.setText(pages == null || pages.from() == null ? "" : pages.from().toString());
        pageTo.setText(pages == null || pages.to() == null ? "" : pages.to().toString());
        pageList.setText(pages == null || pages.pages() == null ? "" : pages.pages().stream().map(String::valueOf).toList().toString().replace("[", "").replace("]", ""));
        var ocr = draft.ocr();
        ocrLanguage.setText(ocr == null ? "" : nullToEmpty(ocr.language()));
        ocrDatapath.setText(ocr == null ? "" : nullToEmpty(ocr.datapath()));
        identificationPropertiesPanel.refresh();
        anchorPropertiesPanel.refresh();
        fieldsCount.setText(String.valueOf(draft.fields() == null ? 0 : draft.fields().size()));
        geometryPropertiesPanel.refresh();
        var selectedField = selectedField();
        var selectedRegion = selectedField == null ? null : selectedField.region();
        fieldRegionX.setText(selectedRegion == null ? "" : formatRegionNumber(selectedRegion.x()));
        fieldRegionY.setText(selectedRegion == null ? "" : formatRegionNumber(selectedRegion.y()));
        fieldRegionWidth.setText(selectedRegion == null ? "" : formatRegionNumber(selectedRegion.width()));
        fieldRegionHeight.setText(selectedRegion == null ? "" : formatRegionNumber(selectedRegion.height()));
        detailsInfo.setText("Dirty=" + viewModel.session().dirty()
            + " | Reference document=" + (viewModel.session().referenceDocument() == null ? "" : viewModel.session().referenceDocument()));
        detailsPanel.getChildren().setAll(detailsFormForSelection());
        renderOcrOverlay();
        refreshingDetails = false;
    }

    private void configureCategoryDetailsForm() {
        pageTypeSingle.setToggleGroup(pageType);
        pageTypeRange.setToggleGroup(pageType);
        pageTypeList.setToggleGroup(pageType);
        pageTypeAll.setToggleGroup(pageType);
        pageTypeSingle.setUserData(PAGE_TYPE_SINGLE);
        pageTypeRange.setUserData(PAGE_TYPE_RANGE);
        pageTypeList.setUserData(PAGE_TYPE_LIST);
        pageTypeAll.setUserData(PAGE_TYPE_ALL);
        pageTypeSingle.setStyle("-fx-text-fill: #111827;");
        pageTypeRange.setStyle("-fx-text-fill: #111827;");
        pageTypeList.setStyle("-fx-text-fill: #111827;");
        pageTypeAll.setStyle("-fx-text-fill: #111827;");
        pageTypeSingle.setSelected(true);
        categoryDescription.setPrefRowCount(3);
        categoryDescription.setWrapText(true);
        detailsInfo.setWrapText(true);
        detailsInfo.setStyle("-fx-text-fill: #111827;");
        fieldsCount.setStyle("-fx-text-fill: #111827;");
        installTooltip(categoryId, "Unique category identifier written to category JSON.");
        installTooltip(categoryDisplayName, "Human-readable category name shown in UI and diagnostics.");
        installTooltip(categoryDescription, "Optional category description.");
        installTooltip(categoryVersion, "Category configuration version.");
        installTooltip(pageTypeSingle, "Use a single page.");
        installTooltip(pageTypeRange, "Use a continuous page range.");
        installTooltip(pageTypeList, "Use explicit comma-separated page numbers.");
        installTooltip(pageTypeAll, "Use all pages.");
        installTooltip(pageNumber, "Single page number for SINGLE page policy.");
        installTooltip(pageFrom, "First page for RANGE page policy.");
        installTooltip(pageTo, "Last page for RANGE page policy.");
        installTooltip(pageList, "Comma-separated page numbers for LIST page policy.");
        installTooltip(ocrLanguage, "Default OCR language for fields that do not override OCR settings.");
        installTooltip(ocrDatapath, "Optional Tesseract datapath override.");

        addDraftListener(categoryId, this::applyCategoryMetadata);
        addDraftListener(categoryDisplayName, this::applyCategoryMetadata);
        addDraftListener(categoryDescription, this::applyCategoryMetadata);
        addDraftListener(categoryVersion, this::applyCategoryMetadata);
        pageType.selectedToggleProperty().addListener((obs, old, value) -> {
            updatePagePolicyFieldsVisibility();
            applyPages();
        });
        addDraftListener(pageNumber, this::applyPages);
        addDraftListener(pageFrom, this::applyPages);
        addDraftListener(pageTo, this::applyPages);
        addDraftListener(pageList, this::applyPages);
        addDraftListener(ocrLanguage, this::applyOcrDefaults);
        addDraftListener(ocrDatapath, this::applyOcrDefaults);

        updatePagePolicyFieldsVisibility();
    }

    private void configureOtherDetailsForms() {
        installTooltip(fieldsCount, "Number of fields configured for extraction.");
        installTooltip(fieldRegionX, "Field region X coordinate in image/reference coordinates.");
        installTooltip(fieldRegionY, "Field region Y coordinate in image/reference coordinates.");
        installTooltip(fieldRegionWidth, "Field region width in image/reference coordinates.");
        installTooltip(fieldRegionHeight, "Field region height in image/reference coordinates.");
        drawFieldRegion.setGraphic(svgIcon("mode-draw-region.svg"));
        drawFieldRegion.setTooltip(new Tooltip("Draw field region on document preview."));
        drawFieldRegion.setMinSize(36, 32);
        drawFieldRegion.setPrefSize(36, 32);
        drawFieldRegion.setMaxSize(36, 32);

        addDraftListener(fieldRegionX, this::applySelectedFieldRegion);
        addDraftListener(fieldRegionY, this::applySelectedFieldRegion);
        addDraftListener(fieldRegionWidth, this::applySelectedFieldRegion);
        addDraftListener(fieldRegionHeight, this::applySelectedFieldRegion);
        drawFieldRegion.setOnAction(event -> activateFieldRegionDrawing());
    }

    private javafx.scene.Node detailsFormForSelection() {
        return switch (selectedNodeType()) {
            case IDENTIFICATION, IDENTIFICATION_GROUP, CONDITION -> identificationDetailsForm();
            case ANCHORS, ANCHOR -> anchorsDetailsForm();
            case GEOMETRY, GEOMETRY_STRATEGY -> geometryDetailsForm();
            case FIELDS, FIELD, FIELD_OCR, FIELD_OUTPUT, FIELD_IMAGE_PROCESSORS, FIELD_TRANSFORMERS, FIELD_VALIDATORS, PIPELINE_STEP -> fieldsDetailsForm();
            case ROOT -> categoryDetailsForm();
        };
    }

    private VBox categoryDetailsForm() {
        var categorySection = section("Category");
        addFormRow(categorySection, "ID", categoryId);
        addFormRow(categorySection, "Display Name", categoryDisplayName);
        addFormRow(categorySection, "Description", categoryDescription);
        addFormRow(categorySection, "Version", categoryVersion);

        var pagePolicySection = section("Page Policy");
        detachFromParent(pageTypeControls);
        pagePolicySection.getChildren().add(pageTypeControls);
        addFormRow(pagePolicySection, "Page", pageNumber, pageNumberField);
        addFormRow(pagePolicySection, "From", pageFrom, pageFromField);
        addFormRow(pagePolicySection, "To", pageTo, pageToField);
        addFormRow(pagePolicySection, "Pages", pageList, pageListField);

        var ocrSection = section("OCR");
        addFormRow(ocrSection, "Language", ocrLanguage);
        addFormRow(ocrSection, "Datapath", ocrDatapath);
        return new VBox(10, categorySection, pagePolicySection, ocrSection, detailsInfo);
    }

    private javafx.scene.Node identificationDetailsForm() {
        return identificationPropertiesPanel.view();
    }

    private javafx.scene.Node anchorsDetailsForm() {
        return anchorPropertiesPanel.view();
    }

    private javafx.scene.Node geometryDetailsForm() {
        return geometryPropertiesPanel.view();
    }

    private VBox fieldsDetailsForm() {
        var section = section("Fields");
        var selected = selectedTreeNode();
        if (selected != null && selected.type() == TreeNodeType.FIELD) {
            addFormRow(section, "Region X", fieldRegionX);
            addFormRow(section, "Region Y", fieldRegionY);
            addFormRow(section, "Region Width", fieldRegionWidth);
            addFormRow(section, "Region Height", fieldRegionHeight);
            detachFromParent(drawFieldRegion);
            section.getChildren().add(drawFieldRegion);
        } else {
            addFormRow(section, "Fields", fieldsCount);
        }
        return new VBox(10, section, detailsInfo);
    }

    private VBox emptyDetailsForm() {
        var section = section("Category");
        var message = new Label("Create or open a category configuration.");
        message.setWrapText(true);
        installTooltip(message, "No category draft is currently open.");
        section.getChildren().add(message);
        return new VBox(10, section, detailsInfo);
    }

    private void updatePagePolicyFieldsVisibility() {
        var selected = selectedPageType();
        setVisibleManaged(pageNumberField, PAGE_TYPE_SINGLE.equals(selected));
        setVisibleManaged(pageFromField, PAGE_TYPE_RANGE.equals(selected));
        setVisibleManaged(pageToField, PAGE_TYPE_RANGE.equals(selected));
        setVisibleManaged(pageListField, PAGE_TYPE_LIST.equals(selected));
    }

    private void applyCategoryMetadata() {
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        runUiSafe(() -> {
            viewModel.updateCategoryMetadata(categoryId.getText(), categoryDisplayName.getText(),
                categoryDescription.getText(), categoryVersion.getText());
            refreshAfterDraftEdit();
        });
    }

    private void applyPages() {
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        runUiSafe(() -> {
            viewModel.updatePages(new pl.sk.ocr.config.dto.PageSelectionDto(
                selectedPageType(),
                parseInteger(pageNumber.getText()),
                parseInteger(pageFrom.getText()),
                parseInteger(pageTo.getText()),
                parseIntegerList(pageList.getText())
            ));
            refreshAfterDraftEdit();
            renderOcrOverlay();
        });
    }

    private void applyOcrDefaults() {
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        runUiSafe(() -> {
            viewModel.updateOcr(new pl.sk.ocr.config.dto.OcrSettingsDto(blankToNull(ocrLanguage.getText()), blankToNull(ocrDatapath.getText())));
            refreshAfterDraftEdit();
        });
    }

    private void applyGeometry() {
        geometryPropertiesPanel.commit();
    }

    private void applySelectedFieldRegion() {
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.FIELD) {
            return;
        }
        runUiSafe(() -> {
            viewModel.updateFieldRegion(selected.index(), new RegionDto(
                parseDouble(fieldRegionX.getText()),
                parseDouble(fieldRegionY.getText()),
                parseDouble(fieldRegionWidth.getText()),
                parseDouble(fieldRegionHeight.getText())
            ));
            refreshAfterDraftEdit();
        });
    }

    private void applySelectedCondition() {
        identificationPropertiesPanel.commit();
    }

    private void applySelectedAnchor() {
        anchorPropertiesPanel.commit();
    }

    private void activateFieldRegionDrawing() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.FIELD) {
            status.setText("Select a field to draw its region");
            return;
        }
        regionEditTarget = new RegionEditTarget(RegionTargetType.FIELD_REGION, selected.index(), -1);
        setViewerMode(ViewerMode.DRAW_REGION);
    }

    private void activateConditionSearchRegionDrawing() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.CONDITION) {
            status.setText("Select a condition to draw its search region");
            return;
        }
        regionEditTarget = new RegionEditTarget(RegionTargetType.CONDITION_SEARCH_REGION, selected.index(), selected.childIndex());
        setViewerMode(ViewerMode.DRAW_REGION);
    }

    private void activateAnchorSearchRegionDrawing() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.ANCHOR) {
            status.setText("Select an anchor to draw its search region");
            return;
        }
        regionEditTarget = new RegionEditTarget(RegionTargetType.ANCHOR_SEARCH_REGION, selected.index(), -1);
        setViewerMode(ViewerMode.DRAW_REGION);
    }

    private void activateAnchorReferenceBoundsDrawing() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.ANCHOR) {
            status.setText("Select an anchor to draw its reference feature");
            return;
        }
        regionEditTarget = new RegionEditTarget(RegionTargetType.ANCHOR_REFERENCE_BOUNDS, selected.index(), -1);
        setViewerMode(ViewerMode.DRAW_REGION);
    }

    private void ensureRegionEditTargetForSelection() {
        var selected = selectedTreeNode();
        if (selected == null) {
            return;
        }
        if (selected.type() == TreeNodeType.CONDITION) {
            regionEditTarget = new RegionEditTarget(RegionTargetType.CONDITION_SEARCH_REGION, selected.index(), selected.childIndex());
        } else if (selected.type() == TreeNodeType.ANCHOR) {
            regionEditTarget = new RegionEditTarget(RegionTargetType.ANCHOR_REFERENCE_BOUNDS, selected.index(), -1);
        } else if (selected.type() == TreeNodeType.FIELD) {
            regionEditTarget = new RegionEditTarget(RegionTargetType.FIELD_REGION, selected.index(), -1);
        }
    }

    private void updateSelectedConditionRegionFromViewer(RegionDto region, boolean commit) {
        identificationPropertiesPanel.updateConditionSearchRegionFromViewer(region, commit);
    }

    private void updateSelectedAnchorReferenceBoundsFromViewer(RegionDto region, boolean commit) {
        anchorPropertiesPanel.updateReferenceBoundsFromViewer(region, commit);
    }

    private void updateSelectedAnchorSearchRegionFromViewer(RegionDto region, boolean commit) {
        anchorPropertiesPanel.updateSearchRegionFromViewer(region, commit);
    }

    private void updateSelectedEditableRegionFromViewer(RegionTargetType targetType, RegionDto region, boolean commit) {
        var selected = selectedTreeNode();
        if (selected == null) {
            return;
        }
        if (targetType == RegionTargetType.CONDITION_SEARCH_REGION) {
            updateSelectedConditionRegionFromViewer(region, commit);
        } else if (targetType == RegionTargetType.ANCHOR_SEARCH_REGION) {
            updateSelectedAnchorSearchRegionFromViewer(region, commit);
        } else if (targetType == RegionTargetType.ANCHOR_REFERENCE_BOUNDS) {
            updateSelectedAnchorReferenceBoundsFromViewer(region, commit);
        }
    }

    private void applyDrawnRegion(RegionDto region) {
        if (regionEditTarget == null) {
            return;
        }
        runUiSafe(() -> {
            if (regionEditTarget.type() == RegionTargetType.FIELD_REGION) {
                viewModel.updateFieldRegion(regionEditTarget.index(), region);
            } else if (regionEditTarget.type() == RegionTargetType.CONDITION_SEARCH_REGION) {
                identificationPropertiesPanel.replaceConditionSearchRegion(regionEditTarget.index(), regionEditTarget.childIndex(), region);
            } else if (regionEditTarget.type() == RegionTargetType.ANCHOR_SEARCH_REGION) {
                anchorPropertiesPanel.replaceSearchRegion(regionEditTarget.index(), region);
            } else if (regionEditTarget.type() == RegionTargetType.ANCHOR_REFERENCE_BOUNDS) {
                anchorPropertiesPanel.replaceReferenceBounds(regionEditTarget.index(), region);
            }
            regionEditTarget = null;
            setViewerMode(ViewerMode.SELECT);
            refreshAll();
        });
    }

    private void cancelRegionEdit() {
        regionEditTarget = null;
        setViewerMode(ViewerMode.SELECT);
        status.setText("Region drawing cancelled");
    }

    private void setViewerMode(ViewerMode mode) {
        if (mode == ViewerMode.DRAW_REGION && regionEditTarget == null) {
            status.setText("Select a region property first");
            return;
        }
        viewerMode = mode;
        documentScroll.setPannable(mode == ViewerMode.PAN);
        viewer.mode(mode);
        refreshViewerModeButtons();
        status.setText("Viewer mode: " + mode.label());
    }

    private void refreshViewerModeButtons() {
        styleModeButton(selectMode, viewerMode == ViewerMode.SELECT);
        styleModeButton(panMode, viewerMode == ViewerMode.PAN);
        styleModeButton(drawRegionMode, viewerMode == ViewerMode.DRAW_REGION);
    }

    private void styleModeButton(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setStyle(selected
            ? "-fx-background-color: #dbeafe; -fx-border-color: #1f7aec; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-background-radius: 4;"
            : "");
    }

    private void refreshAfterDraftEdit() {
        refreshTree();
        status.setText(viewModel.status());
        validationList.getItems().setAll(viewModel.validationProblems().stream()
            .map(problem -> problem.code() + " " + problem.path() + " " + problem.message())
            .toList());
        detailsInfo.setText("Dirty=" + viewModel.session().dirty()
            + " | Reference document=" + (viewModel.session().referenceDocument() == null ? "" : viewModel.session().referenceDocument()));
    }

    private void clearCategoryDetailsForm() {
        categoryId.clear();
        categoryDisplayName.clear();
        categoryDescription.clear();
        categoryVersion.clear();
        selectPageType(PAGE_TYPE_SINGLE);
        pageNumber.clear();
        pageFrom.clear();
        pageTo.clear();
        pageList.clear();
        ocrLanguage.clear();
        ocrDatapath.clear();
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private double parseDouble(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return 0.0;
        }
        return Double.parseDouble(text);
    }

    private String formatRegionNumber(double value) {
        return String.valueOf(Math.round(value));
    }

    private RegionDto roundedRegion(double x, double y, double width, double height) {
        return new RegionDto(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }

    private pl.sk.ocr.domain.geometry.Region toDomainRegion(RegionDto region) {
        return new pl.sk.ocr.domain.geometry.Region(region.x(), region.y(), region.width(), region.height());
    }

    private RegionDto spinnerRegion(Spinner<Integer> x, Spinner<Integer> y, Spinner<Integer> width, Spinner<Integer> height) {
        if (blankToNull(x.getEditor().getText()) == null
            || blankToNull(y.getEditor().getText()) == null
            || blankToNull(width.getEditor().getText()) == null
            || blankToNull(height.getEditor().getText()) == null) {
            return null;
        }
        return new RegionDto(
            parseInteger(x.getEditor().getText()),
            parseInteger(y.getEditor().getText()),
            parseInteger(width.getEditor().getText()),
            parseInteger(height.getEditor().getText())
        );
    }

    private void setRegionSpinnerText(Spinner<Integer> spinner, String value) {
        spinner.getEditor().setText(value);
        if (blankToNull(value) != null) {
            spinner.getValueFactory().setValue(Integer.parseInt(value));
        }
    }

    private ExtensionRefDto extensionRef(String id) {
        var normalized = blankToNull(id);
        return normalized == null ? null : new ExtensionRefDto(normalized, Map.of());
    }

    private java.util.List<Integer> parseIntegerList(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return null;
        }
        return java.util.Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(Integer::parseInt)
            .toList();
    }

    private java.util.List<String> parseStringList(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String selectedPageType() {
        var selected = pageType.getSelectedToggle();
        return selected == null ? PAGE_TYPE_SINGLE : selected.getUserData().toString();
    }

    private TreeNodeType selectedNodeType() {
        var selected = selectedTreeNode();
        return selected == null ? TreeNodeType.ROOT : selected.type();
    }

    private ConfigurationTreeNode selectedTreeNode() {
        var selected = configurationTree.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getValue();
    }

    private pl.sk.ocr.config.dto.FieldDto selectedField() {
        var selected = selectedTreeNode();
        var fields = fields();
        if (selected == null || selected.type() != TreeNodeType.FIELD || selected.index() < 0 || selected.index() >= fields.size()) {
            return null;
        }
        return fields.get(selected.index());
    }

    private List<pl.sk.ocr.config.dto.FieldDto> fields() {
        return viewModel.draft() == null || viewModel.draft().fields() == null ? List.of() : viewModel.draft().fields();
    }

    private List<ConditionGroupDto> identificationGroups() {
        return viewModel.draft() == null || viewModel.draft().identification() == null || viewModel.draft().identification().groups() == null
            ? List.of()
            : viewModel.draft().identification().groups();
    }

    private List<pl.sk.ocr.config.dto.ConditionDto> conditions(int groupIndex) {
        var groups = identificationGroups();
        if (groupIndex < 0 || groupIndex >= groups.size() || groups.get(groupIndex).conditions() == null) {
            return List.of();
        }
        return groups.get(groupIndex).conditions();
    }

    private List<pl.sk.ocr.config.dto.AnchorDto> anchors() {
        return viewModel.draft() == null || viewModel.draft().anchors() == null ? List.of() : viewModel.draft().anchors();
    }

    private pl.sk.ocr.config.dto.AnchorDto anchor(int anchorIndex) {
        var anchors = anchors();
        if (anchorIndex < 0 || anchorIndex >= anchors.size()) {
            return null;
        }
        return anchors.get(anchorIndex);
    }

    private pl.sk.ocr.config.dto.ConditionDto condition(int groupIndex, int conditionIndex) {
        var conditions = conditions(groupIndex);
        if (conditionIndex < 0 || conditionIndex >= conditions.size()) {
            return null;
        }
        return conditions.get(conditionIndex);
    }

    private pl.sk.ocr.config.dto.ConditionDto selectedCondition() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.CONDITION) {
            return null;
        }
        return condition(selected.index(), selected.childIndex());
    }

    private Selection identificationSelection() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() == TreeNodeType.IDENTIFICATION) {
            return new Selection(SelectionType.ROOT, -1, -1);
        }
        if (selected.type() == TreeNodeType.IDENTIFICATION_GROUP) {
            return new Selection(SelectionType.GROUP, selected.index(), -1);
        }
        if (selected.type() == TreeNodeType.CONDITION) {
            return new Selection(SelectionType.CONDITION, selected.index(), selected.childIndex());
        }
        return new Selection(SelectionType.ROOT, -1, -1);
    }

    private pl.sk.ocr.config.dto.AnchorDto selectedAnchor() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.ANCHOR) {
            return null;
        }
        return anchor(selected.index());
    }

    private int selectedAnchorIndex() {
        var selected = selectedTreeNode();
        return selected == null || selected.type() != TreeNodeType.ANCHOR ? -1 : selected.index();
    }

    private String selectedTreeNodeId() {
        var selected = configurationTree.getSelectionModel().getSelectedItem();
        return selected == null || selected.getValue() == null ? null : selected.getValue().id();
    }

    private boolean selectTreeNode(TreeItem<ConfigurationTreeNode> item, String id) {
        if (id != null && id.equals(item.getValue().id())) {
            configurationTree.getSelectionModel().select(item);
            return true;
        }
        for (var child : item.getChildren()) {
            if (selectTreeNode(child, id)) {
                return true;
            }
        }
        if (id == null) {
            configurationTree.getSelectionModel().select(item);
            return true;
        }
        return false;
    }

    private boolean selectTreeNodeAndExpandParents(TreeItem<ConfigurationTreeNode> item, String id) {
        if (id == null) {
            return selectTreeNode(item, null);
        }
        if (id.equals(item.getValue().id())) {
            configurationTree.getSelectionModel().select(item);
            return true;
        }
        for (var child : item.getChildren()) {
            if (selectTreeNodeAndExpandParents(child, id)) {
                item.setExpanded(true);
                return true;
            }
        }
        return false;
    }

    private Set<String> expandedTreeNodeIds(TreeItem<ConfigurationTreeNode> item) {
        var ids = new HashSet<String>();
        collectExpandedTreeNodeIds(item, ids);
        return ids;
    }

    private void collectExpandedTreeNodeIds(TreeItem<ConfigurationTreeNode> item, Set<String> ids) {
        if (item == null || item.getValue() == null) {
            return;
        }
        if (item.isExpanded()) {
            ids.add(item.getValue().id());
        }
        for (var child : item.getChildren()) {
            collectExpandedTreeNodeIds(child, ids);
        }
    }

    private void restoreExpandedTreeNodes(TreeItem<ConfigurationTreeNode> item, Set<String> expandedNodeIds) {
        if (item == null || item.getValue() == null) {
            return;
        }
        item.setExpanded(item.getValue().type() == TreeNodeType.ROOT || expandedNodeIds.contains(item.getValue().id()));
        for (var child : item.getChildren()) {
            restoreExpandedTreeNodes(child, expandedNodeIds);
        }
    }

    private void switchToNodePage(ConfigurationTreeNode node) {
        if (node == null || node.page() == null || viewModel.session().pageCache().isEmpty()) {
            return;
        }
        var page = Math.max(1, Math.min(viewModel.session().pageCache().size(), node.page()));
        if (page != viewModel.session().currentPage()) {
            viewModel.session().currentPage(page);
            renderPage();
            refreshPageStatus();
        }
    }

    private void refreshPageStatus() {
        var pages = viewModel.session().pageCache().size();
        var current = pages == 0 ? 0 : viewModel.session().currentPage();
        pageLabel.setText("Page " + current + "/" + pages
            + " | Zoom " + Math.round(zoom * 100) + "%");
        viewerPageNumber.setText(String.valueOf(current == 0 ? 1 : current));
        viewerPageTotal.setText("/" + pages);
        previousPage.setDisable(pages <= 1 || current <= 1);
        nextPage.setDisable(pages <= 1 || current >= pages);
        viewerPageNumber.setEditable(pages > 1);
        viewerPageNumber.setDisable(pages == 0);
    }

    private String labelOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void selectPageType(String type) {
        switch (type) {
            case PAGE_TYPE_RANGE -> pageType.selectToggle(pageTypeRange);
            case PAGE_TYPE_LIST -> pageType.selectToggle(pageTypeList);
            case PAGE_TYPE_ALL -> pageType.selectToggle(pageTypeAll);
            default -> pageType.selectToggle(pageTypeSingle);
        }
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

    private final class PaneOverlay extends javafx.scene.layout.Pane {
        private static final double REGION_HIT_TOLERANCE = 6.0;
        private static final double MIN_REGION_SIZE = 1.0;
        private final ImageView imageView;
        private ScaledCoordinateMapper mapper = new ScaledCoordinateMapper(1, 0, 0);
        private ViewerMode mode = ViewerMode.SELECT;
        private ViewerPoint dragStart;
        private Rectangle draftRegion;
        private final List<EditableRegion> editableRegions = new ArrayList<>();
        private EditableRegion activeEditableRegion;
        private RegionDragMode regionDragMode = RegionDragMode.NONE;
        private double regionDragStartX;
        private double regionDragStartY;
        private double regionStartX;
        private double regionStartY;
        private double regionStartWidth;
        private double regionStartHeight;

        PaneOverlay(ImageView imageView) {
            this.imageView = imageView;
            getChildren().add(imageView);
            setPadding(new Insets(12));
            imageView.setPreserveRatio(true);
            setOnMousePressed(event -> {
                if (mode == ViewerMode.SELECT) {
                    regionDragMode = hitEditableRegion(event.getX(), event.getY()).mode();
                    if (regionDragMode != RegionDragMode.NONE) {
                        regionDragStartX = event.getX();
                        regionDragStartY = event.getY();
                        regionStartX = activeEditableRegion.rectangle().getX();
                        regionStartY = activeEditableRegion.rectangle().getY();
                        regionStartWidth = activeEditableRegion.rectangle().getWidth();
                        regionStartHeight = activeEditableRegion.rectangle().getHeight();
                        event.consume();
                    }
                    return;
                }
                if (mode != ViewerMode.DRAW_REGION) {
                    return;
                }
                dragStart = new ViewerPoint(event.getX(), event.getY());
                draftRegion = new Rectangle(event.getX(), event.getY(), 0, 0);
                draftRegion.setFill(Color.color(0.12, 0.48, 0.93, 0.18));
                draftRegion.setStroke(Color.web("#1f7aec"));
                draftRegion.setStrokeWidth(1.5);
                addOverlay(draftRegion);
                event.consume();
            });
            setOnMouseDragged(event -> {
                if (mode == ViewerMode.SELECT && regionDragMode != RegionDragMode.NONE && activeEditableRegion != null) {
                    updateEditableRegionDrag(event.getX(), event.getY());
                    updateSelectedEditableRegionFromViewer(activeEditableRegion.type(), screenRegion(activeEditableRegion.rectangle()), false);
                    event.consume();
                    return;
                }
                if (mode != ViewerMode.DRAW_REGION || dragStart == null || draftRegion == null) {
                    return;
                }
                var x = Math.min(dragStart.x(), event.getX());
                var y = Math.min(dragStart.y(), event.getY());
                draftRegion.setX(x);
                draftRegion.setY(y);
                draftRegion.setWidth(Math.abs(event.getX() - dragStart.x()));
                draftRegion.setHeight(Math.abs(event.getY() - dragStart.y()));
                event.consume();
            });
            setOnMouseReleased(event -> {
                if (mode == ViewerMode.SELECT && regionDragMode != RegionDragMode.NONE) {
                    if (activeEditableRegion != null) {
                        updateSelectedEditableRegionFromViewer(activeEditableRegion.type(), screenRegion(activeEditableRegion.rectangle()), true);
                    }
                    regionDragMode = RegionDragMode.NONE;
                    activeEditableRegion = null;
                    updateSelectCursor(event.getX(), event.getY());
                    event.consume();
                    return;
                }
                if (mode != ViewerMode.DRAW_REGION || dragStart == null) {
                    return;
                }
                var end = new ViewerPoint(event.getX(), event.getY());
                var startImage = mapper.screenToImage(dragStart);
                var endImage = mapper.screenToImage(end);
                var x = Math.min(startImage.x(), endImage.x());
                var y = Math.min(startImage.y(), endImage.y());
                var width = Math.abs(endImage.x() - startImage.x());
                var height = Math.abs(endImage.y() - startImage.y());
                dragStart = null;
                draftRegion = null;
                if (width > 0 && height > 0) {
                    applyDrawnRegion(roundedRegion(x, y, width, height));
                }
                event.consume();
            });
            setOnMouseMoved(event -> {
                if (mode == ViewerMode.SELECT) {
                    updateSelectCursor(event.getX(), event.getY());
                }
            });
            setOnMouseExited(event -> {
                if (mode == ViewerMode.SELECT && regionDragMode == RegionDragMode.NONE) {
                    setCursor(Cursor.DEFAULT);
                }
            });
            setOnMouseClicked(event -> {
                if (mode == ViewerMode.DRAW_REGION) {
                    event.consume();
                    return;
                }
                var point = mapper.screenToImage(new ViewerPoint(event.getX(), event.getY()));
                setUserData(point);
            });
        }

        Insets padding() {
            return getPadding();
        }

        void setContentSize(double width, double height) {
            var padding = getPadding();
            setMinSize(width + padding.getLeft() + padding.getRight(), height + padding.getTop() + padding.getBottom());
            setPrefSize(width + padding.getLeft() + padding.getRight(), height + padding.getTop() + padding.getBottom());
        }

        ScaledCoordinateMapper mapper() {
            return mapper;
        }

        void setMapper(ScaledCoordinateMapper mapper) {
            this.mapper = mapper;
        }

        void mode(ViewerMode mode) {
            this.mode = mode;
            regionDragMode = RegionDragMode.NONE;
            setCursor(cursorForMode(mode));
        }

        void addOverlay(Rectangle rectangle) {
            getChildren().add(rectangle);
        }

        void editableRegion(Rectangle rectangle, RegionTargetType type) {
            editableRegions.add(new EditableRegion(rectangle, type));
        }

        void clearEditableRegions() {
            editableRegions.clear();
            activeEditableRegion = null;
        }

        void clearOverlay() {
            getChildren().removeIf(node -> node != imageView);
            draftRegion = null;
            clearEditableRegions();
            regionDragMode = RegionDragMode.NONE;
        }

        private Cursor cursorForMode(ViewerMode mode) {
            return switch (mode) {
                case PAN -> Cursor.MOVE;
                case DRAW_REGION -> Cursor.CROSSHAIR;
                case SELECT -> Cursor.DEFAULT;
            };
        }

        private void updateSelectCursor(double x, double y) {
            setCursor(cursorForRegionDragMode(hitEditableRegion(x, y).mode()));
        }

        private Cursor cursorForRegionDragMode(RegionDragMode dragMode) {
            return switch (dragMode) {
                case MOVE -> Cursor.MOVE;
                case LEFT, RIGHT -> Cursor.H_RESIZE;
                case TOP, BOTTOM -> Cursor.V_RESIZE;
                case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
                case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
                case NONE -> Cursor.DEFAULT;
            };
        }

        private RegionHit hitEditableRegion(double x, double y) {
            for (int i = editableRegions.size() - 1; i >= 0; i--) {
                var editableRegion = editableRegions.get(i);
                var dragMode = hitRegion(editableRegion.rectangle(), x, y);
                if (dragMode != RegionDragMode.NONE) {
                    activeEditableRegion = editableRegion;
                    return new RegionHit(editableRegion, dragMode);
                }
            }
            activeEditableRegion = null;
            return new RegionHit(null, RegionDragMode.NONE);
        }

        private RegionDragMode hitRegion(Rectangle rectangle, double x, double y) {
            if (rectangle == null || rectangle.getWidth() <= 0 || rectangle.getHeight() <= 0) {
                return RegionDragMode.NONE;
            }
            var left = rectangle.getX();
            var top = rectangle.getY();
            var right = left + rectangle.getWidth();
            var bottom = top + rectangle.getHeight();
            var withinExpanded = x >= left - REGION_HIT_TOLERANCE
                && x <= right + REGION_HIT_TOLERANCE
                && y >= top - REGION_HIT_TOLERANCE
                && y <= bottom + REGION_HIT_TOLERANCE;
            if (!withinExpanded) {
                return RegionDragMode.NONE;
            }
            var nearLeft = Math.abs(x - left) <= REGION_HIT_TOLERANCE;
            var nearRight = Math.abs(x - right) <= REGION_HIT_TOLERANCE;
            var nearTop = Math.abs(y - top) <= REGION_HIT_TOLERANCE;
            var nearBottom = Math.abs(y - bottom) <= REGION_HIT_TOLERANCE;
            if (nearLeft && nearTop) {
                return RegionDragMode.TOP_LEFT;
            }
            if (nearRight && nearTop) {
                return RegionDragMode.TOP_RIGHT;
            }
            if (nearLeft && nearBottom) {
                return RegionDragMode.BOTTOM_LEFT;
            }
            if (nearRight && nearBottom) {
                return RegionDragMode.BOTTOM_RIGHT;
            }
            if (nearLeft) {
                return RegionDragMode.LEFT;
            }
            if (nearRight) {
                return RegionDragMode.RIGHT;
            }
            if (nearTop) {
                return RegionDragMode.TOP;
            }
            if (nearBottom) {
                return RegionDragMode.BOTTOM;
            }
            if (x >= left && x <= right && y >= top && y <= bottom) {
                return RegionDragMode.MOVE;
            }
            return RegionDragMode.NONE;
        }

        private void updateEditableRegionDrag(double x, double y) {
            var dx = x - regionDragStartX;
            var dy = y - regionDragStartY;
            var left = regionStartX;
            var top = regionStartY;
            var right = regionStartX + regionStartWidth;
            var bottom = regionStartY + regionStartHeight;
            switch (regionDragMode) {
                case MOVE -> {
                    left = regionStartX + dx;
                    right = left + regionStartWidth;
                    top = regionStartY + dy;
                    bottom = top + regionStartHeight;
                }
                case LEFT, TOP_LEFT, BOTTOM_LEFT -> left = Math.min(right - MIN_REGION_SIZE, regionStartX + dx);
                case RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> right = Math.max(left + MIN_REGION_SIZE, regionStartX + regionStartWidth + dx);
                case TOP -> top = Math.min(bottom - MIN_REGION_SIZE, regionStartY + dy);
                case BOTTOM -> bottom = Math.max(top + MIN_REGION_SIZE, regionStartY + regionStartHeight + dy);
                case NONE -> {
                }
            }
            if (regionDragMode == RegionDragMode.TOP_LEFT || regionDragMode == RegionDragMode.TOP_RIGHT) {
                top = Math.min(bottom - MIN_REGION_SIZE, regionStartY + dy);
            }
            if (regionDragMode == RegionDragMode.BOTTOM_LEFT || regionDragMode == RegionDragMode.BOTTOM_RIGHT) {
                bottom = Math.max(top + MIN_REGION_SIZE, regionStartY + regionStartHeight + dy);
            }
            activeEditableRegion.rectangle().setX(left);
            activeEditableRegion.rectangle().setY(top);
            activeEditableRegion.rectangle().setWidth(right - left);
            activeEditableRegion.rectangle().setHeight(bottom - top);
        }

        private RegionDto screenRegion(Rectangle rectangle) {
            var topLeft = mapper.screenToImage(new ViewerPoint(rectangle.getX(), rectangle.getY()));
            var bottomRight = mapper.screenToImage(new ViewerPoint(rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight()));
            return roundedRegion(topLeft.x(), topLeft.y(), bottomRight.x() - topLeft.x(), bottomRight.y() - topLeft.y());
        }
    }

    private record ConfigurationTreeNode(TreeNodeType type, String label, String id, int index, int childIndex, Integer page) {
        static ConfigurationTreeNode root(String label) {
            return new ConfigurationTreeNode(TreeNodeType.ROOT, label, "root", -1, -1, null);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum TreeNodeType {
        ROOT,
        IDENTIFICATION,
        IDENTIFICATION_GROUP,
        CONDITION,
        ANCHORS,
        ANCHOR,
        GEOMETRY,
        GEOMETRY_STRATEGY,
        FIELDS,
        FIELD,
        FIELD_OCR,
        FIELD_OUTPUT,
        FIELD_IMAGE_PROCESSORS,
        FIELD_TRANSFORMERS,
        FIELD_VALIDATORS,
        PIPELINE_STEP
    }

    private record RegionEditTarget(RegionTargetType type, int index, int childIndex) {
    }

    private record EditableRegion(Rectangle rectangle, RegionTargetType type) {
    }

    private record RegionHit(EditableRegion region, RegionDragMode mode) {
    }

    private enum RegionTargetType {
        FIELD_REGION,
        CONDITION_SEARCH_REGION,
        ANCHOR_SEARCH_REGION,
        ANCHOR_REFERENCE_BOUNDS
    }

    private enum RegionDragMode {
        NONE,
        MOVE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private enum ViewerMode {
        SELECT("Select"),
        PAN("Pan"),
        DRAW_REGION("Draw Region");

        private final String label;

        ViewerMode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
