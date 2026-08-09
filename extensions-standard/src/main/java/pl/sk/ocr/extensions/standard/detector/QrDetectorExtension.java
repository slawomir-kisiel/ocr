package pl.sk.ocr.extensions.standard.detector;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectorContext;

public final class QrDetectorExtension extends AbstractDetectorExtension {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("qr", ExtensionType.DETECTOR, "QR", "Standard QR detector placeholder. Use ZXing adapter for physical QR decoding.");
    }

    @Override
    public DetectionResult detect(DetectionRequest request, DetectorContext context) {
        return notDetected("QR decoding is provided by adapter-specific detector implementations.");
    }
}

