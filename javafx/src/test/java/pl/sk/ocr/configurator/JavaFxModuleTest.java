package pl.sk.ocr.configurator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JavaFxModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(JavaFxModule.NAME).isEqualTo("javafx");
    }
}
