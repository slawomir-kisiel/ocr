package pl.sk.ocr.config.runtime;

import java.nio.file.Path;

public record DirectoriesConfiguration(Path input, Path success, Path error) {
}
