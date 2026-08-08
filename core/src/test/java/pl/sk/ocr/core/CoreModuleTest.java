package pl.sk.ocr.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoreModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(CoreModule.NAME).isEqualTo("core");
    }
}
