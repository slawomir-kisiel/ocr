package pl.sk.ocr.extensions.standard.transform;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.transform.ValueTransformationRequest;
import pl.sk.ocr.extension.api.transform.ValueTransformer;

public final class TrimTransformer implements ValueTransformer {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("trim", ExtensionType.VALUE_TRANSFORMER, "Trim", "Removes leading and trailing whitespace.");
    }

    @Override
    public String transform(ValueTransformationRequest request) {
        return request.value().trim();
    }
}

