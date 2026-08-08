package pl.sk.ocr.cli;

import pl.sk.ocr.config.runtime.CsvOutputConfiguration;
import pl.sk.ocr.config.runtime.DirectoriesConfiguration;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.ProcessingConfiguration;
import pl.sk.ocr.config.runtime.ProfileRuntimeConfiguration;

public final class ProfileOverrideMerger {
    public ProfileRuntimeConfiguration merge(ProfileRuntimeConfiguration profile, CliOptions options) {
        var directories = new DirectoriesConfiguration(
            options.input() == null ? profile.directories().input() : options.input().toAbsolutePath().normalize(),
            options.success() == null ? profile.directories().success() : options.success().toAbsolutePath().normalize(),
            options.error() == null ? profile.directories().error() : options.error().toAbsolutePath().normalize()
        );
        var processing = new ProcessingConfiguration(
            options.workers() == null ? profile.processing().workers() : options.workers(),
            profile.processing().queueCapacity()
        );
        var datapath = options.ocrDatapath() == null ? profile.ocr().datapath() : options.ocrDatapath().toAbsolutePath().normalize().toString();
        var ocr = new OcrSettings(
            options.ocrLanguage() == null ? profile.ocr().language() : options.ocrLanguage(),
            datapath
        );
        var csv = new CsvOutputConfiguration(
            options.output() == null ? profile.csvOutput().file() : options.output().toAbsolutePath().normalize(),
            profile.csvOutput().charset(),
            profile.csvOutput().delimiter(),
            profile.csvOutput().quote(),
            profile.csvOutput().includeHeader(),
            profile.csvOutput().overwrite()
        );
        return new ProfileRuntimeConfiguration(
            profile.id(),
            profile.version(),
            profile.categoriesDirectory(),
            profile.categoriesMode(),
            profile.activeCategories(),
            directories,
            processing,
            ocr,
            options.trace() == null ? profile.traceMode() : options.trace(),
            csv
        );
    }
}
