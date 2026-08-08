package pl.sk.ocr.configurator.viewer;

import pl.sk.ocr.domain.geometry.Region;

public interface CoordinateMapper {
    ViewerPoint screenToImage(ViewerPoint point);
    ViewerPoint imageToScreen(ViewerPoint point);
    Region screenToImage(Region region);
    Region imageToScreen(Region region);
}
