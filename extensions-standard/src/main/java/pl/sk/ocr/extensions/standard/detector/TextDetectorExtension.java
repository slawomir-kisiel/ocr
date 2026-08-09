package pl.sk.ocr.extensions.standard.detector;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.stringParameter;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectorContext;

public final class TextDetectorExtension extends AbstractDetectorExtension {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("text", ExtensionType.DETECTOR, "Text", "Detects expected text in OCR text and returns whole image bounds.",
            stringParameter("text", "Text", "Text that should be present in OCR output.", false, ""));
    }

    @Override
    public DetectionResult detect(DetectionRequest request, DetectorContext context) {
        var expected = request.parameters().getString("text").orElse("");
        var text = request.text() == null ? "" : request.text();
        if (expected.isBlank()) {
            return text.isBlank() ? notDetected("No text available") : detectedWholeImage(request.image(), "Text present");
        }
        return text.contains(expected) ? detectedWholeImage(request.image(), "Expected text found") : notDetected("Expected text not found");
    }
}

