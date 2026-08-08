package pl.sk.ocr.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdaptersModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(AdaptersModule.NAME).isEqualTo("adapters");
    }
}
