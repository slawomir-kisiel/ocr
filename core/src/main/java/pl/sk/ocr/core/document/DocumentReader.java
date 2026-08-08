package pl.sk.ocr.core.document;

import java.nio.file.Path;

public interface DocumentReader {
    RenderedDocument read(Path source, RenderOptions options);
}
