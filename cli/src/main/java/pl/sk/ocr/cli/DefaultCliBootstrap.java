package pl.sk.ocr.cli;

import pl.sk.ocr.adapter.pdfbox.PdfBoxDocumentReader;
import pl.sk.ocr.adapter.tess4j.Tess4jOcrEngine;
import pl.sk.ocr.config.CategoryLoader;
import pl.sk.ocr.config.CategoryValidator;
import pl.sk.ocr.config.ConfigurationRepository;
import pl.sk.ocr.config.JsonConfigurationMapper;
import pl.sk.ocr.config.ProfileLoader;
import pl.sk.ocr.config.ProfileValidator;
import pl.sk.ocr.config.ProjectPackageService;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.batch.BatchOptions;
import pl.sk.ocr.core.processing.DocumentProcessor;
import pl.sk.ocr.extension.api.ServiceLoaderExtensionRegistryFactory;

public final class DefaultCliBootstrap implements CliBootstrap {
    private final ProfileOverrideMerger merger = new ProfileOverrideMerger();
    private final CliEnvironmentValidator environmentValidator = new CliEnvironmentValidator();

    @Override
    public ProcessingContext bootstrap(CliOptions options) {
        var extensionRegistry = ServiceLoaderExtensionRegistryFactory.load();
        var mapper = new JsonConfigurationMapper();
        var packageService = new ProjectPackageService(mapper);
        var profilePath = options.profile().toAbsolutePath().normalize();
        if (packageService.isPackage(profilePath)) {
            profilePath = packageService.extractProfileToTemp(profilePath);
        }
        var repository = new ConfigurationRepository(
            new ProfileLoader(mapper, new ProfileValidator()),
            new CategoryLoader(mapper, new CategoryValidator(extensionRegistry))
        );
        var loaded = repository.load(profilePath);
        var profile = merger.merge(loaded.profile(), options);
        environmentValidator.validate(profile, null);
        var configuration = new RuntimeConfiguration(profile, loaded.categories());
        var processor = new DocumentProcessor(new PdfBoxDocumentReader(), new Tess4jOcrEngine(), extensionRegistry);
        var summaryJson = options.summaryJson() == null ? null : options.summaryJson().toAbsolutePath().normalize();
        var batchOptions = new BatchOptions(configuration, options.workers(), null, summaryJson);
        return new ProcessingContext(configuration, processor, batchOptions);
    }
}
