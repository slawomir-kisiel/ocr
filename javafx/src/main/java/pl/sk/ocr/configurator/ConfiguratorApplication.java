package pl.sk.ocr.configurator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import pl.sk.ocr.config.dto.GeometryDto;
import pl.sk.ocr.config.dto.GeometryStrategyDto;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.ConfiguratorServices;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.app.RunPageOcrUseCase;
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
    private final Label identificationGroupsCount = new Label();
    private final Button addIdentificationGroup = new Button("Add Group");
    private final Button removeLastIdentificationGroup = new Button("Remove Last Group");
    private final Label anchorsCount = new Label();
    private final Label fieldsCount = new Label();
    private final TextField geometryReferenceWidth = new TextField();
    private final TextField geometryReferenceHeight = new TextField();
    private final TextField geometryStrategyType = new TextField();
    private final TextField geometryStrategyAnchors = new TextField();
    private final ListView<String> validationList = new ListView<>();
    private final Label status = new Label("Ready");
    private final Label pageLabel = new Label("Page 0/0");
    private final Button previousPage = compactButton("<", "Previous Page", () -> changePage(-1));
    private final Button nextPage = compactButton(">", "Next Page", () -> changePage(1));
    private final TextField viewerPageNumber = new TextField("1");
    private final Label viewerPageTotal = new Label("/0");
    private double zoom = 1.0;
    private boolean refreshingDetails;

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
        var right = new VBox(8, new Label("Properties / Details"), detailsPanel, new Label("Validation"), validationList);
        right.setPadding(new Insets(8));
        VBox.setVgrow(detailsPanel, Priority.ALWAYS);
        documentScroll.setFitToWidth(false);
        documentScroll.setFitToHeight(false);
        documentScroll.setPannable(true);
        documentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.addEventFilter(ScrollEvent.SCROLL, this::handleScrollZoom);
        documentScroll.addEventFilter(ZoomEvent.ZOOM, this::handleTouchpadZoom);
        var split = new SplitPane(configurationTree, documentViewer(), right);
        split.setDividerPositions(0.22, 0.72);
        return split;
    }

    private HBox documentViewer() {
        var zoomToolbar = new VBox(6,
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
        viewer.setMapper(new ScaledCoordinateMapper(zoom, 0, 0));
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
                detailsInfo.setText("OCR word: " + word.text() + " | confidence=" + word.confidence().value()
                    + " | bounds=" + word.boundingBox().region());
                event.consume();
            });
            viewer.addOverlay(rectangle);
        }
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
        var selectedId = selectedTreeNodeId();
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
        selectTreeNode(root, selectedId);
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
        identificationGroupsCount.setText(String.valueOf(draft.identification() == null || draft.identification().groups() == null
            ? 0
            : draft.identification().groups().size()));
        anchorsCount.setText(String.valueOf(draft.anchors() == null ? 0 : draft.anchors().size()));
        fieldsCount.setText(String.valueOf(draft.fields() == null ? 0 : draft.fields().size()));
        var geometry = draft.geometry();
        geometryReferenceWidth.setText(geometry == null || geometry.referenceWidth() == null ? "" : geometry.referenceWidth().toString());
        geometryReferenceHeight.setText(geometry == null || geometry.referenceHeight() == null ? "" : geometry.referenceHeight().toString());
        var strategy = geometry == null ? null : geometry.strategy();
        geometryStrategyType.setText(strategy == null ? "" : nullToEmpty(strategy.type()));
        geometryStrategyAnchors.setText(strategy == null || strategy.anchors() == null
            ? ""
            : String.join(", ", strategy.anchors()));
        detailsInfo.setText("Dirty=" + viewModel.session().dirty()
            + " | Reference document=" + (viewModel.session().referenceDocument() == null ? "" : viewModel.session().referenceDocument()));
        detailsPanel.getChildren().setAll(detailsFormForSelection());
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
        pageTypeSingle.setSelected(true);
        categoryDescription.setPrefRowCount(3);
        categoryDescription.setWrapText(true);
        detailsInfo.setWrapText(true);
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
        installTooltip(identificationGroupsCount, "Number of OR groups in category identification.");
        installTooltip(addIdentificationGroup, "Add a new empty OR group.");
        installTooltip(removeLastIdentificationGroup, "Remove the last OR group.");
        installTooltip(anchorsCount, "Number of anchors configured for geometry detection.");
        installTooltip(fieldsCount, "Number of fields configured for extraction.");
        installTooltip(geometryReferenceWidth, "Reference document width used by geometry normalization.");
        installTooltip(geometryReferenceHeight, "Reference document height used by geometry normalization.");
        installTooltip(geometryStrategyType, "Geometry strategy type, for example NONE.");
        installTooltip(geometryStrategyAnchors, "Comma-separated anchor IDs used by geometry strategy.");

        addIdentificationGroup.setOnAction(event -> runUiSafe(() -> {
            viewModel.addIdentificationGroup(new ConditionGroupDto(java.util.List.of()));
            refreshAll();
        }));
        removeLastIdentificationGroup.setOnAction(event -> runUiSafe(() -> {
            var groups = viewModel.draft().identification() == null || viewModel.draft().identification().groups() == null
                ? java.util.List.<ConditionGroupDto>of()
                : viewModel.draft().identification().groups();
            if (!groups.isEmpty()) {
                viewModel.removeIdentificationGroup(groups.size() - 1);
                refreshAll();
            }
        }));
        addDraftListener(geometryReferenceWidth, this::applyGeometry);
        addDraftListener(geometryReferenceHeight, this::applyGeometry);
        addDraftListener(geometryStrategyType, this::applyGeometry);
        addDraftListener(geometryStrategyAnchors, this::applyGeometry);
    }

    private VBox detailsFormForSelection() {
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

    private VBox identificationDetailsForm() {
        var section = section("Identification");
        addFormRow(section, "Groups", identificationGroupsCount);
        detachFromParent(addIdentificationGroup);
        detachFromParent(removeLastIdentificationGroup);
        section.getChildren().add(new HBox(8, addIdentificationGroup, removeLastIdentificationGroup));
        return new VBox(10, section, detailsInfo);
    }

    private VBox anchorsDetailsForm() {
        var section = section("Anchors");
        addFormRow(section, "Anchors", anchorsCount);
        return new VBox(10, section, detailsInfo);
    }

    private VBox geometryDetailsForm() {
        var section = section("Geometry");
        addFormRow(section, "Reference Width", geometryReferenceWidth);
        addFormRow(section, "Reference Height", geometryReferenceHeight);
        addFormRow(section, "Strategy Type", geometryStrategyType);
        addFormRow(section, "Strategy Anchors", geometryStrategyAnchors);
        return new VBox(10, section, detailsInfo);
    }

    private VBox fieldsDetailsForm() {
        var section = section("Fields");
        addFormRow(section, "Fields", fieldsCount);
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

    private VBox section(String title) {
        var label = new Label(title);
        label.getStyleClass().add("details-section-title");
        var content = new VBox(8);
        content.setPadding(new Insets(8));
        content.setStyle("-fx-border-color: #c8cdd4; -fx-border-radius: 4; -fx-background-radius: 4;");
        content.getChildren().add(label);
        return content;
    }

    private void addFormRow(VBox form, String labelText, Control control) {
        addFormRow(form, labelText, control, new VBox());
    }

    private void addFormRow(VBox form, String labelText, javafx.scene.Node control) {
        addFormRow(form, labelText, control, new VBox());
    }

    private void addFormRow(VBox form, String labelText, javafx.scene.Node control, VBox field) {
        detachFromParent(control);
        detachFromParent(field);
        var label = new Label(labelText);
        if (control instanceof Control fxControl) {
            label.setLabelFor(fxControl);
            label.setTooltip(fxControl.getTooltip());
            fxControl.setMaxWidth(Double.MAX_VALUE);
        }
        label.setMaxWidth(Double.MAX_VALUE);
        field.setSpacing(2);
        field.getChildren().setAll(label, control);
        field.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().add(field);
        VBox.setVgrow(control, Priority.NEVER);
    }

    private void updatePagePolicyFieldsVisibility() {
        var selected = selectedPageType();
        setVisibleManaged(pageNumberField, PAGE_TYPE_SINGLE.equals(selected));
        setVisibleManaged(pageFromField, PAGE_TYPE_RANGE.equals(selected));
        setVisibleManaged(pageToField, PAGE_TYPE_RANGE.equals(selected));
        setVisibleManaged(pageListField, PAGE_TYPE_LIST.equals(selected));
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void detachFromParent(javafx.scene.Node node) {
        if (node.getParent() instanceof javafx.scene.layout.Pane pane) {
            pane.getChildren().remove(node);
        }
    }

    private void installTooltip(Control control, String text) {
        control.setTooltip(new Tooltip(text));
    }

    private void addDraftListener(TextInputControl control, Runnable action) {
        control.textProperty().addListener((obs, old, value) -> action.run());
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
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        runUiSafe(() -> {
            viewModel.updateGeometry(new GeometryDto(
                parseInteger(geometryReferenceWidth.getText()),
                parseInteger(geometryReferenceHeight.getText()),
                new GeometryStrategyDto(blankToNull(geometryStrategyType.getText()), parseStringList(geometryStrategyAnchors.getText()))
            ));
            refreshAfterDraftEdit();
        });
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

    private String selectedPageType() {
        var selected = pageType.getSelectedToggle();
        return selected == null ? PAGE_TYPE_SINGLE : selected.getUserData().toString();
    }

    private TreeNodeType selectedNodeType() {
        var selected = configurationTree.getSelectionModel().getSelectedItem();
        return selected == null || selected.getValue() == null ? TreeNodeType.ROOT : selected.getValue().type();
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

        void addOverlay(Rectangle rectangle) {
            getChildren().add(rectangle);
        }

        void clearOverlay() {
            getChildren().removeIf(node -> node != imageView);
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
}
