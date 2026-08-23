package pl.sk.ocr.configurator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import static pl.sk.ocr.configurator.ui.FormControls.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import pl.sk.ocr.config.dto.ConditionGroupDto;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.dto.CategoryReferenceDocumentDto;
import pl.sk.ocr.config.dto.CategoryReferenceDocumentsDto;
import pl.sk.ocr.config.dto.DirectoriesDto;
import pl.sk.ocr.config.dto.ExtensionRefDto;
import pl.sk.ocr.config.dto.GeometryDto;
import pl.sk.ocr.config.dto.GeometryStrategyDto;
import pl.sk.ocr.config.dto.ProfileCategoriesDto;
import pl.sk.ocr.config.dto.ProfileDto;
import pl.sk.ocr.config.runtime.ExtensionRef;
import pl.sk.ocr.config.dto.ReferenceFeatureDto;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.ConfiguratorServices;
import pl.sk.ocr.configurator.app.DiagnosticExportUseCase;
import pl.sk.ocr.configurator.app.ApplicationPreferences;
import pl.sk.ocr.configurator.app.ApplicationPreferences.DirectoryKey;
import pl.sk.ocr.configurator.app.ApplicationPreferences.RecentKey;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.app.ProfileWorkspace;
import pl.sk.ocr.configurator.app.RunPageOcrUseCase;
import pl.sk.ocr.configurator.app.InMemoryTraceImageStore;
import pl.sk.ocr.configurator.properties.AnchorPropertiesPanel;
import pl.sk.ocr.configurator.properties.CategoryPropertiesPanel;
import pl.sk.ocr.configurator.properties.FieldPropertiesPanel;
import pl.sk.ocr.configurator.properties.FieldPropertiesPanel.Pipeline;
import pl.sk.ocr.configurator.properties.GeometryPropertiesPanel;
import pl.sk.ocr.configurator.properties.IdentificationPropertiesPanel;
import pl.sk.ocr.configurator.properties.IdentificationPropertiesPanel.Selection;
import pl.sk.ocr.configurator.properties.IdentificationPropertiesPanel.SelectionType;
import pl.sk.ocr.configurator.properties.ProfilePreprocessingPanel;
import pl.sk.ocr.configurator.result.CategoryTestResultPanel;
import pl.sk.ocr.configurator.result.CategoryReferenceDocumentTestResult;
import pl.sk.ocr.configurator.result.FieldResultPanel;
import pl.sk.ocr.configurator.settings.LoadedExtensionsDialog;
import pl.sk.ocr.configurator.settings.SettingsDialog;
import pl.sk.ocr.configurator.trace.TraceViewerPanel;
import pl.sk.ocr.configurator.validation.DraftValidationProblem;
import pl.sk.ocr.configurator.viewer.ScaledCoordinateMapper;
import pl.sk.ocr.configurator.viewer.ViewerPoint;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;

public final class ConfiguratorApplication extends Application {
    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 5.0;

    private ConfiguratorServices services;
    private CategoryEditorViewModel viewModel;
    private ConfigurationFileService fileService;
    private Stage primaryStage;
    private final TreeView<ConfigurationTreeNode> configurationTree = new TreeView<>();
    private final ComboBox<ProfileWorkspace.CategoryEntry> profileCategorySelector = new ComboBox<>();
    private final ProfileWorkspace workspace = new ProfileWorkspace();
    private final ImageView pageImage = new ImageView();
    private final DocumentViewerOverlay viewer = new DocumentViewerOverlay(pageImage, this::updateSelectedEditableRegionFromViewer, this::applyDrawnRegion);
    private final ScrollPane documentScroll = new ScrollPane(viewer);
    private final VBox detailsPanel = new VBox(8);
    private final ScrollPane detailsScroll = new ScrollPane(detailsPanel);
    private final Label detailsInfo = new Label();
    private final TableView<ValidationRow> validationTable = new TableView<>();
    private final Label status = new Label("Ready");
    private final Label pageLabel = new Label("Page 0/0");
    private final Button previousPage = compactButton("<", "Previous Page", () -> changePage(-1));
    private final Button nextPage = compactButton(">", "Next Page", () -> changePage(1));
    private final TextField viewerPageNumber = new TextField("1");
    private final Label viewerPageTotal = new Label("/0");
    private final ComboBox<CategoryReferenceDocumentDto> referenceDocumentSelector = new ComboBox<>();
    private Button selectMode;
    private Button panMode;
    private Button drawRegionMode;
    private double zoom = 1.0;
    private boolean refreshingDetails;
    private boolean refreshingReferenceDocuments;
    private ViewerMode viewerMode = ViewerMode.SELECT;
    private RegionEditTarget regionEditTarget;
    private String pendingTreeSelectionId;
    private GeometryPropertiesPanel geometryPropertiesPanel;
    private AnchorPropertiesPanel anchorPropertiesPanel;
    private IdentificationPropertiesPanel identificationPropertiesPanel;
    private CategoryPropertiesPanel categoryPropertiesPanel;
    private FieldPropertiesPanel fieldPropertiesPanel;
    private ProfilePreprocessingPanel profilePreprocessingPanel;
    private PropertiesPanel propertiesPanel;
    private final DiagnosticExportUseCase diagnosticExport = new DiagnosticExportUseCase();
    private TraceViewerPanel traceViewerPanel;
    private FieldResultPanel fieldResultPanel;
    private CategoryTestResultPanel categoryTestResultPanel;
    private final ApplicationPreferences preferences = new ApplicationPreferences();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        services = ConfiguratorServices.production();
        fileService = new ConfigurationFileService(services.mapper());
        viewModel = new CategoryEditorViewModel(
            fileService,
            new OpenReferenceDocumentUseCase(services.documentReader()),
            new RunPageOcrUseCase(services.ocrEngine()),
            services.previewField(),
            services.testCategory(),
            services.validationService(),
            services.backgroundExecutor()
        );
        geometryPropertiesPanel = new GeometryPropertiesPanel(viewModel, this::anchors, this::fields, pageImage::getImage, status, detailsInfo, this::refreshAfterDraftEdit);
        anchorPropertiesPanel = new AnchorPropertiesPanel(viewModel, this::anchors, this::selectedAnchorIndex, detailsInfo,
            this::refreshAfterDraftEdit, this::refreshAll, selection -> pendingTreeSelectionId = selection,
            this::activateAnchorSearchRegionDrawing, this::activateAnchorReferenceBoundsDrawing, this::svgIcon,
            services.extensionRegistry());
        identificationPropertiesPanel = new IdentificationPropertiesPanel(viewModel, this::identificationSelection, detailsInfo,
            this::refreshAfterDraftEdit, this::refreshAll, selection -> pendingTreeSelectionId = selection,
            this::activateConditionSearchRegionDrawing, this::svgIcon, services.extensionRegistry());
        categoryPropertiesPanel = new CategoryPropertiesPanel(viewModel, detailsInfo, this::refreshAfterDraftEdit, () -> {
            refreshAfterDraftEdit();
            renderOcrOverlay();
        });
        fieldPropertiesPanel = new FieldPropertiesPanel(viewModel, this::fields, this::fieldSelection, detailsInfo,
            this::refreshAfterDraftEdit, this::refreshAll, selection -> pendingTreeSelectionId = selection,
            this::activateFieldRegionDrawing, this::svgIcon, services.extensionRegistry(),
            this::fieldImageProcessorDebugSource);
        profilePreprocessingPanel = new ProfilePreprocessingPanel(workspace::profile, this::updateWorkspaceProfile,
            this::refreshWorkspaceProfile, this::applyWorkspacePreprocessing, services.extensionRegistry(),
            this::workspaceDebugSourceImage);
        propertiesPanel = new PropertiesPanel(detailsPanel, categoryPropertiesPanel, identificationPropertiesPanel,
            anchorPropertiesPanel, geometryPropertiesPanel, fieldPropertiesPanel, this::selectedNodeType, this::emptyDetailsForm);
        traceViewerPanel = new TraceViewerPanel(() -> viewModel.session().latestTrace(), () -> viewModel.session().traceImageStore());
        fieldResultPanel = new FieldResultPanel(() -> viewModel.session().latestFieldResult(), () -> viewModel.session().latestTrace());
        categoryTestResultPanel = new CategoryTestResultPanel(() -> viewModel.session().latestDocumentResult(),
            () -> viewModel.session().latestCategoryTestResults());
        stage.setTitle("OCR Configurator");
        var scene = new Scene(layout(stage), 1280, 820);
        configureAccelerators(scene, stage);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!confirmUnsavedChanges(stage)) {
                event.consume();
            }
        });
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
        root.setTop(new VBox(menuBar(stage), toolbar(stage)));
        root.setCenter(center());
        root.setBottom(statusBar());
        refreshTree();
        refreshReferenceDocuments();
        refreshDetails();
        return root;
    }

    private ToolBar toolbar(Stage stage) {
        var newProfile = button("New Profile", () -> newProfile(stage));
        var openProfile = recentSplitButton("Open Profile", () -> chooseProfile(stage), RecentKey.PROFILE, path -> openRecentProfile(stage, path));
        var save = button("Save Profile", () -> saveProfile(stage, false));
        var saveAs = button("Save Profile As", () -> saveProfile(stage, true));
        var runOcr = button("Run OCR", this::runOcr);
        var previewField = button("Preview Field", this::previewField);
        var testCategory = button("Test Category", this::testCategory);
        var testAllDocuments = button("Test All Documents", this::testAllReferenceDocuments);
        var validate = button("Validate", this::validate);
        var settings = button("Settings", this::showSettings);
        var extensions = button("Extensions", this::showLoadedExtensions);
        return new ToolBar(newProfile, openProfile, save, saveAs, new Separator(),
            runOcr, previewField, testCategory, testAllDocuments, validate, new Separator(), settings, extensions);
    }

    private MenuBar menuBar(Stage stage) {
        var file = new Menu("File");
        var openRecentProfiles = new Menu("Open Recent Profile");
        openRecentProfiles.setOnShowing(event -> populateRecentMenu(openRecentProfiles, RecentKey.PROFILE, path -> openRecentProfile(stage, path)));
        file.setOnShowing(event -> {
            populateRecentMenu(openRecentProfiles, RecentKey.PROFILE, path -> openRecentProfile(stage, path));
        });
        populateRecentMenu(openRecentProfiles, RecentKey.PROFILE, path -> openRecentProfile(stage, path));
        file.getItems().addAll(
            menuItem("New Profile", () -> newProfile(stage), new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN)),
            menuItem("Open Profile", () -> chooseProfile(stage), new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN)),
            openRecentProfiles,
            menuItem("Save Profile", () -> saveProfile(stage, false), new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)),
            menuItem("Save Profile As", () -> saveProfile(stage, true), new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
            new SeparatorMenuItem(),
            menuItem("Exit", stage::close)
        );
        var view = new Menu("View");
        view.getItems().addAll(
            menuItem("Zoom In", () -> setZoom(zoom * 1.25), new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN)),
            menuItem("Zoom Out", () -> setZoom(zoom / 1.25), new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN)),
            menuItem("Fit Page", this::fitPage, new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)),
            menuItem("Fit Width", this::fitWidth, new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
            menuItem("100%", this::actualSize, new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN)),
            new SeparatorMenuItem(),
            menuItem("Previous Page", () -> runOutsideTextInput(() -> changePage(-1)), new KeyCodeCombination(KeyCode.PAGE_UP)),
            menuItem("Next Page", () -> runOutsideTextInput(() -> changePage(1)), new KeyCodeCombination(KeyCode.PAGE_DOWN)),
            menuItem("Focus Page Number", () -> viewerPageNumber.requestFocus(), new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN)),
            new SeparatorMenuItem(),
            menuItem("Select Mode", () -> runOutsideTextInput(() -> setViewerMode(ViewerMode.SELECT)), new KeyCodeCombination(KeyCode.S)),
            menuItem("Pan Mode", () -> runOutsideTextInput(() -> setViewerMode(ViewerMode.PAN)), new KeyCodeCombination(KeyCode.P)),
            menuItem("Draw Region Mode", () -> runOutsideTextInput(this::activateDrawRegionModeFromShortcut), new KeyCodeCombination(KeyCode.R))
        );
        var run = new Menu("Run");
        run.getItems().addAll(
            menuItem("Run OCR", this::runOcr),
            menuItem("Preview Field", this::previewField),
            menuItem("Test Category", this::testCategory, new KeyCodeCombination(KeyCode.F5)),
            menuItem("Test All Reference Documents", this::testAllReferenceDocuments),
            menuItem("Validate Configuration", this::validate)
        );
        var tools = new Menu("Tools");
        tools.getItems().add(menuItem("Settings", this::showSettings));
        var help = new Menu("Help");
        help.getItems().addAll(menuItem("Loaded Extensions", this::showLoadedExtensions), menuItem("About", this::showAbout));
        return new MenuBar(file, view, run, tools, help);
    }

    private MenuItem menuItem(String text, Runnable action) {
        return menuItem(text, action, null);
    }

    private MenuItem menuItem(String text, Runnable action, KeyCombination accelerator) {
        var item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        return item;
    }

    private SplitMenuButton recentSplitButton(String text, Runnable action, RecentKey key, Consumer<Path> recentAction) {
        var button = new SplitMenuButton();
        button.setText(text);
        button.setOnAction(event -> action.run());
        button.setOnShowing(event -> populateRecentMenu(button.getItems(), key, recentAction));
        return button;
    }

    private void populateRecentMenu(Menu menu, RecentKey key, Consumer<Path> action) {
        populateRecentMenu(menu.getItems(), key, action);
    }

    private void populateRecentMenu(javafx.collections.ObservableList<MenuItem> items, RecentKey key, Consumer<Path> action) {
        items.clear();
        var recent = preferences.recentFiles(key);
        if (recent.isEmpty()) {
            var empty = new MenuItem("No recent files");
            empty.setDisable(true);
            items.add(empty);
            return;
        }
        for (var path : recent) {
            var exists = Files.exists(path);
            var item = new MenuItem((exists ? "" : "(missing) ") + recentLabel(path));
            item.setOnAction(event -> action.accept(path));
            items.add(item);
        }
    }

    private String recentLabel(Path path) {
        var fileName = path.getFileName();
        return (fileName == null ? path.toString() : fileName.toString()) + "  " + path;
    }

    private void configureAccelerators(Scene scene, Stage stage) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> saveProfile(stage, false));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), () -> saveProfile(stage, true));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), () -> chooseProfile(stage));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> newProfile(stage));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom * 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom * 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom * 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom / 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN), () -> setZoom(zoom / 1.25));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN), this::actualSize);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.CONTROL_DOWN), this::actualSize);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), this::fitPage);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::fitWidth);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PAGE_UP), () -> runOutsideTextInput(() -> changePage(-1)));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PAGE_DOWN), () -> runOutsideTextInput(() -> changePage(1)));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN), () -> viewerPageNumber.requestFocus());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S), () -> runOutsideTextInput(() -> setViewerMode(ViewerMode.SELECT)));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.P), () -> runOutsideTextInput(() -> setViewerMode(ViewerMode.PAN)));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R), () -> runOutsideTextInput(this::activateDrawRegionModeFromShortcut));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::testCategory);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::cancelRegionEdit);
    }

    private void runOutsideTextInput(Runnable action) {
        if (!isTextInputFocused()) {
            action.run();
        }
    }

    private boolean isTextInputFocused() {
        var scene = primaryStage == null ? null : primaryStage.getScene();
        return scene != null && scene.getFocusOwner() instanceof TextInputControl;
    }

    private void activateDrawRegionModeFromShortcut() {
        ensureRegionEditTargetForSelection();
        if (regionEditTarget != null) {
            setViewerMode(ViewerMode.DRAW_REGION);
        }
    }

    private SplitPane center() {
        configurationTree.setPrefWidth(280);
        configurationTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            switchToNodePage(selected == null ? null : selected.getValue());
            refreshDetails();
        });
        configureValidationTable();
        configureCategoryDetailsForm();
        detailsScroll.setFitToWidth(true);
        detailsScroll.setFitToHeight(false);
        detailsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        detailsPanel.setMaxWidth(Double.MAX_VALUE);
        var categoryTestResultView = categoryTestResultPanel.view();
        var fieldResultView = fieldResultPanel.view();
        var traceView = traceViewerPanel.view();
        var propertiesTabContent = new VBox(8, sectionLabel("Properties"), detailsScroll);
        var validationTraceContent = new VBox(8, sectionLabel("Validation"), validationTable,
            sectionLabel("Category Test Result"), categoryTestResultView,
            sectionLabel("Field Result"), fieldResultView, sectionLabel("Trace"), traceExportBar(), traceView);
        validationTraceContent.setPadding(new Insets(8));
        var validationTraceScroll = new ScrollPane(validationTraceContent);
        validationTraceScroll.setFitToWidth(true);
        validationTraceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        validationTraceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        validationTraceScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        var tabs = new TabPane(
            closableTab("Properties", propertiesTabContent),
            closableTab("Validation/Trace", validationTraceScroll)
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        var right = new VBox(tabs);
        right.setPadding(new Insets(8));
        VBox.setVgrow(detailsScroll, Priority.ALWAYS);
        VBox.setVgrow(propertiesTabContent, Priority.ALWAYS);
        VBox.setVgrow(categoryTestResultView, Priority.NEVER);
        VBox.setVgrow(fieldResultView, Priority.NEVER);
        VBox.setVgrow(traceView, Priority.ALWAYS);
        VBox.setVgrow(validationTraceContent, Priority.ALWAYS);
        VBox.setVgrow(validationTraceScroll, Priority.ALWAYS);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        documentScroll.setFitToWidth(false);
        documentScroll.setFitToHeight(false);
        documentScroll.setPannable(false);
        documentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentScroll.addEventFilter(ScrollEvent.SCROLL, this::handleScrollZoom);
        documentScroll.addEventFilter(ZoomEvent.ZOOM, this::handleTouchpadZoom);
        var split = new SplitPane(workspacePane(primaryStage), documentViewer(), right);
        split.setDividerPositions(0.22, 0.72);
        return split;
    }

    private VBox workspacePane(Stage stage) {
        profileCategorySelector.setMaxWidth(Double.MAX_VALUE);
        profileCategorySelector.setPromptText("No category");
        profileCategorySelector.setTooltip(new Tooltip("Categories included in the current profile."));
        profileCategorySelector.setOnAction(event -> selectWorkspaceCategory(profileCategorySelector.getSelectionModel().getSelectedIndex()));
        var actions = new HBox(6,
            button("Nowa", () -> addNewWorkspaceCategory(stage)),
            button("Otwórz", () -> attachWorkspaceCategory(stage)),
            button("Usuń", () -> removeWorkspaceCategory(stage))
        );
        actions.setAlignment(Pos.CENTER_LEFT);
        var header = new VBox(6, sectionLabel("Profile categories"), profileCategorySelector, actions);
        header.setPadding(new Insets(8));
        header.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #c8cdd4; -fx-border-width: 0 0 1 0;");
        var categoriesPane = new VBox(header, configurationTree);
        VBox.setVgrow(configurationTree, Priority.ALWAYS);
        var tabs = new TabPane(
            closableTab("Preprocessing", profilePreprocessingPanel.view()),
            closableTab("Categories", categoriesPane)
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        var pane = new VBox(tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        VBox.setVgrow(configurationTree, Priority.ALWAYS);
        pane.setPrefWidth(300);
        return pane;
    }

    private Tab closableTab(String title, Node content) {
        var tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private void configureValidationTable() {
        validationTable.setPrefHeight(180);
        validationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        validationTable.getColumns().add(validationColumn("Severity", "severity", 70));
        validationTable.getColumns().add(validationColumn("Code", "code", 150));
        validationTable.getColumns().add(validationColumn("Path", "path", 190));
        validationTable.getColumns().add(validationColumn("Message", "message", 260));
        validationTable.setRowFactory(table -> {
            var row = new TableRow<ValidationRow>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    navigateToValidationProblem(row.getItem().path());
                }
            });
            return row;
        });
        validationTable.setStyle("-fx-text-fill: #111827;");
        validationTable.skinProperty().addListener((obs, old, skin) ->
            Platform.runLater(() -> validationTable.lookupAll(".column-header .label")
                .forEach(node -> node.setStyle("-fx-text-fill: #111827;"))));
    }

    private TableColumn<ValidationRow, String> validationColumn(String title, String property, double width) {
        var column = new TableColumn<ValidationRow, String>(title);
        column.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        column.setStyle("-fx-text-fill: #111827;");
        return column;
    }

    private HBox traceExportBar() {
        var selected = button("Export Selected Image", () -> exportSelectedTraceImage(primaryStage));
        var allImages = button("Export All Images", () -> exportAllTraceImages(primaryStage));
        var metadata = button("Export Metadata", () -> exportTraceMetadata(primaryStage));
        var bundle = button("Export Bundle ZIP", () -> exportTraceBundle(primaryStage));
        var bar = new HBox(6, selected, allImages, metadata, bundle);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private Label sectionLabel(String text) {
        var label = new Label(text);
        label.setStyle("-fx-text-fill: #111827;");
        return label;
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
        content.setTop(referenceDocumentBar());
        content.setBottom(pageNavigator());
        var pane = new HBox(zoomToolbar, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox.setHgrow(documentScroll, Priority.ALWAYS);
        refreshViewerModeButtons();
        return pane;
    }

    private HBox referenceDocumentBar() {
        referenceDocumentSelector.setMaxWidth(Double.MAX_VALUE);
        referenceDocumentSelector.setPromptText("No reference document");
        referenceDocumentSelector.setTooltip(new Tooltip("Reference documents configured for the selected category."));
        referenceDocumentSelector.setCellFactory(view -> referenceDocumentCell());
        referenceDocumentSelector.setButtonCell(referenceDocumentCell());
        referenceDocumentSelector.setOnAction(event -> selectReferenceDocument(referenceDocumentSelector.getSelectionModel().getSelectedItem()));
        var add = iconButton("plus.svg", "Add reference document", () -> addReferenceDocument(primaryStage));
        var edit = iconButton("edit.svg", "Edit reference document description", () -> editReferenceDocument(primaryStage));
        var remove = iconButton("eraser.svg", "Remove reference document", () -> removeReferenceDocument(primaryStage));
        var bar = new HBox(6, referenceDocumentSelector, add, edit, remove);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #c8cdd4; -fx-border-width: 0 0 1 0;");
        HBox.setHgrow(referenceDocumentSelector, Priority.ALWAYS);
        return bar;
    }

    private ListCell<CategoryReferenceDocumentDto> referenceDocumentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CategoryReferenceDocumentDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : referenceDocumentLabel(item));
            }
        };
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

    private void newProfile(Stage stage) {
        if (!confirmUnsavedChanges(stage)) {
            return;
        }
        workspace.profilePath(null);
        workspace.profile(fileService.newProfile("default", "Default Profile"));
        workspace.categoriesDirectory(null);
        workspace.categories().clear();
        workspace.selectedIndex(-1);
        workspace.dirty(true);
        viewModel.newCategory("new-category", "New Category");
        addCurrentDraftToWorkspace(null);
        status.setText("New profile workspace");
        refreshWorkspaceCategories();
        refreshAll();
    }

    private void chooseProfile(Stage stage) {
        if (!confirmUnsavedChanges(stage)) {
            return;
        }
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Profile JSON", "*.json"));
        configureInitialDirectory(chooser, DirectoryKey.OPEN_PROFILE);
        var file = chooser.showOpenDialog(stage);
        if (file != null) {
            preferences.rememberFile(DirectoryKey.OPEN_PROFILE, file.toPath());
            openProfilePath(file.toPath());
        }
    }

    private boolean saveProfile(Stage stage, boolean saveAs) {
        commitCurrentDetailsForm();
        rememberCurrentWorkspaceDraft();
        ensureWorkspace();
        var path = workspace.profilePath();
        if (saveAs || path == null) {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Profile JSON", "*.json"));
            configureInitialDirectory(chooser, DirectoryKey.SAVE_PROFILE);
            var file = chooser.showSaveDialog(stage);
            path = file == null ? null : file.toPath();
        }
        if (path != null) {
            Path savePath = path;
            preferences.rememberFile(DirectoryKey.SAVE_PROFILE, savePath);
            runUiSafe(() -> {
                saveWorkspace(savePath);
                preferences.rememberRecentFile(RecentKey.PROFILE, savePath);
                refreshAll();
            });
            return true;
        }
        return false;
    }

    private void commitCurrentDetailsForm() {
        if (refreshingDetails || viewModel.draft() == null) {
            return;
        }
        propertiesPanel.commitActive();
    }

    private void openRecentProfile(Stage stage, Path path) {
        if (!Files.exists(path)) {
            showMissingRecentFile(path);
            return;
        }
        if (!confirmUnsavedChanges(stage)) {
            return;
        }
        preferences.rememberFile(DirectoryKey.OPEN_PROFILE, path);
        openProfilePath(path);
    }

    private void openProfilePath(Path path) {
        runUiSafe(() -> {
            loadWorkspace(path);
            preferences.rememberRecentFile(RecentKey.PROFILE, path);
            refreshAll();
        });
    }

    private void attachWorkspaceCategory(Stage stage) {
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Category JSON", "*.json"));
        configureInitialDirectory(chooser, DirectoryKey.OPEN_CONFIGURATION);
        var file = chooser.showOpenDialog(stage);
        if (file != null) {
            preferences.rememberFile(DirectoryKey.OPEN_CONFIGURATION, file.toPath());
            runUiSafe(() -> {
                rememberCurrentWorkspaceDraft();
                addCategoryToWorkspace(file.toPath(), fileService.loadCategory(file.toPath()));
                preferences.rememberRecentFile(RecentKey.CONFIGURATION, file.toPath());
                refreshWorkspaceCategories();
                refreshAll();
            });
        }
    }

    private void addNewWorkspaceCategory(Stage stage) {
        ensureWorkspace();
        rememberCurrentWorkspaceDraft();
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Category JSON", "*.json"));
        configureInitialDirectory(chooser, DirectoryKey.SAVE_CONFIGURATION);
        chooser.setInitialFileName("new-category.json");
        var file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        preferences.rememberFile(DirectoryKey.SAVE_CONFIGURATION, file.toPath());
        viewModel.newCategory("new-category", "New Category");
        addCurrentDraftToWorkspace(file.toPath());
        refreshWorkspaceCategories();
        refreshAll();
    }

    private void removeWorkspaceCategory(Stage stage) {
        var selected = workspace.selectedIndex();
        if (selected < 0 || selected >= workspace.categories().size()) {
            status.setText("Select a category to remove");
            return;
        }
        var entry = workspace.categories().get(selected);
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Remove Category");
        alert.setHeaderText("Remove category from profile?");
        alert.setContentText("The category file will not be deleted: " + (entry.path() == null ? "(new category)" : entry.path()));
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        workspace.categories().remove(selected);
        workspace.dirty(true);
        var next = Math.min(selected, workspace.categories().size() - 1);
        workspace.selectedIndex(next);
        if (next >= 0) {
            openWorkspaceCategory(next);
        } else {
            viewModel.newCategory("new-category", "New Category");
            viewModel.session().categoryPath(null);
        }
        refreshWorkspaceCategories();
        refreshAll();
    }

    private void ensureWorkspace() {
        if (workspace.profile() != null) {
            return;
        }
        workspace.profile(fileService.newProfile("default", "Default Profile"));
        workspace.categoriesDirectory(null);
        workspace.dirty(true);
    }

    private void loadWorkspace(Path profilePath) {
        var profile = fileService.loadProfile(profilePath);
        workspace.profilePath(profilePath);
        workspace.profile(profile);
        workspace.categories().clear();
        workspace.categoriesDirectory(resolveProfilePath(profilePath, profile.categories().directory()));
        for (var categoryPath : categoryPaths(profilePath, profile)) {
            if (Files.exists(categoryPath)) {
                addCategoryToWorkspace(categoryPath, fileService.loadCategory(categoryPath));
            }
        }
        workspace.markSaved();
        workspace.selectedIndex(workspace.categories().isEmpty() ? -1 : 0);
        if (workspace.selectedIndex() >= 0) {
            openWorkspaceCategory(workspace.selectedIndex());
        }
        refreshWorkspaceCategories();
        status.setText("Loaded profile: " + profilePath.getFileName());
    }

    private List<Path> categoryPaths(Path profilePath, ProfileDto profile) {
        var categories = profile.categories();
        if (categories == null || categories.directory() == null || categories.directory().isBlank()) {
            return List.of();
        }
        var directory = resolveProfilePath(profilePath, categories.directory());
        if (categories.files() != null && !categories.files().isEmpty()) {
            return categories.files().stream()
                .map(file -> resolveProfilePath(profilePath, file))
                .toList();
        }
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        var mode = categories.mode() == null ? "EXPLICIT" : categories.mode();
        if ("ALL".equals(mode)) {
            return listCategoryJsonFiles(directory);
        }
        var active = categories.active() == null ? List.<String>of() : categories.active();
        var byId = new java.util.LinkedHashMap<String, Path>();
        for (var categoryPath : listCategoryJsonFiles(directory)) {
            var category = fileService.loadCategory(categoryPath);
            if (active.contains(category.id())) {
                byId.put(category.id(), categoryPath);
            }
        }
        var paths = new ArrayList<Path>();
        for (var id : active) {
            var path = byId.get(id);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    private List<Path> listCategoryJsonFiles(Path directory) {
        var paths = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (var path : stream) {
                paths.add(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list profile categories: " + directory, e);
        }
        paths.sort(java.util.Comparator.comparing(path -> path.getFileName().toString()));
        return paths;
    }

    private void saveWorkspace(Path profilePath) {
        var directory = workspace.categoriesDirectory();
        if (directory == null) {
            directory = profilePath.toAbsolutePath().getParent().resolve("categories").normalize();
            workspace.categoriesDirectory(directory);
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create categories directory: " + directory, e);
        }
        for (int i = 0; i < workspace.categories().size(); i++) {
            var entry = workspace.categories().get(i);
            var path = entry.path() == null ? directory.resolve(fileName(entry.draft().id())) : entry.path();
            fileService.saveCategory(path, entry.draft());
            workspace.categories().set(i, new ProfileWorkspace.CategoryEntry(entry.draft().id(), entry.draft().displayName(), path, entry.draft()));
        }
        var savedProfile = profileForSave(profilePath, directory);
        fileService.saveProfile(profilePath, savedProfile);
        workspace.profilePath(profilePath);
        workspace.profile(savedProfile);
        workspace.markSaved();
        var selected = workspace.selectedCategory();
        if (selected != null) {
            viewModel.session().categoryPath(selected.path());
            viewModel.session().markSaved();
        }
        refreshWorkspaceCategories();
        status.setText("Saved profile: " + profilePath.getFileName());
    }

    private ProfileDto profileForSave(Path profilePath, Path categoriesDirectory) {
        var base = workspace.profile() == null ? fileService.newProfile("default", "Default Profile") : workspace.profile();
        var active = workspace.categories().stream()
            .map(ProfileWorkspace.CategoryEntry::draft)
            .map(CategoryDto::id)
            .toList();
        var relativeCategories = relativize(profilePath.toAbsolutePath().getParent(), categoriesDirectory);
        var categoryFiles = workspace.categories().stream()
            .map(ProfileWorkspace.CategoryEntry::path)
            .filter(java.util.Objects::nonNull)
            .map(path -> relativize(profilePath.toAbsolutePath().getParent(), path))
            .toList();
        return new ProfileDto(
            base.schemaVersion() == null ? "1.0" : base.schemaVersion(),
            base.id() == null || base.id().isBlank() ? "default" : base.id(),
            base.version() == null || base.version().isBlank() ? "1.0" : base.version(),
            base.displayName(),
            base.description(),
            new ProfileCategoriesDto(relativeCategories, "EXPLICIT", active, categoryFiles),
            base.preprocessing(),
            base.directories() == null ? new DirectoriesDto("./input", "./success", "./error") : base.directories(),
            base.processing(),
            base.ocr(),
            base.trace(),
            base.output()
        );
    }

    private void addCurrentDraftToWorkspace(Path path) {
        addCategoryToWorkspace(path, viewModel.draft());
    }

    private void addCategoryToWorkspace(Path path, CategoryDto draft) {
        if (draft == null) {
            return;
        }
        var entry = new ProfileWorkspace.CategoryEntry(draft.id(), draft.displayName(), path, draft);
        var existing = indexOfCategory(draft.id());
        if (existing >= 0) {
            workspace.categories().set(existing, entry);
            workspace.selectedIndex(existing);
        } else {
            workspace.categories().add(entry);
            workspace.selectedIndex(workspace.categories().size() - 1);
        }
        openWorkspaceCategory(workspace.selectedIndex());
        workspace.dirty(true);
    }

    private int indexOfCategory(String id) {
        for (int i = 0; i < workspace.categories().size(); i++) {
            if (java.util.Objects.equals(workspace.categories().get(i).id(), id)) {
                return i;
            }
        }
        return -1;
    }

    private void rememberCurrentWorkspaceDraft() {
        var selected = workspace.selectedIndex();
        var draft = viewModel.draft();
        if (selected >= 0 && selected < workspace.categories().size() && draft != null) {
            var previous = workspace.categories().get(selected);
            workspace.categories().set(selected, new ProfileWorkspace.CategoryEntry(draft.id(), draft.displayName(), previous.path(), draft));
        }
        if (viewModel.session().dirty()) {
            workspace.dirty(true);
        }
    }

    private void selectWorkspaceCategory(int index) {
        if (index < 0 || index == workspace.selectedIndex() || index >= workspace.categories().size()) {
            return;
        }
        rememberCurrentWorkspaceDraft();
        workspace.selectedIndex(index);
        openWorkspaceCategory(index);
        refreshAll();
    }

    private void openWorkspaceCategory(int index) {
        var entry = workspace.categories().get(index);
        viewModel.session().categoryPath(entry.path());
        viewModel.session().openDraft(entry.draft());
        status.setText("Selected category: " + entry);
        openActiveCategoryReferenceDocument();
    }

    private void refreshWorkspaceCategories() {
        profileCategorySelector.getItems().setAll(workspace.categories());
        if (workspace.selectedIndex() >= 0 && workspace.selectedIndex() < workspace.categories().size()) {
            profileCategorySelector.getSelectionModel().select(workspace.selectedIndex());
        } else {
            profileCategorySelector.getSelectionModel().clearSelection();
        }
    }

    private void refreshReferenceDocuments() {
        refreshingReferenceDocuments = true;
        var selected = referenceDocumentSelector.getSelectionModel().getSelectedItem();
        var documents = referenceDocuments(viewModel.draft());
        referenceDocumentSelector.getItems().setAll(documents);
        var active = activeReferenceDocument(viewModel.draft());
        var toSelect = documents.stream()
            .filter(document -> java.util.Objects.equals(document.id(), active))
            .findFirst()
            .orElse(selected == null ? null : documents.stream()
                .filter(document -> java.util.Objects.equals(document.id(), selected.id()))
                .findFirst()
                .orElse(null));
        if (toSelect != null) {
            referenceDocumentSelector.getSelectionModel().select(toSelect);
        } else {
            referenceDocumentSelector.getSelectionModel().clearSelection();
        }
        refreshingReferenceDocuments = false;
    }

    private void addReferenceDocument(Stage stage) {
        if (viewModel.draft() == null) {
            status.setText("Select a category before adding reference document");
            return;
        }
        if (viewModel.session().categoryPath() == null) {
            status.setText("Save category before adding reference document");
            return;
        }
        var chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff"),
            new FileChooser.ExtensionFilter("All files", "*.*")
        );
        configureInitialDirectory(chooser, DirectoryKey.OPEN_DOCUMENT);
        var file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        preferences.rememberFile(DirectoryKey.OPEN_DOCUMENT, file.toPath());
        var id = uniqueReferenceDocumentId(file.toPath());
        var relative = relativize(viewModel.session().categoryPath().toAbsolutePath().getParent(), file.toPath());
        var document = new CategoryReferenceDocumentDto(id, relative, displayName(file.toPath()), "");
        updateReferenceDocuments(addReferenceDocument(referenceDocuments(viewModel.draft()), document), id);
        openDocumentPath(file.toPath());
    }

    private void editReferenceDocument(Stage stage) {
        var selected = referenceDocumentSelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Select reference document to edit");
            return;
        }
        var dialog = new Dialog<CategoryReferenceDocumentDto>();
        dialog.initOwner(stage);
        dialog.setTitle("Reference Document");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        var path = new TextField(selected.path());
        var displayName = new TextField(selected.displayName());
        var description = new TextArea(selected.description());
        description.setPrefRowCount(4);
        var browse = iconButton("edit.svg", "Choose reference document file", () -> {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff"),
                new FileChooser.ExtensionFilter("All files", "*.*")
            );
            configureInitialDirectory(chooser, DirectoryKey.OPEN_DOCUMENT);
            var file = chooser.showOpenDialog(stage);
            if (file != null) {
                preferences.rememberFile(DirectoryKey.OPEN_DOCUMENT, file.toPath());
                var categoryPath = viewModel.session().categoryPath();
                var value = categoryPath == null || categoryPath.toAbsolutePath().getParent() == null
                    ? file.toPath().toAbsolutePath().normalize().toString()
                    : relativize(categoryPath.toAbsolutePath().getParent(), file.toPath());
                path.setText(value);
                if (displayName.getText() == null || displayName.getText().isBlank()) {
                    displayName.setText(displayName(file.toPath()));
                }
            }
        });
        var content = new VBox(8);
        addFormRow(content, "Path", new HBox(6, path, browse));
        addFormRow(content, "Display Name", displayName);
        addFormRow(content, "Description", description);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> button == ButtonType.OK
            ? new CategoryReferenceDocumentDto(selected.id(), path.getText(), displayName.getText(), description.getText())
            : null);
        dialog.showAndWait().ifPresent(updated -> {
            var documents = referenceDocuments(viewModel.draft()).stream()
                .map(document -> java.util.Objects.equals(document.id(), updated.id()) ? updated : document)
                .toList();
            updateReferenceDocuments(documents, updated.id());
            var resolved = resolveCategoryPath(updated.path());
            if (resolved != null && Files.exists(resolved)) {
                openDocumentPath(resolved);
            }
        });
    }

    private void removeReferenceDocument(Stage stage) {
        var selected = referenceDocumentSelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Select reference document to remove");
            return;
        }
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Remove Reference Document");
        alert.setHeaderText("Remove reference document from category?");
        alert.setContentText(referenceDocumentLabel(selected));
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        var documents = referenceDocuments(viewModel.draft()).stream()
            .filter(document -> !java.util.Objects.equals(document.id(), selected.id()))
            .toList();
        var nextActive = documents.isEmpty() ? null : documents.getFirst().id();
        updateReferenceDocuments(documents, nextActive);
        if (nextActive == null) {
            clearOpenDocument();
        }
    }

    private void selectReferenceDocument(CategoryReferenceDocumentDto document) {
        if (refreshingReferenceDocuments) {
            return;
        }
        if (document == null || viewModel.draft() == null) {
            return;
        }
        if (java.util.Objects.equals(activeReferenceDocument(viewModel.draft()), document.id())
            && viewModel.session().referenceDocument() != null) {
            return;
        }
        updateReferenceDocuments(referenceDocuments(viewModel.draft()), document.id());
        var path = resolveCategoryPath(document.path());
        if (path != null && Files.exists(path)) {
            openDocumentPath(path);
        } else {
            clearOpenDocument();
            status.setText("Reference document is not available: " + document.path());
            refreshAll();
        }
    }

    private void openActiveCategoryReferenceDocument() {
        var document = currentReferenceDocument();
        if (document == null) {
            clearOpenDocument();
            refreshReferenceDocuments();
            return;
        }
        refreshReferenceDocuments();
        var path = resolveCategoryPath(document.path());
        if (path != null && Files.exists(path)) {
            openDocumentPath(path);
        } else {
            clearOpenDocument();
            status.setText("Reference document is not available: " + document.path());
        }
    }

    private CategoryReferenceDocumentDto currentReferenceDocument() {
        var documents = referenceDocuments(viewModel.draft());
        if (documents.isEmpty()) {
            return null;
        }
        var active = activeReferenceDocument(viewModel.draft());
        return documents.stream()
            .filter(document -> java.util.Objects.equals(document.id(), active))
            .findFirst()
            .orElse(documents.getFirst());
    }

    private void updateReferenceDocuments(List<CategoryReferenceDocumentDto> documents, String active) {
        var draft = viewModel.draft();
        if (draft == null) {
            return;
        }
        var normalized = List.copyOf(documents == null ? List.of() : documents);
        var updated = new CategoryDto(
            draft.schemaVersion(),
            draft.id(),
            draft.version(),
            draft.displayName(),
            draft.description(),
            normalized.isEmpty() ? null : new CategoryReferenceDocumentsDto(active, normalized),
            draft.pages(),
            draft.ocr(),
            draft.identification(),
            draft.geometry(),
            draft.anchors(),
            draft.fields()
        );
        viewModel.session().draftCategory(updated);
        rememberCurrentWorkspaceDraft();
        refreshReferenceDocuments();
        refreshAll();
    }

    private List<CategoryReferenceDocumentDto> addReferenceDocument(List<CategoryReferenceDocumentDto> documents, CategoryReferenceDocumentDto document) {
        var updated = new ArrayList<>(documents);
        updated.add(document);
        return List.copyOf(updated);
    }

    private List<CategoryReferenceDocumentDto> referenceDocuments(CategoryDto draft) {
        if (draft == null || draft.referenceDocuments() == null || draft.referenceDocuments().documents() == null) {
            return List.of();
        }
        return draft.referenceDocuments().documents();
    }

    private String activeReferenceDocument(CategoryDto draft) {
        return draft == null || draft.referenceDocuments() == null ? null : draft.referenceDocuments().active();
    }

    private String uniqueReferenceDocumentId(Path path) {
        var base = path.getFileName().toString().replaceFirst("\\.[^.]+$", "").toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "document";
        }
        var existing = referenceDocuments(viewModel.draft()).stream().map(CategoryReferenceDocumentDto::id).collect(java.util.stream.Collectors.toSet());
        var candidate = base;
        var suffix = 2;
        while (existing.contains(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String displayName(Path path) {
        var fileName = path.getFileName().toString();
        return fileName.replaceFirst("\\.[^.]+$", "");
    }

    private String referenceDocumentLabel(CategoryReferenceDocumentDto document) {
        var label = document.displayName() == null || document.displayName().isBlank() ? document.id() : document.displayName();
        return label == null || label.isBlank() ? document.path() : label;
    }

    private Path resolveCategoryPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        var categoryPath = viewModel.session().categoryPath();
        if (categoryPath == null || categoryPath.toAbsolutePath().getParent() == null) {
            return path.toAbsolutePath().normalize();
        }
        return categoryPath.toAbsolutePath().getParent().resolve(path).normalize();
    }

    private void clearOpenDocument() {
        viewModel.session().referenceDocument(null);
        viewModel.session().renderedPageCache().clear();
        viewModel.session().pageCache().clear();
        viewModel.session().currentPage(1);
        viewModel.session().clearDownstreamCaches();
        pageImage.setImage(null);
        viewer.clearOverlay();
        refreshPageStatus();
    }

    private void updateWorkspaceProfile(ProfileDto profile) {
        workspace.profile(profile);
        workspace.dirty(true);
        viewModel.session().clearDownstreamCaches();
    }

    private void refreshWorkspaceProfile() {
        workspace.dirty(true);
        if (profilePreprocessingPanel != null) {
            profilePreprocessingPanel.refresh();
        }
        status.setText("Profile preprocessing changed");
    }

    private void applyWorkspacePreprocessing() {
        if (viewModel.session().renderedPageCache().isEmpty() && viewModel.session().pageCache().isEmpty()) {
            status.setText("Open a document before applying preprocessing");
            return;
        }
        var steps = workspacePreprocessingSteps();
        var refs = steps.stream()
            .map(step -> new ExtensionRef(new ExtensionId(step.id()), step.parameters()))
            .toList();
        status.setText("Applying preprocessing...");
        services.backgroundExecutor().submit(() -> {
            var service = new pl.sk.ocr.core.image.DocumentImagePreprocessingService(services.extensionRegistry());
            var prepared = new java.util.LinkedHashMap<PageNumber, pl.sk.ocr.extension.api.image.ProcessingImage>();
            var source = viewModel.session().renderedPageCache().isEmpty()
                ? viewModel.session().pageCache()
                : viewModel.session().renderedPageCache();
            for (var entry : source.entrySet()) {
                prepared.put(entry.getKey(), service.prepare(entry.getKey(), entry.getValue(), refs));
            }
            return prepared;
        }).whenComplete((prepared, error) -> Platform.runLater(() -> {
            if (error != null) {
                showError(error);
                return;
            }
            viewModel.session().pageCache().clear();
            viewModel.session().pageCache().putAll(prepared);
            viewModel.session().clearDownstreamCaches();
            renderPage();
            refreshAll();
            status.setText("Preprocessing applied");
        }));
    }

    private pl.sk.ocr.extension.api.image.ProcessingImage workspaceDebugSourceImage(Integer stepIndex) {
        var pageNumber = new PageNumber(viewModel.session().currentPage());
        var rendered = viewModel.session().renderedPageCache().get(pageNumber);
        var source = rendered == null ? viewModel.session().pageCache().get(pageNumber) : rendered;
        if (source == null) {
            return null;
        }
        var index = stepIndex == null ? 0 : Math.max(0, stepIndex);
        var previousSteps = workspacePreprocessingSteps().stream()
            .limit(index)
            .filter(step -> step.id() != null && !step.id().isBlank())
            .map(step -> new ExtensionRef(new ExtensionId(step.id()), step.parameters()))
            .toList();
        if (previousSteps.isEmpty()) {
            return source;
        }
        var service = new pl.sk.ocr.core.image.DocumentImagePreprocessingService(services.extensionRegistry());
        return service.prepare(pageNumber, source, previousSteps);
    }

    private pl.sk.ocr.extension.api.image.ProcessingImage fieldImageProcessorDebugSource(Integer fieldIndex, Integer stepIndex) {
        var field = field(fieldIndex == null ? -1 : fieldIndex);
        if (field == null || field.region() == null) {
            return null;
        }
        var pageNumber = new PageNumber(field.page() == null ? viewModel.session().currentPage() : field.page());
        var source = viewModel.session().pageCache().get(pageNumber);
        if (source == null) {
            return null;
        }
        var region = field.region();
        var cropped = new pl.sk.ocr.core.image.BufferedProcessingImage(source.asBufferedImage())
            .crop(new pl.sk.ocr.domain.geometry.Region(region.x(), region.y(), region.width(), region.height()));
        var index = stepIndex == null ? 0 : Math.max(0, stepIndex);
        var previousSteps = (field.imageProcessors() == null ? List.<ExtensionRefDto>of() : field.imageProcessors()).stream()
            .limit(index)
            .filter(step -> step.id() != null && !step.id().isBlank())
            .map(step -> new ExtensionRef(new ExtensionId(step.id()), step.parameters()))
            .toList();
        if (previousSteps.isEmpty()) {
            return cropped;
        }
        var service = new pl.sk.ocr.core.image.DocumentImagePreprocessingService(services.extensionRegistry());
        return service.prepare(pageNumber, cropped, previousSteps);
    }

    private boolean hasWorkspacePreprocessingSteps() {
        return !workspacePreprocessingSteps().isEmpty();
    }

    private List<ExtensionRefDto> workspacePreprocessingSteps() {
        var profile = workspace.profile();
        return profile == null || profile.preprocessing() == null || profile.preprocessing().imageProcessors() == null
            ? List.of()
            : profile.preprocessing().imageProcessors();
    }

    private Path resolveProfilePath(Path profilePath, String value) {
        var path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return profilePath.toAbsolutePath().getParent().resolve(path).normalize();
    }

    private String relativize(Path base, Path path) {
        if (base == null || path == null) {
            return "categories";
        }
        try {
            return base.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return path.toAbsolutePath().normalize().toString();
        }
    }

    private String fileName(String categoryId) {
        var id = categoryId == null || categoryId.isBlank() ? "new-category" : categoryId.trim();
        return id + ".json";
    }

    private void openDocumentPath(Path path) {
        status.setText("Opening document...");
        viewModel.openReferenceDocument(path, preferences.renderOptions())
            .whenComplete((ignored, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error);
                } else {
                    if (hasWorkspacePreprocessingSteps()) {
                        applyWorkspacePreprocessing();
                    } else {
                        renderPage();
                        refreshAll();
                    }
                }
            }));
    }

    private void showMissingRecentFile(Path path) {
        var alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Recent File");
        alert.setHeaderText("File is not available");
        alert.setContentText(path.toString());
        alert.showAndWait();
    }

    private boolean confirmUnsavedChanges(Stage stage) {
        if (viewModel == null || (!workspace.dirty() && (viewModel.draft() == null || !viewModel.session().dirty()))) {
            return true;
        }
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Current profile workspace has unsaved changes.");
        alert.setContentText("Save changes before continuing?");
        var save = new ButtonType("Save", ButtonBar.ButtonData.YES);
        var discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        var cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        var choice = alert.showAndWait().orElse(cancel);
        if (choice == save) {
            return saveProfile(stage, false);
        }
        return choice == discard;
    }

    private void runOcr() {
        status.setText("Running OCR...");
        viewModel.runCurrentPageOcr(preferences.ocrSettings())
            .whenComplete((ocr, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error);
                } else {
                    renderOcrOverlay();
                    refreshAll();
                }
            }));
    }

    private void previewField() {
        var fieldIndex = selectedPreviewFieldIndex();
        if (fieldIndex < 0) {
            status.setText("Select a field to preview");
            return;
        }
        commitCurrentDetailsForm();
        status.setText("Running field preview...");
        viewModel.previewField(fieldIndex)
            .whenComplete((preview, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error);
                } else {
                    refreshAll();
                    fieldResultPanel.refresh();
                    traceViewerPanel.refresh();
                }
            }));
    }

    private void testCategory() {
        commitCurrentDetailsForm();
        status.setText("Running category test...");
        viewModel.testCategory(preferences.ocrSettings())
            .whenComplete((result, error) -> Platform.runLater(() -> {
                if (error != null) {
                    showError(error);
                } else {
                    viewModel.session().latestCategoryTestResults(List.of());
                    refreshAll();
                    categoryTestResultPanel.refresh();
                    traceViewerPanel.refresh();
                }
            }));
    }

    private void testAllReferenceDocuments() {
        commitCurrentDetailsForm();
        var draft = viewModel.draft();
        var documents = referenceDocuments(draft);
        if (draft == null) {
            status.setText("No category draft is open");
            return;
        }
        if (documents.isEmpty()) {
            status.setText("No reference documents configured");
            return;
        }
        var settings = preferences.ocrSettings();
        var renderOptions = preferences.renderOptions();
        var steps = workspacePreprocessingSteps();
        status.setText("Running category test for " + documents.size() + " reference document(s)...");
        services.backgroundExecutor().submit(() -> {
            var results = new ArrayList<CategoryReferenceDocumentTestResult>();
            for (var document : documents) {
                var path = resolveCategoryPath(document.path());
                if (path == null || !Files.exists(path)) {
                    results.add(new CategoryReferenceDocumentTestResult(document.id(), document.path(), path,
                        failedReferenceDocumentResult(document, "Reference document is not available")));
                    continue;
                }
                try {
                    var rendered = services.documentReader().read(path, renderOptions);
                    var pages = prepareReferenceDocumentPages(rendered.pages(), steps);
                    var result = services.testCategory().test(draft, path, pages, new InMemoryTraceImageStore(), settings);
                    results.add(new CategoryReferenceDocumentTestResult(document.id(), document.path(), path, result));
                } catch (RuntimeException e) {
                    results.add(new CategoryReferenceDocumentTestResult(document.id(), document.path(), path,
                        failedReferenceDocumentResult(document, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
                }
            }
            return List.copyOf(results);
        }).whenComplete((results, error) -> Platform.runLater(() -> {
            if (error != null) {
                showError(error);
                return;
            }
            viewModel.session().latestCategoryTestResults(results);
            var first = results.isEmpty() ? null : results.getFirst().result();
            viewModel.session().latestDocumentResult(first);
            viewModel.session().latestTrace(first == null ? ProcessingTrace.off() : first.trace());
            refreshAll();
            categoryTestResultPanel.refresh();
            traceViewerPanel.refresh();
            status.setText("Category test finished for " + results.size() + " reference document(s)");
        }));
    }

    private Map<PageNumber, pl.sk.ocr.extension.api.image.ProcessingImage> prepareReferenceDocumentPages(
        Map<PageNumber, pl.sk.ocr.extension.api.image.ProcessingImage> rendered,
        List<ExtensionRefDto> steps
    ) {
        if (steps == null || steps.isEmpty()) {
            return rendered;
        }
        var refs = steps.stream()
            .map(step -> new ExtensionRef(new ExtensionId(step.id()), step.parameters()))
            .toList();
        var service = new pl.sk.ocr.core.image.DocumentImagePreprocessingService(services.extensionRegistry());
        var prepared = new java.util.LinkedHashMap<PageNumber, pl.sk.ocr.extension.api.image.ProcessingImage>();
        for (var entry : rendered.entrySet()) {
            prepared.put(entry.getKey(), service.prepare(entry.getKey(), entry.getValue(), refs));
        }
        return prepared;
    }

    private DocumentResult failedReferenceDocumentResult(CategoryReferenceDocumentDto document, String message) {
        var documentId = document.path() == null || document.path().isBlank() ? document.id() : document.path();
        var issue = ProcessingIssue.error(
            new IssueCode("REFERENCE_DOCUMENT_TEST_FAILED"),
            ErrorScope.DOCUMENT,
            ProcessingStage.DOCUMENT_LOADING,
            message
        );
        return DocumentResult.from(new DocumentId(documentId), null, List.of(), List.of(issue), ProcessingTrace.off());
    }

    private void validate() {
        viewModel.validate();
        refreshAll();
    }

    private void exportSelectedTraceImage(Stage stage) {
        var directory = chooseExportDirectory(stage);
        if (directory == null) {
            return;
        }
        status.setText("Exporting selected trace image...");
        services.backgroundExecutor().submit(() -> diagnosticExport.exportSelectedImage(directory,
                viewModel.session().latestTrace(), viewModel.session().traceImageStore(), traceViewerPanel.selectedImageRefs()))
            .whenComplete((result, error) -> Platform.runLater(() -> finishExport(result, error)));
    }

    private void exportAllTraceImages(Stage stage) {
        var directory = chooseExportDirectory(stage);
        if (directory == null) {
            return;
        }
        status.setText("Exporting trace images...");
        services.backgroundExecutor().submit(() -> diagnosticExport.exportAllImages(directory,
                viewModel.session().latestTrace(), viewModel.session().traceImageStore()))
            .whenComplete((result, error) -> Platform.runLater(() -> finishExport(result, error)));
    }

    private void exportTraceMetadata(Stage stage) {
        var directory = chooseExportDirectory(stage);
        if (directory == null) {
            return;
        }
        status.setText("Exporting trace metadata...");
        services.backgroundExecutor().submit(() -> diagnosticExport.exportMetadata(directory, viewModel.session().latestTrace()))
            .whenComplete((result, error) -> Platform.runLater(() -> finishExport(result, error)));
    }

    private void exportTraceBundle(Stage stage) {
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP bundle", "*.zip"));
        chooser.setInitialFileName("diagnostic-bundle.zip");
        configureInitialDirectory(chooser, DirectoryKey.EXPORT_DOCUMENT);
        var file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        preferences.rememberFile(DirectoryKey.EXPORT_DOCUMENT, file.toPath());
        status.setText("Exporting diagnostic bundle...");
        services.backgroundExecutor().submit(() -> diagnosticExport.exportBundle(file.toPath(),
                viewModel.session().latestTrace(), viewModel.session().traceImageStore()))
            .whenComplete((result, error) -> Platform.runLater(() -> finishExport(result, error)));
    }

    private Path chooseExportDirectory(Stage stage) {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Select diagnostics export folder");
        preferences.directory(DirectoryKey.EXPORT_DOCUMENT).map(Path::toFile).ifPresent(chooser::setInitialDirectory);
        var directory = chooser.showDialog(stage);
        if (directory != null) {
            preferences.rememberFile(DirectoryKey.EXPORT_DOCUMENT, directory.toPath());
        }
        return directory == null ? null : directory.toPath();
    }

    private void configureInitialDirectory(FileChooser chooser, DirectoryKey key) {
        preferences.directory(key).map(Path::toFile).ifPresent(chooser::setInitialDirectory);
    }

    private void showSettings() {
        var before = preferences.settings();
        new SettingsDialog().show(before).ifPresent(settings -> {
            var ocrChanged = !java.util.Objects.equals(before.tesseractDatapath(), settings.tesseractDatapath())
                || !java.util.Objects.equals(before.defaultOcrLanguage(), settings.defaultOcrLanguage());
            preferences.save(settings);
            if (ocrChanged) {
                viewModel.session().ocrCache().clear();
                viewModel.session().latestFieldResult(null);
                viewModel.session().latestDocumentResult(null);
                viewModel.session().latestTrace(pl.sk.ocr.domain.trace.ProcessingTrace.off());
                viewModel.session().traceImageStore().clear();
                refreshAll();
            }
            status.setText("Settings saved");
        });
    }

    private void showLoadedExtensions() {
        new LoadedExtensionsDialog().show(services.extensionRegistry());
    }

    private void showAbout() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("OCR Configurator");
        alert.setContentText("OCR Configurator 0.1.0-SNAPSHOT");
        alert.showAndWait();
    }

    private void finishExport(DiagnosticExportUseCase.ExportResult result, Throwable error) {
        if (error != null) {
            showError(error);
            return;
        }
        status.setText("Diagnostics exported: " + result.files().size() + " file(s) to " + result.target());
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
        renderSelectedFieldOverlay();
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

    private void renderSelectedFieldOverlay() {
        var selected = selectedTreeNode();
        if (selected == null || selected.type() != TreeNodeType.FIELD) {
            return;
        }
        var field = field(selected.index());
        if (field == null || field.region() == null || field.page() != null && field.page() != viewModel.session().currentPage()) {
            return;
        }
        var rectangle = regionRectangle(field.region(), Color.color(0.50, 0.20, 0.82, 0.12), "#7c3aed", 2.0);
        viewer.addOverlay(rectangle);
        viewer.editableRegion(rectangle, RegionTargetType.FIELD_REGION);
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
        refreshValidationTable();
        if (profilePreprocessingPanel != null) {
            profilePreprocessingPanel.refresh();
        }
        categoryTestResultPanel.refresh();
        fieldResultPanel.refresh();
        traceViewerPanel.refresh();
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
            detailsInfo.setText("Create or open a category configuration.");
            propertiesPanel.showEmpty();
            refreshingDetails = false;
            return;
        }
        detailsInfo.setText("Dirty=" + viewModel.session().dirty()
            + " | Profile dirty=" + workspace.dirty()
            + " | Reference document=" + (viewModel.session().referenceDocument() == null ? "" : viewModel.session().referenceDocument()));
        propertiesPanel.refreshActive();
        renderOcrOverlay();
        refreshingDetails = false;
    }

    private void configureCategoryDetailsForm() {
        detailsInfo.setWrapText(true);
        detailsInfo.setStyle("-fx-text-fill: #111827;");
    }

    private VBox emptyDetailsForm() {
        var section = section("Category");
        var message = new Label("Create or open a category configuration.");
        message.setWrapText(true);
        installTooltip(message, "No category draft is currently open.");
        section.getChildren().add(message);
        return new VBox(10, section, detailsInfo);
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
        } else if (targetType == RegionTargetType.FIELD_REGION) {
            fieldPropertiesPanel.updateFieldRegionFromViewer(region, commit);
        }
    }

    private void applyDrawnRegion(RegionDto region) {
        if (regionEditTarget == null) {
            return;
        }
        runUiSafe(() -> {
            if (regionEditTarget.type() == RegionTargetType.FIELD_REGION) {
                fieldPropertiesPanel.replaceFieldRegion(regionEditTarget.index(), region);
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
        rememberCurrentWorkspaceDraft();
        status.setText(viewModel.status());
        refreshValidationTable();
        detailsInfo.setText("Dirty=" + viewModel.session().dirty()
            + " | Profile dirty=" + workspace.dirty()
            + " | Reference document=" + (viewModel.session().referenceDocument() == null ? "" : viewModel.session().referenceDocument()));
        renderOcrOverlay();
    }

    private void refreshValidationTable() {
        validationTable.getItems().setAll(viewModel.validationProblems().stream()
            .map(ValidationRow::from)
            .toList());
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return null;
        }
        return Integer.parseInt(text);
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

    private TreeNodeType selectedNodeType() {
        var selected = selectedTreeNode();
        return selected == null ? TreeNodeType.ROOT : selected.type();
    }

    private ConfigurationTreeNode selectedTreeNode() {
        var selected = configurationTree.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getValue();
    }

    private int selectedFieldIndex() {
        var selected = selectedTreeNode();
        return selected == null || selected.type() != TreeNodeType.FIELD ? -1 : selected.index();
    }

    private int selectedPreviewFieldIndex() {
        var selection = fieldSelection();
        return selection.fieldIndex();
    }

    private FieldPropertiesPanel.Selection fieldSelection() {
        var selected = selectedTreeNode();
        if (selected == null) {
            return new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.FIELDS, -1, -1, null);
        }
        return switch (selected.type()) {
            case FIELD -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.FIELD, selected.index(), -1, null);
            case FIELD_OCR -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.FIELD_OCR, selected.index(), -1, null);
            case FIELD_OUTPUT -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.FIELD_OUTPUT, selected.index(), -1, null);
            case FIELD_IMAGE_PROCESSORS -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.IMAGE_PROCESSORS, selected.index(), -1, Pipeline.IMAGE_PROCESSORS);
            case FIELD_TRANSFORMERS -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.TRANSFORMERS, selected.index(), -1, Pipeline.TRANSFORMERS);
            case FIELD_VALIDATORS -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.VALIDATORS, selected.index(), -1, Pipeline.VALIDATORS);
            case PIPELINE_STEP -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.PIPELINE_STEP, selected.index(), selected.childIndex(), pipelineFromNodeId(selected.id()));
            default -> new FieldPropertiesPanel.Selection(FieldPropertiesPanel.SelectionType.FIELDS, -1, -1, null);
        };
    }

    private Pipeline pipelineFromNodeId(String id) {
        if (id != null && id.contains(".imageProcessors.")) {
            return Pipeline.IMAGE_PROCESSORS;
        }
        if (id != null && id.contains(".validators.")) {
            return Pipeline.VALIDATORS;
        }
        return Pipeline.TRANSFORMERS;
    }

    private pl.sk.ocr.config.dto.FieldDto field(int index) {
        var fieldList = fields();
        return index < 0 || index >= fieldList.size() ? null : fieldList.get(index);
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

    private void navigateToValidationProblem(String path) {
        var nodeId = treeNodeIdForValidationPath(path);
        if (nodeId == null || configurationTree.getRoot() == null) {
            status.setText("Validation path has no editor mapping: " + path);
            return;
        }
        if (selectTreeNodeAndExpandParents(configurationTree.getRoot(), nodeId)) {
            configurationTree.scrollTo(configurationTree.getRow(configurationTree.getSelectionModel().getSelectedItem()));
            refreshDetails();
            status.setText("Selected validation target: " + path);
        } else {
            status.setText("Validation target not found: " + path);
        }
    }

    private String treeNodeIdForValidationPath(String path) {
        if (path == null || path.isBlank() || "$".equals(path) || "$.id".equals(path) || "$.displayName".equals(path)
            || "$.description".equals(path) || "$.version".equals(path) || path.startsWith("$.pages")) {
            return "root";
        }
        var groupCondition = Pattern.compile("^\\$\\.identification\\.groups\\[(\\d+)]\\.conditions\\[(\\d+)].*").matcher(path);
        if (groupCondition.matches()) {
            return "identification.group." + groupCondition.group(1) + ".condition." + groupCondition.group(2);
        }
        var group = Pattern.compile("^\\$\\.identification\\.groups\\[(\\d+)].*").matcher(path);
        if (group.matches()) {
            return "identification.group." + group.group(1);
        }
        if (path.startsWith("$.identification")) {
            return "identification";
        }
        var anchor = Pattern.compile("^\\$\\.anchors\\[(\\d+)].*").matcher(path);
        if (anchor.matches()) {
            return "anchor." + anchor.group(1);
        }
        if (path.startsWith("$.anchors")) {
            return "anchors";
        }
        if (path.startsWith("$.geometry.strategy")) {
            return "geometry.strategy";
        }
        if (path.startsWith("$.geometry")) {
            return "geometry";
        }
        var pipeline = Pattern.compile("^\\$\\.fields\\[(\\d+)]\\.(imageProcessors|transformers|validators)\\[(\\d+)].*").matcher(path);
        if (pipeline.matches()) {
            return "field." + pipeline.group(1) + "." + pipeline.group(2) + "." + pipeline.group(3);
        }
        var fieldChild = Pattern.compile("^\\$\\.fields\\[(\\d+)]\\.(ocr|output).*").matcher(path);
        if (fieldChild.matches()) {
            return "field." + fieldChild.group(1) + "." + fieldChild.group(2);
        }
        var field = Pattern.compile("^\\$\\.fields\\[(\\d+)].*").matcher(path);
        if (field.matches()) {
            return "field." + field.group(1);
        }
        if (path.startsWith("$.fields")) {
            return "fields";
        }
        return null;
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

    private void runUiSafe(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void showError(Throwable error) {
        var cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null ? error.getCause() : error;
        cause.printStackTrace(System.err);
        var message = cause.getMessage() == null || cause.getMessage().isBlank() ? cause.getClass().getSimpleName() : cause.getMessage();
        status.setText("Error: " + message);
        var alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Operation failed");
        alert.setContentText(message + System.lineSeparator() + System.lineSeparator() + "Type: " + cause.getClass().getName());
        alert.showAndWait();
    }

    public record ValidationRow(String severity, String code, String path, String message) {
        static ValidationRow from(DraftValidationProblem problem) {
            return new ValidationRow("ERROR", problem.code(), problem.path(), problem.message());
        }

        public String getSeverity() {
            return severity;
        }

        public String getCode() {
            return code;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
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

    enum TreeNodeType {
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

    enum ViewerMode {
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
