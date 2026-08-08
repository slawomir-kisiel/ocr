package pl.sk.ocr.core.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import pl.sk.ocr.config.runtime.DirectoriesConfiguration;
import pl.sk.ocr.domain.result.ProcessingStatus;

public final class SourceFileMover {
    public Path move(Path source, ProcessingStatus status, DirectoriesConfiguration directories) {
        var targetDirectory = switch (status) {
            case SUCCESS, WARNING -> directories.success();
            case FAILED, FATAL -> directories.error();
        };
        var target = targetDirectory.resolve(source.getFileName());
        try {
            Files.createDirectories(targetDirectory);
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BatchProcessingException("SOURCE_FILE_MOVE_FAILED", "Cannot move source file: " + source, e);
        }
    }
}
