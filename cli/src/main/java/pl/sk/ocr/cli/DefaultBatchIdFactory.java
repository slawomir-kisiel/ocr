package pl.sk.ocr.cli;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import pl.sk.ocr.domain.identifier.BatchId;

public final class DefaultBatchIdFactory implements BatchIdFactory {
    private final Clock clock;

    public DefaultBatchIdFactory() {
        this(Clock.systemDefaultZone());
    }

    public DefaultBatchIdFactory(Clock clock) {
        this.clock = clock;
    }

    @Override
    public BatchId create() {
        var timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
            .withZone(clock.getZone())
            .format(clock.instant());
        return new BatchId(timestamp + "-" + UUID.randomUUID().toString().substring(0, 6));
    }
}
