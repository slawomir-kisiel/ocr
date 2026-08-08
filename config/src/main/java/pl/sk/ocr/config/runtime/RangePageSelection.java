package pl.sk.ocr.config.runtime;

import java.util.List;
import java.util.stream.IntStream;

public record RangePageSelection(int from, int to) implements PageSelection {
    @Override
    public List<Integer> explicitPages() {
        return IntStream.rangeClosed(from, to).boxed().toList();
    }
}
