package pl.sk.ocr.extension.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import pl.sk.ocr.domain.identifier.ExtensionId;

public final class DefaultExtensionRegistry implements ExtensionRegistry {
    private final Map<ExtensionId, Extension> extensions;

    public DefaultExtensionRegistry(Collection<? extends Extension> extensions) {
        var byId = new LinkedHashMap<ExtensionId, Extension>();
        for (Extension extension : extensions == null ? java.util.List.<Extension>of() : extensions) {
            var previous = byId.putIfAbsent(extension.descriptor().id(), extension);
            if (previous != null) {
                throw new ExtensionException("EXTENSION_DUPLICATE_ID", "Duplicate extension id: " + extension.descriptor().id().value());
            }
        }
        this.extensions = Map.copyOf(byId);
    }

    @Override
    public Collection<Extension> extensions() {
        return extensions.values();
    }

    @Override
    public Optional<Extension> find(ExtensionId id) {
        return Optional.ofNullable(extensions.get(id));
    }
}
