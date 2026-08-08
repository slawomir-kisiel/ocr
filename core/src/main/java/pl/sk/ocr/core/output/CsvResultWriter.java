package pl.sk.ocr.core.output;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import pl.sk.ocr.config.runtime.CsvOutputConfiguration;

public final class CsvResultWriter {
    public void write(Path outputFile, OutputSchema schema, List<ResultRow> rows, CsvOutputConfiguration options) {
        var tmp = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");
        var partial = outputFile.resolveSibling(outputFile.getFileName() + ".partial");
        try {
            Files.createDirectories(outputFile.toAbsolutePath().getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, options.charset());
                 CSVPrinter printer = new CSVPrinter(writer, format(schema, options))) {
                for (ResultRow row : rows) {
                    printer.printRecord(schema.columnNames().stream()
                        .map(column -> row.values().getOrDefault(column, ""))
                        .toList());
                }
            }
            moveIntoPlace(tmp, outputFile);
            Files.deleteIfExists(partial);
        } catch (IOException e) {
            try {
                if (Files.exists(tmp)) {
                    Files.move(tmp, partial, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw new OutputWriteException("OUTPUT_WRITE_FAILED", "Cannot write CSV output: " + outputFile, e);
        }
    }

    private void moveIntoPlace(Path tmp, Path outputFile) throws IOException {
        try {
            Files.move(tmp, outputFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private CSVFormat format(OutputSchema schema, CsvOutputConfiguration options) {
        var builder = CSVFormat.DEFAULT.builder()
            .setDelimiter(options.delimiter())
            .setQuote(options.quote().charAt(0));
        if (options.includeHeader()) {
            builder.setHeader(schema.columnNames().toArray(String[]::new));
        }
        return builder.get();
    }
}
