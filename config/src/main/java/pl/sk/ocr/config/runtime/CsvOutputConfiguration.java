package pl.sk.ocr.config.runtime;

import java.nio.charset.Charset;
import java.nio.file.Path;

public record CsvOutputConfiguration(Path file, Charset charset, String delimiter, String quote, boolean includeHeader, boolean overwrite) {
}
