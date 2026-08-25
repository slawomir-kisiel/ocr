package pl.sk.ocr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.geometry.Point;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.geometry.Scale;
import pl.sk.ocr.domain.geometry.Transform;

class GeometryTest {

    @Test
    void mapsPointAndRegionByScaleAndTranslation() {
        var transform = new Transform(new Scale(2.0, 3.0), 10.0, 20.0);

        assertThat(transform.map(new Point(4.0, 5.0))).isEqualTo(new Point(18.0, 35.0));
        assertThat(transform.map(new Region(1.0, 2.0, 3.0, 4.0)))
            .isEqualTo(new Region(12.0, 26.0, 6.0, 12.0));
    }

    @Test
    void mapsPointAndRegionByAffineCoefficients() {
        var transform = new Transform(new Scale(2.0, 1.0), 5.0, 7.0, 1.0, 1.0, 0.0, 1.0);

        assertThat(transform.map(new Point(4.0, 5.0))).isEqualTo(new Point(14.0, 12.0));
        assertThat(transform.map(new Region(0.0, 0.0, 10.0, 10.0)))
            .isEqualTo(new Region(5.0, 7.0, 20.0, 10.0));
    }
}
