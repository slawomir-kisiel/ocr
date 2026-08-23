package pl.sk.ocr.extensions.standard.matcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.matcher.MatchRequest;

class ContainsMatcherTest {
    private final ContainsMatcher matcher = new ContainsMatcher();

    @Test
    void matchesTextContainingExpectedValueIgnoringCaseByDefault() {
        var result = matcher.match(new MatchRequest("VOUCHER", "Payment voucher number", ExtensionParameters.empty()));

        assertThat(result.matched()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
    }

    @Test
    void respectsCaseSensitiveParameter() {
        var result = matcher.match(new MatchRequest("VOUCHER", "Payment voucher number",
            ExtensionParameters.of(Map.of("caseSensitive", true))));

        assertThat(result.matched()).isFalse();
    }
}
