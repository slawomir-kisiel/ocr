package pl.sk.ocr.extensions.standard.transform;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.transform.ValueTransformationRequest;
import pl.sk.ocr.extension.api.transform.ValueTransformer;

public final class RemoveWhitespaceTransformer implements ValueTransformer {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("remove-whitespace", ExtensionType.VALUE_TRANSFORMER, "Remove Whitespace", "Removes all whitespace characters.");
    }

    @Override
    public String transform(ValueTransformationRequest request) {
        return request.value().replaceAll("\\s+", "");
    }
}

