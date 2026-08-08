package pl.sk.ocr.config.runtime;

import java.util.List;

public record SinglePageSelection(int page) implements PageSelection {
    @Override
    public List<Integer> explicitPages() {
        return List.of(page);
    }
}
