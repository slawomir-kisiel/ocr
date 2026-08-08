package pl.sk.ocr.domain.geometry;

public record Scale(double x, double y) {
    public static final Scale IDENTITY = new Scale(1.0, 1.0);

    public Scale {
        if (x <= 0 || y <= 0) {
            throw new IllegalArgumentException("scale factors must be positive");
        }
    }
}
