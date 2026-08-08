package pl.sk.ocr.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import pl.sk.ocr.config.runtime.ProfileRuntimeConfiguration;

public final class CliEnvironmentValidator {
    public void validate(ProfileRuntimeConfiguration profile, Path outputOverride) {
        requireReadableDirectory(profile.directories().input(), "input");
        requireWritableDirectory(profile.directories().success(), "success");
        requireWritableDirectory(profile.directories().error(), "error");
        validateOutput(outputOverride == null ? profile.csvOutput().file() : outputOverride, profile.csvOutput().overwrite());
        if (profile.ocr().datapath() != null && !profile.ocr().datapath().isBlank()) {
            requireReadableDirectory(Path.of(profile.ocr().datapath()), "ocr datapath");
        }
    }

    private void requireReadableDirectory(Path path, String name) {
        if (!Files.isDirectory(path) || !Files.isReadable(path)) {
            throw new CliEnvironmentException(name + " directory is not readable: " + path);
        }
    }

    private void requireWritableDirectory(Path path, String name) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new CliEnvironmentException(name + " directory cannot be created: " + path, e);
        }
        if (!Files.isDirectory(path) || !Files.isWritable(path)) {
            throw new CliEnvironmentException(name + " directory is not writable: " + path);
        }
    }

    private void validateOutput(Path output, boolean overwrite) {
        if (Files.exists(output) && !overwrite) {
            throw new CliEnvironmentException("output CSV already exists and overwrite=false: " + output);
        }
        var parent = output.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new CliEnvironmentException("output directory cannot be created: " + parent, e);
        }
        if (!Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw new CliEnvironmentException("output directory is not writable: " + parent);
        }
    }
}
