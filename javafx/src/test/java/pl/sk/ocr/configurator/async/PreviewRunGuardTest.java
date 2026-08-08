package pl.sk.ocr.configurator.async;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PreviewRunGuardTest {

    @Test
    void onlyLatestPreviewRunMayApplyResult() {
        var guard = new PreviewRunGuard();

        var first = guard.next();
        var second = guard.next();

        assertThat(guard.isLatest(first)).isFalse();
        assertThat(guard.isLatest(second)).isTrue();
    }
}
