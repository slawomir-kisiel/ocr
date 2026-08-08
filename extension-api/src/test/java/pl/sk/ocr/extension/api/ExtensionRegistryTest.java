package pl.sk.ocr.extension.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.identifier.ExtensionId;

class ExtensionRegistryTest {

    @Test
    void indexesExtensionsById() {
        var extension = new TestExtension("trim");
        var registry = new DefaultExtensionRegistry(List.of(extension));

        assertThat(registry.find(new ExtensionId("trim"))).containsSame(extension);
    }

    @Test
    void rejectsDuplicateExtensionIds() {
        var first = new TestExtension("trim");
        var second = new TestExtension("trim");

        assertThatThrownBy(() -> new DefaultExtensionRegistry(List.of(first, second)))
            .isInstanceOf(ExtensionException.class)
            .hasMessageContaining("Duplicate extension id");
    }
}
