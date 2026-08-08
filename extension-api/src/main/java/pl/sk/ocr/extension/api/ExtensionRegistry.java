package pl.sk.ocr.extension.api;

import java.util.Collection;
import java.util.Optional;
import pl.sk.ocr.domain.identifier.ExtensionId;

public interface ExtensionRegistry {
    Collection<Extension> extensions();

    Optional<Extension> find(ExtensionId id);

    default Extension require(ExtensionId id) {
        return find(id).orElseThrow(() -> new ExtensionException("EXTENSION_NOT_FOUND", "Extension not found: " + id.value()));
    }
}
