package pl.sk.ocr.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(ConfigModule.NAME).isEqualTo("config");
    }
}
