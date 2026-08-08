package pl.sk.ocr.extension.api;

import java.util.Collection;
import java.util.List;

public final class TestExtensionProvider implements ExtensionProvider {
    @Override
    public Collection<? extends Extension> extensions() {
        return List.of(new TestExtension("test-provider-extension"));
    }
}
