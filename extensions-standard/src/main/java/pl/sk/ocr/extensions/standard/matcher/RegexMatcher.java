package pl.sk.ocr.extensions.standard.matcher;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.MatchResult;
import pl.sk.ocr.extension.api.matcher.Matcher;

public final class RegexMatcher implements Matcher {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("regex", ExtensionType.MATCHER, "Regex", "Treats expected text as Java regular expression.");
    }

    @Override
    public MatchResult match(MatchRequest request) {
        try {
            var matched = Pattern.compile(request.expected()).matcher(request.actual()).find();
            return new MatchResult(matched, matched ? 1.0 : 0.0, matched ? "Regex matched" : "Regex did not match");
        } catch (PatternSyntaxException e) {
            return new MatchResult(false, 0.0, e.getMessage());
        }
    }
}

