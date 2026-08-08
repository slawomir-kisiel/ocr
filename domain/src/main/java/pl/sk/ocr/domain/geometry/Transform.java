package pl.sk.ocr.domain.geometry;

public record Transform(Scale scale, double translateX, double translateY) {
    public static final Transform IDENTITY = new Transform(Scale.IDENTITY, 0.0, 0.0);

    public Transform {
        if (scale == null) {
            throw new IllegalArgumentException("scale must not be null");
        }
    }

    public Point map(Point point) {
        return point.scale(scale).translate(translateX, translateY);
    }

    public Region map(Region region) {
        return region.scale(scale).translate(translateX, translateY);
    }
}
