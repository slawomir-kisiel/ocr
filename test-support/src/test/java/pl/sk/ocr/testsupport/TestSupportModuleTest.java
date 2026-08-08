package pl.sk.ocr.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestSupportModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(TestSupportModule.NAME).isEqualTo("test-support");
    }
}
