package pl.sk.ocr.domain.geometry;

public enum Rotation {
    NONE(0),
    CLOCKWISE_90(90),
    CLOCKWISE_180(180),
    CLOCKWISE_270(270);

    private final int degrees;

    Rotation(int degrees) {
        this.degrees = degrees;
    }

    public int degrees() {
        return degrees;
    }
}
