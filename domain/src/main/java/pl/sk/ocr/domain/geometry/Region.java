package pl.sk.ocr.domain.geometry;

public record Region(double x, double y, double width, double height) {
    public Region {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("region dimensions must be non-negative");
        }
    }

    public Point topLeft() {
        return new Point(x, y);
    }

    public Point bottomRight() {
        return new Point(x + width, y + height);
    }

    public boolean contains(Point point) {
        return point.x() >= x && point.x() <= x + width
            && point.y() >= y && point.y() <= y + height;
    }

    public Region translate(double dx, double dy) {
        return new Region(x + dx, y + dy, width, height);
    }

    public Region scale(Scale scale) {
        return new Region(x * scale.x(), y * scale.y(), width * scale.x(), height * scale.y());
    }
}
