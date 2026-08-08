package pl.sk.ocr.cli;

import pl.sk.ocr.domain.identifier.BatchId;

public interface BatchIdFactory {
    BatchId create();
}
