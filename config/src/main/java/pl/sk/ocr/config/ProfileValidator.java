package pl.sk.ocr.config;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import pl.sk.ocr.config.dto.ProfileDto;

public final class ProfileValidator implements ConfigurationValidator<ProfileDto> {
    @Override
    public List<ConfigurationProblem> validate(ProfileDto dto) {
        var problems = new ArrayList<ConfigurationProblem>();
        required(dto.schemaVersion(), "$.schemaVersion", problems);
        required(dto.id(), "$.id", problems);
        required(dto.version(), "$.version", problems);
        if (!"1.0".equals(dto.schemaVersion())) {
            problems.add(problem("CONFIGURATION_SCHEMA_UNSUPPORTED", "$.schemaVersion", "Only schemaVersion 1.0 is supported"));
        }
        if (dto.categories() == null) {
            problems.add(problem("PROFILE_INVALID", "$.categories", "Categories section is required"));
        } else {
            required(dto.categories().directory(), "$.categories.directory", problems);
            var mode = dto.categories().mode() == null ? "EXPLICIT" : dto.categories().mode();
            if (!mode.equals("EXPLICIT") && !mode.equals("ALL")) {
                problems.add(problem("PROFILE_INVALID", "$.categories.mode", "Unsupported categories mode"));
            }
            if (mode.equals("EXPLICIT")) {
                if (dto.categories().active() == null || dto.categories().active().isEmpty()) {
                    problems.add(problem("PROFILE_INVALID", "$.categories.active", "Active categories must not be empty"));
                } else if (new HashSet<>(dto.categories().active()).size() != dto.categories().active().size()) {
                    problems.add(problem("PROFILE_INVALID", "$.categories.active", "Duplicate active category id"));
                }
            }
            if (mode.equals("ALL") && dto.categories().active() != null && !dto.categories().active().isEmpty()) {
                problems.add(problem("PROFILE_INVALID", "$.categories.active", "Active categories must be omitted in ALL mode"));
            }
            if (dto.categories().files() != null) {
                var files = dto.categories().files();
                for (int i = 0; i < files.size(); i++) {
                    if (files.get(i) == null || files.get(i).isBlank()) {
                        problems.add(problem("PROFILE_INVALID", "$.categories.files[" + i + "]", "Category file path must not be blank"));
                    }
                }
            }
        }
        if (dto.directories() == null) {
            problems.add(problem("PROFILE_INVALID", "$.directories", "Directories section is required"));
        } else {
            required(dto.directories().input(), "$.directories.input", problems);
            required(dto.directories().success(), "$.directories.success", problems);
            required(dto.directories().error(), "$.directories.error", problems);
        }
        if (dto.processing() == null || dto.processing().workers() == null || dto.processing().workers() < 1) {
            problems.add(problem("PROFILE_INVALID", "$.processing.workers", "workers must be >= 1"));
        } else if (dto.processing().queueCapacity() != null && dto.processing().queueCapacity() < dto.processing().workers()) {
            problems.add(problem("PROFILE_INVALID", "$.processing.queueCapacity", "queueCapacity must be >= workers"));
        }
        if (dto.ocr() != null && dto.ocr().language() != null && dto.ocr().language().isBlank()) {
            problems.add(problem("OCR_LANGUAGE_INVALID", "$.ocr.language", "OCR language must not be blank"));
        }
        if (dto.preprocessing() != null && dto.preprocessing().imageProcessors() != null) {
            for (int i = 0; i < dto.preprocessing().imageProcessors().size(); i++) {
                var processor = dto.preprocessing().imageProcessors().get(i);
                if (processor == null || processor.id() == null || processor.id().isBlank()) {
                    problems.add(problem("PROFILE_INVALID", "$.preprocessing.imageProcessors[" + i + "].id", "Image processor id is required"));
                }
            }
        }
        if (dto.output() == null || dto.output().csv() == null) {
            problems.add(problem("PROFILE_INVALID", "$.output.csv", "CSV output is required"));
        } else {
            required(dto.output().csv().file(), "$.output.csv.file", problems);
            if (dto.output().csv().charset() != null && !Charset.isSupported(dto.output().csv().charset())) {
                problems.add(problem("PROFILE_INVALID", "$.output.csv.charset", "Unsupported charset"));
            }
        }
        return problems;
    }

    private static void required(String value, String path, List<ConfigurationProblem> problems) {
        if (value == null || value.isBlank()) {
            problems.add(problem("CONFIGURATION_INVALID", path, "Value is required"));
        }
    }

    private static ConfigurationProblem problem(String code, String path, String message) {
        return new ConfigurationProblem(code, path, message);
    }
}
