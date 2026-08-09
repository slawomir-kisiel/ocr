package pl.sk.ocr.extensions.standard.transform;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.TextSupport.normalize;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.transform.ValueTransformationRequest;
import pl.sk.ocr.extension.api.transform.ValueTransformer;

public final class NormalizeTransformer implements ValueTransformer {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("normalize", ExtensionType.VALUE_TRANSFORMER, "Normalize", "Lower-cases text, removes diacritics and collapses spaces.");
    }

    @Override
    public String transform(ValueTransformationRequest request) {
        return normalize(request.value());
    }
}

