package pl.sk.ocr.extensions.standard.matcher;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.MatchResult;
import pl.sk.ocr.extension.api.matcher.Matcher;

public final class ExactMatcher implements Matcher {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("exact", ExtensionType.MATCHER, "Exact", "Matches text exactly.");
    }

    @Override
    public MatchResult match(MatchRequest request) {
        var matched = request.expected().equals(request.actual());
        return new MatchResult(matched, matched ? 1.0 : 0.0, matched ? "Exact match" : "Text differs");
    }
}

