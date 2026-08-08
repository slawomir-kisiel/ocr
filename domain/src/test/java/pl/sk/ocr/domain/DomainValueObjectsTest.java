package pl.sk.ocr.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.Confidence;

class DomainValueObjectsTest {

    @Test
    void validatesIdentifiers() {
        assertThat(new ExtensionId("remove-boxes").value()).isEqualTo("remove-boxes");

        assertThatThrownBy(() -> new ExtensionId("RemoveBoxes"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesPageNumberAndConfidence() {
        assertThat(new PageNumber(1).value()).isOne();
        assertThat(new Confidence(0.75).value()).isEqualTo(0.75);

        assertThatThrownBy(() -> new PageNumber(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Confidence(1.01)).isInstanceOf(IllegalArgumentException.class);
    }
}
