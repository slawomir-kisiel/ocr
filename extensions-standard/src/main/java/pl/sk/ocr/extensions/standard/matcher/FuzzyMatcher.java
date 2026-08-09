package pl.sk.ocr.extensions.standard.matcher;

import static pl.sk.ocr.extensions.standard.StandardDescriptors.decimalParameter;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;
import static pl.sk.ocr.extensions.standard.TextSupport.doubleParameter;
import static pl.sk.ocr.extensions.standard.TextSupport.normalize;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;
import pl.sk.ocr.extension.api.matcher.MatchRequest;
import pl.sk.ocr.extension.api.matcher.MatchResult;
import pl.sk.ocr.extension.api.matcher.Matcher;

public final class FuzzyMatcher implements Matcher {
    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("fuzzy", ExtensionType.MATCHER, "Fuzzy", "Matches text by normalized Levenshtein similarity.",
            decimalParameter("threshold", "Threshold", "Minimum accepted score from 0.0 to 1.0.", false, 0.0, 1.0, 0.85));
    }

    @Override
    public MatchResult match(MatchRequest request) {
        var expected = normalize(request.expected());
        var actual = normalize(request.actual());
        var max = Math.max(expected.length(), actual.length());
        var score = max == 0 ? 1.0 : 1.0 - ((double) levenshtein(expected, actual) / max);
        var threshold = doubleParameter(request.parameters(), "threshold", 0.85);
        return new MatchResult(score >= threshold, Math.max(0, score), "Score=" + score + ", threshold=" + threshold);
    }

    private int levenshtein(String left, String right) {
        var costs = new int[right.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            costs[0] = i;
            var previous = i - 1;
            for (int j = 1; j <= right.length(); j++) {
                var current = costs[j];
                costs[j] = Math.min(Math.min(costs[j] + 1, costs[j - 1] + 1),
                    previous + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
                previous = current;
            }
        }
        return costs[right.length()];
    }
}

