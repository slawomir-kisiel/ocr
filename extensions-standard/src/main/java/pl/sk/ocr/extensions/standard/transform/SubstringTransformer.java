package pl.sk.ocr.extensions.standard.transform;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.integerParameter;
import static pl.sk.ocr.extensions.standard.TextSupport.intParameter;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.transform.ValueTransformationRequest;
import pl.sk.ocr.extension.api.transform.ValueTransformer;

public final class SubstringTransformer implements ValueTransformer {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("substring", ExtensionType.VALUE_TRANSFORMER, "Substring", "Extracts a substring by start and optional length.",
            integerParameter("start", "Start", "Zero-based start index.", false, 0, null, 0),
            integerParameter("length", "Length", "Maximum number of characters. Empty means until end.", false, 0, null, null));
    }

    @Override
    public String transform(ValueTransformationRequest request) {
        var value = request.value();
        var start = Math.min(value.length(), Math.max(0, intParameter(request.parameters(), "start", 0)));
        var length = intParameter(request.parameters(), "length", value.length() - start);
        var end = Math.min(value.length(), start + Math.max(0, length));
        return value.substring(start, end);
    }
}

