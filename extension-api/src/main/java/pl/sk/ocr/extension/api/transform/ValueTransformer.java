package pl.sk.ocr.extension.api.transform;

import pl.sk.ocr.extension.api.Extension;

public interface ValueTransformer extends Extension {
    String transform(ValueTransformationRequest request);
}
