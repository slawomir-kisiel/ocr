package pl.sk.ocr.configurator.viewer;

import pl.sk.ocr.domain.geometry.Region;

public final class ScaledCoordinateMapper implements CoordinateMapper {
    private final double zoom;
    private final double offsetX;
    private final double offsetY;

    public ScaledCoordinateMapper(double zoom, double offsetX, double offsetY) {
        if (zoom <= 0) {
            throw new IllegalArgumentException("zoom must be positive");
        }
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public ViewerPoint screenToImage(ViewerPoint point) {
        return new ViewerPoint((point.x() - offsetX) / zoom, (point.y() - offsetY) / zoom);
    }

    @Override
    public ViewerPoint imageToScreen(ViewerPoint point) {
        return new ViewerPoint(point.x() * zoom + offsetX, point.y() * zoom + offsetY);
    }

    @Override
    public Region screenToImage(Region region) {
        var topLeft = screenToImage(new ViewerPoint(region.x(), region.y()));
        return new Region(topLeft.x(), topLeft.y(), region.width() / zoom, region.height() / zoom);
    }

    @Override
    public Region imageToScreen(Region region) {
        var topLeft = imageToScreen(new ViewerPoint(region.x(), region.y()));
        return new Region(topLeft.x(), topLeft.y(), region.width() * zoom, region.height() * zoom);
    }
}
