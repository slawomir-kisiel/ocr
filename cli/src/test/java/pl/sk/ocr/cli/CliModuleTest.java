package pl.sk.ocr.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CliModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(CliModule.NAME).isEqualTo("cli");
    }
}
