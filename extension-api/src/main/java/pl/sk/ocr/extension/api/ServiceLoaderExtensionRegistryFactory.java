package pl.sk.ocr.extension.api;

import java.util.ArrayList;
import java.util.ServiceLoader;

public final class ServiceLoaderExtensionRegistryFactory {
    private ServiceLoaderExtensionRegistryFactory() {
    }

    public static ExtensionRegistry load() {
        return load(Thread.currentThread().getContextClassLoader());
    }

    public static ExtensionRegistry load(ClassLoader classLoader) {
        var extensions = new ArrayList<Extension>();
        ServiceLoader.load(ExtensionProvider.class, classLoader).forEach(provider -> extensions.addAll(provider.extensions()));
        return new DefaultExtensionRegistry(extensions);
    }
}
