package pl.sk.ocr.extension.api.matcher;

public record MatchResult(boolean matched, double score, String explanation) {
    public MatchResult {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        explanation = explanation == null ? "" : explanation;
    }
}
