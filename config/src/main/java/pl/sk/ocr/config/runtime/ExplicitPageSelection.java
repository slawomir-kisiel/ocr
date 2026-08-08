package pl.sk.ocr.config.runtime;

import java.util.List;

public record ExplicitPageSelection(List<Integer> pages) implements PageSelection {
    public ExplicitPageSelection {
        pages = List.copyOf(pages);
    }

    @Override
    public List<Integer> explicitPages() {
        return pages;
    }
}
