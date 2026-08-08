package pl.sk.ocr.extensions.standard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StandardExtensionsModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(StandardExtensionsModule.NAME).isEqualTo("extensions-standard");
    }
}
