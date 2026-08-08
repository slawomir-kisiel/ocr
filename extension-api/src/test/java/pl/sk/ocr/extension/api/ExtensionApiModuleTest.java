package pl.sk.ocr.extension.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExtensionApiModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(ExtensionApiModule.NAME).isEqualTo("extension-api");
    }
}
