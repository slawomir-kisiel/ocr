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
        return extensionDescriptor("text", ExtensionType.DETECTOR, "Text", "Returns available OCR text with whole image bounds.");
    }

    @Override
    public DetectionResult detect(DetectionRequest request, DetectorContext context) {
        var text = request.text() == null ? "" : request.text();
        return text.isBlank() ? notDetected("No text available") : detectedText(text, new pl.sk.ocr.domain.geometry.Region(0, 0,
            request.image().width(), request.image().height()), 1.0, "Text present");
    }
}

