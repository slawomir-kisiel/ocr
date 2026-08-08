package pl.sk.ocr.config.runtime;

import java.util.List;

public record AllPageSelection() implements PageSelection {
    @Override
    public List<Integer> explicitPages() {
        return List.of();
    }
}
