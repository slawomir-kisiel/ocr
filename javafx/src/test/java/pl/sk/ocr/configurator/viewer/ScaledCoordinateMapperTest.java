package pl.sk.ocr.configurator.viewer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.geometry.Region;

class ScaledCoordinateMapperTest {

    @Test
    void mapsPointsAndRegionsBetweenScreenAndImageCoordinates() {
        var mapper = new ScaledCoordinateMapper(2.0, 10.0, 20.0);

        assertThat(mapper.screenToImage(new ViewerPoint(30, 60))).isEqualTo(new ViewerPoint(10, 20));
        assertThat(mapper.imageToScreen(new ViewerPoint(10, 20))).isEqualTo(new ViewerPoint(30, 60));
        assertThat(mapper.screenToImage(new Region(30, 60, 40, 20))).isEqualTo(new Region(10, 20, 20, 10));
        assertThat(mapper.imageToScreen(new Region(10, 20, 20, 10))).isEqualTo(new Region(30, 60, 40, 20));
    }
}
