package pl.sk.ocr.domain.geometry;

public record Point(double x, double y) {
    public Point translate(double dx, double dy) {
        return new Point(x + dx, y + dy);
    }

    public Point scale(Scale scale) {
        return new Point(x * scale.x(), y * scale.y());
    }
}
