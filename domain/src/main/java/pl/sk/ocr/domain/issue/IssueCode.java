package pl.sk.ocr.domain.issue;

import java.util.regex.Pattern;
import pl.sk.ocr.domain.Validation;

public record IssueCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Z][A-Z0-9_]*");

    public IssueCode {
        value = Validation.requireText(value, "issue code");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("issue code must use upper snake case");
        }
    }
}
