package pl.sk.ocr.adapter.pdfbox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import pl.sk.ocr.core.document.DocumentReader;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class PdfBoxDocumentReader implements DocumentReader {
    @Override
    public RenderedDocument read(Path source, RenderOptions options) {
        try (var document = Loader.loadPDF(source.toFile())) {
            if (document.getNumberOfPages() < 1) {
                throw new IllegalArgumentException("PDF does not contain pages: " + source);
            }
            var renderer = new PDFRenderer(document);
            var pages = new LinkedHashMap<PageNumber, ProcessingImage>();
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                var image = renderer.renderImageWithDPI(index, options.dpi(), ImageType.RGB);
                pages.put(new PageNumber(index + 1), new BufferedProcessingImage(image));
            }
            return new RenderedDocument(pages);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot render PDF: " + source, e);
        }
    }
}
