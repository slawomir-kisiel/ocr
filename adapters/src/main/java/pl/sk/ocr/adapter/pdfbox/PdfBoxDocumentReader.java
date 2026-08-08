package pl.sk.ocr.adapter.pdfbox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.domain.identifier.PageNumber;

public final class PdfBoxDocumentReader implements DocumentReader {
    @Override
    public RenderedDocument read(Path source, RenderOptions options) {
        try (var document = Loader.loadPDF(source.toFile())) {
            if (document.getNumberOfPages() < 1) {
                throw new IllegalArgumentException("PDF does not contain pages: " + source);
            }
            var renderer = new PDFRenderer(document);
            var image = renderer.renderImageWithDPI(0, options.dpi(), ImageType.RGB);
            return new RenderedDocument(Map.of(new PageNumber(1), new BufferedProcessingImage(image)));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot render PDF: " + source, e);
        }
    }
}
