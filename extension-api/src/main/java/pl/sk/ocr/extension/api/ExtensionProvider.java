package pl.sk.ocr.extension.api;

import java.util.Collection;

public interface ExtensionProvider {
    Collection<? extends Extension> extensions();
}
