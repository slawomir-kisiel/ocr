package pl.sk.ocr.configurator;

import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
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
    private static final String PAGE_TYPE_SINGLE = "SINGLE";
    private static final String PAGE_TYPE_RANGE = "RANGE";
    private static final String PAGE_TYPE_LIST = "LIST";
    private static final String PAGE_TYPE_ALL = "ALL";

    private ConfiguratorServices services;
    private CategoryEditorViewModel viewModel;
    private final TreeView<String> configurationTree = new TreeView<>();
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
    private final ListView<String> validationList = new ListView<>();
    private final Label status = new Label("Ready");
    private final Label pageLabel = new Label("Page 0/0");
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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> saveCategory(stage, false));
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
        var previous = button("Previous Page", () -> changePage(-1));
        var next = button("Next Page", () -> changePage(1));
        var zoomIn = button("Zoom In", () -> setZoom(zoom * 1.25));
        var zoomOut = button("Zoom Out", () -> setZoom(zoom / 1.25));
        var fitPage = button("Fit Page", this::fitPage);
        var runOcr = button("Run OCR", this::runOcr);
        var testCategory = button("Test Category", this::validate);
        var validate = button("Validate", this::validate);
        return new ToolBar(newCategory, openConfig, save, saveAs, new Separator(), openDocument,
            previous, next, zoomIn, zoomOut, fitPage, new Separator(), runOcr, testCategory, validate);
    }

    private SplitPane center() {
        configurationTree.setPrefWidth(280);
        configurationTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> refreshDetails());
        validationList.setPrefHeight(180);
        configureCategoryDetailsForm();
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
        var widthZoom = Math.max(0.2, (viewport.getWidth() - horizontalPadding) / image.getWidth());
        var heightZoom = Math.max(0.2, (viewport.getHeight() - verticalPadding) / image.getHeight());
        setZoom(Math.min(widthZoom, heightZoom));
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
        refreshingDetails = true;
        if (draft == null) {
            detailsPanel.setDisable(true);
            detailsInfo.setText("Create or open a category configuration.");
            clearCategoryDetailsForm();
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
        detailsInfo.setText("Dirty=" + viewModel.session().dirty()
            + " | Reference document=" + (viewModel.session().referenceDocument() == null ? "" : viewModel.session().referenceDocument()));
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

        var categorySection = section("Category");
        addFormRow(categorySection, "ID", categoryId);
        addFormRow(categorySection, "Display Name", categoryDisplayName);
        addFormRow(categorySection, "Description", categoryDescription);
        addFormRow(categorySection, "Version", categoryVersion);

        var pagePolicySection = section("Page Policy");
        pagePolicySection.getChildren().add(pageTypeControls);
        addFormRow(pagePolicySection, "Page", pageNumber, pageNumberField);
        addFormRow(pagePolicySection, "From", pageFrom, pageFromField);
        addFormRow(pagePolicySection, "To", pageTo, pageToField);
        addFormRow(pagePolicySection, "Pages", pageList, pageListField);

        var ocrSection = section("OCR");
        addFormRow(ocrSection, "Language", ocrLanguage);
        addFormRow(ocrSection, "Datapath", ocrDatapath);

        var form = new VBox(10, categorySection, pagePolicySection, ocrSection);
        detailsPanel.getChildren().setAll(form, detailsInfo);
        updatePagePolicyFieldsVisibility();
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
}
