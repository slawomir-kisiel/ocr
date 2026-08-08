package pl.sk.ocr.core.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class InputScanner {
    public List<DocumentJob> scan(Path inputDirectory) {
        try (var stream = Files.list(inputDirectory)) {
            var sequence = new AtomicInteger();
            return stream
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .map(path -> new DocumentJob(sequence.getAndIncrement(), path))
                .toList();
        } catch (IOException e) {
            throw new BatchProcessingException("INPUT_SCAN_FAILED", "Cannot scan input directory: " + inputDirectory, e);
        }
    }
}
