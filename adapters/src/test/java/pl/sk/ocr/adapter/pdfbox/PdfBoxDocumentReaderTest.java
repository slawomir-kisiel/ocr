package pl.sk.ocr.adapter.pdfbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.sk.ocr.core.document.RenderOptions;
import pl.sk.ocr.domain.identifier.PageNumber;

class PdfBoxDocumentReaderTest {
    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void rendersFirstPdfPage() throws Exception {
        var pdf = tempDir.resolve("simple-document.pdf");
        try (var document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(Files.newOutputStream(pdf));
        }

        var rendered = new PdfBoxDocumentReader().read(pdf, new RenderOptions(72));

        assertThat(rendered.pages()).containsKey(new PageNumber(1));
        assertThat(rendered.requirePage(new PageNumber(1)).width()).isGreaterThan(0);
    }
}
