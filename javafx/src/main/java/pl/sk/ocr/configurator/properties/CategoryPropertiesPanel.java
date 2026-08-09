package pl.sk.ocr.configurator.properties;

import static pl.sk.ocr.configurator.ui.FormControls.*;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.sk.ocr.config.dto.OcrSettingsDto;
import pl.sk.ocr.config.dto.PageSelectionDto;
import pl.sk.ocr.configurator.viewmodel.CategoryEditorViewModel;

public final class CategoryPropertiesPanel implements DetailsPanel {
    private static final String PAGE_TYPE_SINGLE = "SINGLE";
    private static final String PAGE_TYPE_RANGE = "RANGE";
    private static final String PAGE_TYPE_LIST = "LIST";
    private static final String PAGE_TYPE_ALL = "ALL";

    private final CategoryEditorViewModel viewModel;
    private final Label detailsInfo;
    private final Runnable afterChange;
    private final Runnable afterPagesChange;
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
    private boolean refreshing;

    public CategoryPropertiesPanel(CategoryEditorViewModel viewModel, Label detailsInfo, Runnable afterChange,
                                   Runnable afterPagesChange) {
        this.viewModel = viewModel;
        this.detailsInfo = detailsInfo;
        this.afterChange = afterChange;
        this.afterPagesChange = afterPagesChange;
        configure();
    }

    @Override
    public Node view() {
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

    @Override
    public void refresh() {
        refreshing = true;
        try {
            var draft = viewModel.draft();
            if (draft == null) {
                clear();
                return;
            }
            categoryId.setText(nullToEmpty(draft.id()));
            categoryDisplayName.setText(nullToEmpty(draft.displayName()));
            categoryDescription.setText(nullToEmpty(draft.description()));
            categoryVersion.setText(nullToEmpty(draft.version()));
            var pages = draft.pages();
            selectPageType(pages == null || pages.type() == null ? PAGE_TYPE_SINGLE : pages.type());
            pageNumber.setText(pages == null || pages.page() == null ? "" : pages.page().toString());
            pageFrom.setText(pages == null || pages.from() == null ? "" : pages.from().toString());
            pageTo.setText(pages == null || pages.to() == null ? "" : pages.to().toString());
            pageList.setText(pages == null || pages.pages() == null ? "" : pages.pages().stream()
                .map(String::valueOf).toList().toString().replace("[", "").replace("]", ""));
            var ocr = draft.ocr();
            ocrLanguage.setText(ocr == null ? "" : nullToEmpty(ocr.language()));
            ocrDatapath.setText(ocr == null ? "" : nullToEmpty(ocr.datapath()));
            updatePagePolicyFieldsVisibility();
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void commit() {
        applyCategoryMetadata();
        applyPages();
        applyOcrDefaults();
    }

    public void clear() {
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

    private void configure() {
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

    private void updatePagePolicyFieldsVisibility() {
        var selected = selectedPageType();
        setVisibleManaged(pageNumberField, PAGE_TYPE_SINGLE.equals(selected));
        setVisibleManaged(pageFromField, PAGE_TYPE_RANGE.equals(selected));
        setVisibleManaged(pageToField, PAGE_TYPE_RANGE.equals(selected));
        setVisibleManaged(pageListField, PAGE_TYPE_LIST.equals(selected));
    }

    private void applyCategoryMetadata() {
        if (refreshing || viewModel.draft() == null) {
            return;
        }
        viewModel.updateCategoryMetadata(categoryId.getText(), categoryDisplayName.getText(),
            categoryDescription.getText(), categoryVersion.getText());
        afterChange.run();
    }

    private void applyPages() {
        if (refreshing || viewModel.draft() == null) {
            return;
        }
        viewModel.updatePages(new PageSelectionDto(selectedPageType(), parseInteger(pageNumber.getText()),
            parseInteger(pageFrom.getText()), parseInteger(pageTo.getText()), parseIntegerList(pageList.getText())));
        afterPagesChange.run();
    }

    private void applyOcrDefaults() {
        if (refreshing || viewModel.draft() == null) {
            return;
        }
        viewModel.updateOcr(new OcrSettingsDto(blankToNull(ocrLanguage.getText()), blankToNull(ocrDatapath.getText())));
        afterChange.run();
    }

    private String selectedPageType() {
        var selected = pageType.getSelectedToggle();
        return selected == null || selected.getUserData() == null ? PAGE_TYPE_SINGLE : selected.getUserData().toString();
    }

    private void selectPageType(String type) {
        switch (type) {
            case PAGE_TYPE_RANGE -> pageType.selectToggle(pageTypeRange);
            case PAGE_TYPE_LIST -> pageType.selectToggle(pageTypeList);
            case PAGE_TYPE_ALL -> pageType.selectToggle(pageTypeAll);
            default -> pageType.selectToggle(pageTypeSingle);
        }
    }

    private Integer parseInteger(String value) {
        var text = blankToNull(value);
        return text == null ? null : Integer.parseInt(text);
    }

    private List<Integer> parseIntegerList(String value) {
        var text = blankToNull(value);
        if (text == null) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .map(Integer::parseInt)
            .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
