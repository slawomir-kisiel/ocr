package pl.sk.ocr.extension.api.matcher;

import pl.sk.ocr.extension.api.Extension;

public interface Matcher extends Extension {
    MatchResult match(MatchRequest request);
}
