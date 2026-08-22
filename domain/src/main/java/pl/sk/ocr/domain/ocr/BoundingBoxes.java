package pl.sk.ocr.domain.ocr;

import java.util.Collection;
import pl.sk.ocr.domain.geometry.Region;

final class BoundingBoxes {
    private BoundingBoxes() {
    }

    static BoundingBox unionWords(Collection<OcrWord> words) {
        if (words == null || words.isEmpty()) {
            return new BoundingBox(new Region(0, 0, 0, 0));
        }
        var minX = Double.POSITIVE_INFINITY;
        var minY = Double.POSITIVE_INFINITY;
        var maxX = Double.NEGATIVE_INFINITY;
        var maxY = Double.NEGATIVE_INFINITY;
        for (var word : words) {
            var region = word.boundingBox().region();
            minX = Math.min(minX, region.x());
            minY = Math.min(minY, region.y());
            maxX = Math.max(maxX, region.x() + region.width());
            maxY = Math.max(maxY, region.y() + region.height());
        }
        return new BoundingBox(new Region(minX, minY, maxX - minX, maxY - minY));
    }
}
