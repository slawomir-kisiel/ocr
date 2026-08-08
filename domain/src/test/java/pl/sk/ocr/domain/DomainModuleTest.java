package pl.sk.ocr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainModuleTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(DomainModule.NAME).isEqualTo("domain");
    }
}
