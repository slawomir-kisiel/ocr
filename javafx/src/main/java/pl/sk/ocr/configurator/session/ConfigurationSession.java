package pl.sk.ocr.configurator.session;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.OcrText;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class ConfigurationSession {
    private Path categoryPath;
    private Path referenceDocument;
    private CategoryDto draftCategory;
    private int currentPage = 1;
    private boolean dirty;
    private ProcessingTrace latestTrace = ProcessingTrace.off();
    private final Map<PageNumber, ProcessingImage> pageCache = new ConcurrentHashMap<>();
    private final Map<PageNumber, OcrText> ocrCache = new ConcurrentHashMap<>();

    public Path categoryPath() {
        return categoryPath;
    }

    public void categoryPath(Path categoryPath) {
        this.categoryPath = categoryPath;
    }

    public Path referenceDocument() {
        return referenceDocument;
    }

    public void referenceDocument(Path referenceDocument) {
        this.referenceDocument = referenceDocument;
    }

    public CategoryDto draftCategory() {
        return draftCategory;
    }

    public void draftCategory(CategoryDto draftCategory) {
        this.draftCategory = draftCategory;
        this.dirty = true;
        clearDownstreamCaches();
    }

    public int currentPage() {
        return currentPage;
    }

    public void currentPage(int currentPage) {
        this.currentPage = Math.max(1, currentPage);
    }

    public boolean dirty() {
        return dirty;
    }

    public void markSaved() {
        this.dirty = false;
    }

    public ProcessingTrace latestTrace() {
        return latestTrace;
    }

    public void latestTrace(ProcessingTrace latestTrace) {
        this.latestTrace = latestTrace == null ? ProcessingTrace.off() : latestTrace;
    }

    public Map<PageNumber, ProcessingImage> pageCache() {
        return pageCache;
    }

    public Map<PageNumber, OcrText> ocrCache() {
        return ocrCache;
    }

    public void clearDownstreamCaches() {
        ocrCache.clear();
        latestTrace = ProcessingTrace.off();
    }
}
