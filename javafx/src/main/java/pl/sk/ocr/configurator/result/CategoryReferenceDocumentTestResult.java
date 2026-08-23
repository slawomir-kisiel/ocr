package pl.sk.ocr.configurator.result;

import java.nio.file.Path;
import pl.sk.ocr.domain.result.DocumentResult;

public record CategoryReferenceDocumentTestResult(
    String referenceDocumentId,
    String referenceDocumentPath,
    Path resolvedPath,
    DocumentResult result
) {
}
