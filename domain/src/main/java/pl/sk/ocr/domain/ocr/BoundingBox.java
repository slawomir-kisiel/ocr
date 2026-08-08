package pl.sk.ocr.domain.ocr;

import pl.sk.ocr.domain.geometry.Region;

public record BoundingBox(Region region) {
    public BoundingBox {
        if (region == null) {
            throw new IllegalArgumentException("region must not be null");
        }
    }
}
