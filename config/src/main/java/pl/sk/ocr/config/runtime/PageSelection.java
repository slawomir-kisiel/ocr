package pl.sk.ocr.config.runtime;

import java.util.List;

public sealed interface PageSelection permits SinglePageSelection, RangePageSelection, ExplicitPageSelection, AllPageSelection {
    List<Integer> explicitPages();
}
