package pl.sk.ocr.configurator.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.core.image.BufferedProcessingImage;

class InMemoryTraceImageStoreTest {

    @Test
    void storesAndClearsImagesByTraceReference() {
        var store = new InMemoryTraceImageStore();
        var image = new BufferedProcessingImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));

        var ref = store.put("Input", image);

        assertThat(ref.id()).startsWith("trace-image-");
        assertThat(ref.label()).isEqualTo("Input");
        assertThat(store.get(ref)).hasValueSatisfying(stored -> {
            assertThat(stored).isNotSameAs(image);
            assertThat(stored.width()).isEqualTo(image.width());
            assertThat(stored.height()).isEqualTo(image.height());
        });
        assertThat(store.size()).isEqualTo(1);

        store.clear();

        assertThat(store.get(ref)).isEmpty();
        assertThat(store.size()).isZero();
    }
}
