package pl.sk.ocr.extension.api.detector;

import pl.sk.ocr.extension.api.Extension;

public interface Detector extends Extension {
    DetectionResult detect(DetectionRequest request, DetectorContext context);
}
