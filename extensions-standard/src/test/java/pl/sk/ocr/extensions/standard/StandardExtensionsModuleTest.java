package pl.sk.ocr.extensions.standard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.extension.api.ServiceLoaderExtensionRegistryFactory;

class StandardExtensionsModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(StandardExtensionsModule.NAME).isEqualTo("extensions-standard");
    }

    @Test
    void shouldExposeDocumentedStandardExtensionsThroughServiceLoader() {
        var registry = ServiceLoaderExtensionRegistryFactory.load(getClass().getClassLoader());

        assertThat(registry.extensions())
            .extracting(extension -> extension.descriptor().id().value())
            .containsExactlyInAnyOrderElementsOf(Set.of(
                "exact",
                "normalized",
                "fuzzy",
                "regex",
                "text",
                "qr",
                "barcode",
                "remove-boxes",
                "condense-content",
                "crop-empty-margins",
                "trim",
                "remove-whitespace",
                "substring",
                "normalize",
                "pesel",
                "nip",
                "regon",
                "dictionary",
                "regex-validator"
            ));
    }
}
