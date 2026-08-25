package pl.sk.ocr.domain.geometry;

public record Transform(Scale scale, double translateX, double translateY, double affineA, double affineB, double affineC, double affineD) {
    public static final Transform IDENTITY = new Transform(Scale.IDENTITY, 0.0, 0.0);

    public Transform(Scale scale, double translateX, double translateY) {
        this(scale, translateX, translateY, scale.x(), 0.0, 0.0, scale.y());
    }

    public Transform {
        if (scale == null) {
            throw new IllegalArgumentException("scale must not be null");
        }
    }

    public Point map(Point point) {
        return new Point(
            affineA * point.x() + affineB * point.y() + translateX,
            affineC * point.x() + affineD * point.y() + translateY
        );
    }

    public Region map(Region region) {
        var topLeft = map(region.topLeft());
        var topRight = map(new Point(region.x() + region.width(), region.y()));
        var bottomLeft = map(new Point(region.x(), region.y() + region.height()));
        var bottomRight = map(region.bottomRight());
        var minX = Math.min(Math.min(topLeft.x(), topRight.x()), Math.min(bottomLeft.x(), bottomRight.x()));
        var minY = Math.min(Math.min(topLeft.y(), topRight.y()), Math.min(bottomLeft.y(), bottomRight.y()));
        var maxX = Math.max(Math.max(topLeft.x(), topRight.x()), Math.max(bottomLeft.x(), bottomRight.x()));
        var maxY = Math.max(Math.max(topLeft.y(), topRight.y()), Math.max(bottomLeft.y(), bottomRight.y()));
        return new Region(minX, minY, maxX - minX, maxY - minY);
    }
}
