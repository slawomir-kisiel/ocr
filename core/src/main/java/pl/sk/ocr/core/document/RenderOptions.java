package pl.sk.ocr.core.document;

public record RenderOptions(int dpi) {
    public RenderOptions {
        if (dpi < 1) {
            throw new IllegalArgumentException("dpi must be >= 1");
        }
    }

    public static RenderOptions defaults() {
        return new RenderOptions(300);
    }
}
