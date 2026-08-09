package pl.sk.ocr.extensions.standard.matcher;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.TextSupport.normalize;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.MatchResult;
import pl.sk.ocr.extension.api.matcher.Matcher;

public final class NormalizedMatcher implements Matcher {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("normalized", ExtensionType.MATCHER, "Normalized", "Matches lower-cased text with collapsed spaces and removed diacritics.");
    }

    @Override
    public MatchResult match(MatchRequest request) {
        var matched = normalize(request.expected()).equals(normalize(request.actual()));
        return new MatchResult(matched, matched ? 1.0 : 0.0, matched ? "Normalized match" : "Normalized text differs");
    }
}

