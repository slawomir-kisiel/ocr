package pl.sk.ocr.extensions.standard.detector;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.detector.DetectionRequest;
import pl.sk.ocr.extension.api.detector.DetectionResult;
import pl.sk.ocr.extension.api.detector.DetectorContext;

public final class BarcodeDetectorExtension extends AbstractDetectorExtension {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("barcode", ExtensionType.DETECTOR, "Barcode", "Standard barcode detector placeholder. Use ZXing adapter for physical barcode decoding.");
    }

    @Override
    public DetectionResult detect(DetectionRequest request, DetectorContext context) {
        return notDetected("Barcode decoding is provided by adapter-specific detector implementations.");
    }
}

