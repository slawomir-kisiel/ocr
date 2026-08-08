package pl.sk.ocr.core.document;

import java.util.Map;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public record RenderedDocument(Map<PageNumber, ProcessingImage> pages) {
    public RenderedDocument {
        pages = Map.copyOf(Validation.requireNonNull(pages, "pages"));
    }

    public ProcessingImage requirePage(PageNumber pageNumber) {
        var page = pages.get(pageNumber);
        if (page == null) {
            throw new IllegalArgumentException("Rendered page is missing: " + pageNumber.value());
        }
        return page;
    }
}
