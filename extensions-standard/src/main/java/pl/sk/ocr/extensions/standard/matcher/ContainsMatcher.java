package pl.sk.ocr.extensions.standard.matcher;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.booleanParameter;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import java.util.Locale;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.MatchResult;
import pl.sk.ocr.extension.api.matcher.Matcher;

public final class ContainsMatcher implements Matcher {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("contains", ExtensionType.MATCHER, "Contains", "Matches when OCR text contains expected text.",
            booleanParameter("caseSensitive", "Case sensitive", "Compare text using original letter case.", false, false));
    }

    @Override
    public MatchResult match(MatchRequest request) {
        var caseSensitive = request.parameters().get("caseSensitive")
            .map(value -> value instanceof Boolean bool ? bool : Boolean.parseBoolean(value.toString()))
            .orElse(false);
        var expected = caseSensitive ? request.expected() : request.expected().toLowerCase(Locale.ROOT);
        var actual = caseSensitive ? request.actual() : request.actual().toLowerCase(Locale.ROOT);
        var matched = actual.contains(expected);
        return new MatchResult(matched, matched ? 1.0 : 0.0, matched ? "Expected text found" : "Expected text not found");
    }
}
