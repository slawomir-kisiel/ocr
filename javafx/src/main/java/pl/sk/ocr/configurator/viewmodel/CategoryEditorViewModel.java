package pl.sk.ocr.configurator.viewmodel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.app.RunPageOcrUseCase;
import pl.sk.ocr.configurator.async.BackgroundExecutor;
import pl.sk.ocr.configurator.async.PreviewRunGuard;
import pl.sk.ocr.configurator.session.ConfigurationSession;
import pl.sk.ocr.configurator.validation.DraftValidationProblem;
import pl.sk.ocr.configurator.validation.DraftValidationService;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.OcrText;

public final class CategoryEditorViewModel {
    private final ConfigurationSession session = new ConfigurationSession();
    private final ConfigurationFileService fileService;
    private final OpenReferenceDocumentUseCase openDocument;
    private final RunPageOcrUseCase runOcr;
    private final DraftValidationService validationService;
    private final BackgroundExecutor backgroundExecutor;
    private final PreviewRunGuard previewRunGuard = new PreviewRunGuard();
    private final AtomicReference<String> status = new AtomicReference<>("Ready");
    private final AtomicReference<List<DraftValidationProblem>> validationProblems = new AtomicReference<>(List.of());

    public CategoryEditorViewModel(ConfigurationFileService fileService, OpenReferenceDocumentUseCase openDocument,
                                   RunPageOcrUseCase runOcr, DraftValidationService validationService,
                                   BackgroundExecutor backgroundExecutor) {
        this.fileService = fileService;
        this.openDocument = openDocument;
        this.runOcr = runOcr;
        this.validationService = validationService;
        this.backgroundExecutor = backgroundExecutor;
    }

    public ConfigurationSession session() {
        return session;
    }

    public String status() {
        return status.get();
    }

    public List<DraftValidationProblem> validationProblems() {
        return validationProblems.get();
    }

    public void newCategory(String id, String displayName) {
        session.draftCategory(fileService.newCategory(id, displayName));
        validationProblems.set(validate());
        status.set("New category draft");
    }

    public void loadCategory(Path path) {
        var draft = fileService.loadCategory(path);
        session.categoryPath(path);
        session.draftCategory(draft);
        session.markSaved();
        validationProblems.set(validate());
        status.set("Loaded category: " + path.getFileName());
    }

    public void saveCategory(Path path) {
        fileService.saveCategory(path, session.draftCategory());
        session.categoryPath(path);
        session.markSaved();
        validationProblems.set(validate());
        status.set("Saved category: " + path.getFileName());
    }

    public CompletionStage<Void> openReferenceDocument(Path path) {
        status.set("Opening document...");
        return backgroundExecutor.submit(() -> openDocument.open(path)).thenAccept(rendered -> {
            session.referenceDocument(path);
            session.pageCache().clear();
            session.pageCache().putAll(rendered.pages());
            session.currentPage(1);
            status.set("Document opened: " + path.getFileName());
        });
    }

    public CompletionStage<OcrText> runCurrentPageOcr() {
        var runId = previewRunGuard.next();
        var pageNumber = new PageNumber(session.currentPage());
        var image = session.pageCache().get(pageNumber);
        if (image == null) {
            throw new IllegalStateException("No rendered page is available");
        }
        status.set("Running OCR...");
        return backgroundExecutor.submit(() -> runOcr.run(image, OcrSettings.defaults())).thenApply(result -> {
            if (previewRunGuard.isLatest(runId)) {
                session.ocrCache().put(pageNumber, result);
                status.set("OCR ready: " + result.words().size() + " words");
                return result;
            }
            return session.ocrCache().get(pageNumber);
        });
    }

    public List<DraftValidationProblem> validate() {
        var problems = validationService.validate(session.draftCategory());
        validationProblems.set(problems);
        status.set(problems.isEmpty() ? "Configuration valid" : "Configuration has " + problems.size() + " problems");
        return problems;
    }

    public CategoryDto draft() {
        return session.draftCategory();
    }
}
