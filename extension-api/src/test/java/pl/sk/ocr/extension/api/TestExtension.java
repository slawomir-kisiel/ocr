package pl.sk.ocr.extension.api;

import java.util.List;
import pl.sk.ocr.domain.identifier.ExtensionId;

final class TestExtension implements Extension {
    private final ExtensionDescriptor descriptor;

    TestExtension(String id) {
        this.descriptor = new ExtensionDescriptor(
            new ExtensionId(id),
            ExtensionType.VALUE_TRANSFORMER,
            id,
            "Test extension",
            "1.0",
            List.of()
        );
    }

    @Override
    public ExtensionDescriptor descriptor() {
        return descriptor;
    }
}
