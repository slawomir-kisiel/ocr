package pl.sk.ocr.domain.geometry;

public record Size(double width, double height) {
    public Size {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("size dimensions must be non-negative");
        }
    }
}
