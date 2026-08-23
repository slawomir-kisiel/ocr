package pl.sk.ocr.configurator.app;

import java.nio.file.Path;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.document.RenderedDocument;

public final class OpenReferenceDocumentUseCase {
    private final DocumentReader documentReader;

    public OpenReferenceDocumentUseCase(DocumentReader documentReader) {
        this.documentReader = documentReader;
    }

    public RenderedDocument open(Path path) {
        return documentReader.read(path, RenderOptions.defaults());
    }

    public RenderedDocument open(Path path, RenderOptions options) {
        return documentReader.read(path, options == null ? RenderOptions.defaults() : options);
    }
}
