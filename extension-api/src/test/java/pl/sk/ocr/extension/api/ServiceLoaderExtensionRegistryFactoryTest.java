package pl.sk.ocr.extension.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.identifier.ExtensionId;

class ServiceLoaderExtensionRegistryFactoryTest {

    @Test
    void loadsExtensionsFromServiceLoader() {
        var registry = ServiceLoaderExtensionRegistryFactory.load(getClass().getClassLoader());

        assertThat(registry.find(new ExtensionId("test-provider-extension"))).isPresent();
    }
}
