package pl.sk.ocr.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.CategoriesMode;
import pl.sk.ocr.config.runtime.SinglePageSelection;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;
import pl.sk.ocr.extension.api.Extension;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterType;
import pl.sk.ocr.extension.api.ExtensionType;

class ConfigurationRepositoryTest {

    @Test
    void loadsProfileAndReferencedCategoriesIntoRuntimeSnapshot() {
        var repository = repository();

        var runtime = repository.load(fixture("profiles/minimal-valid-profile.json"));

        assertThat(runtime.profile().categoriesMode()).isEqualTo(CategoriesMode.EXPLICIT);
        assertThat(runtime.profile().ocr().language()).isEqualTo("pol");
        assertThat(runtime.categories()).hasSize(1);
        assertThat(runtime.categories().getFirst().id().value()).isEqualTo("invoice-a");
        assertThat(runtime.categories().getFirst().pages()).isInstanceOf(SinglePageSelection.class);
        assertThat(runtime.categories().getFirst().fields()).hasSize(1);
    }

    @Test
    void rejectsUnknownExtension() {
        var repository = repository(List.of(new TestExtension("normalized")));

        assertThatThrownBy(() -> repository.load(fixture("profiles/minimal-valid-profile.json")))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(error -> assertThat(((ConfigurationException) error).problems())
                .extracting(ConfigurationProblem::code)
                .contains("EXTENSION_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidProfileWorkers() {
        var repository = repository();

        assertThatThrownBy(() -> repository.load(fixture("profiles/invalid-workers-profile.json")))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(error -> assertThat(((ConfigurationException) error).problems())
                .extracting(ConfigurationProblem::path)
                .contains("$.processing.workers"));
    }

    @Test
    void rejectsInvalidCategoryRegion() {
        var loader = categoryLoader(List.of(
            new TestExtension("normalized"),
            new TestExtension("text")
        ));

        assertThatThrownBy(() -> loader.load(fixture("invalid/invalid-region-category.json")))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(error -> assertThat(((ConfigurationException) error).problems())
                .extracting(ConfigurationProblem::code)
                .contains("INVALID_REGION"));
    }

    @Test
    void rejectsMissingRequiredExtensionParameter() {
        var loader = categoryLoader(List.of(
            new TestExtension("normalized"),
            new TestExtension("text", List.of(new ExtensionParameterDescriptor(
                "text",
                "Text",
                "",
                ExtensionParameterType.STRING,
                true,
                null,
                null
            )))
        ));

        assertThatThrownBy(() -> loader.load(fixture("invalid/invalid-extension-parameters-category.json")))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(error -> assertThat(((ConfigurationException) error).problems())
                .extracting(ConfigurationProblem::code)
                .contains("EXTENSION_PARAMETERS_INVALID"));
    }

    @Test
    void rejectsUnknownGeometryAnchorReference() {
        var loader = categoryLoader(List.of(
            new TestExtension("normalized"),
            new TestExtension("text")
        ));

        assertThatThrownBy(() -> loader.load(fixture("invalid/invalid-anchor-reference-category.json")))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(error -> assertThat(((ConfigurationException) error).problems())
                .extracting(ConfigurationProblem::code)
                .contains("UNKNOWN_ANCHOR"));
    }

    private static ConfigurationRepository repository() {
        return repository(List.of(
            new TestExtension("normalized"),
            new TestExtension("text"),
            new TestExtension("trim"),
            new TestExtension("required")
        ));
    }

    private static ConfigurationRepository repository(List<Extension> extensions) {
        var mapper = new JsonConfigurationMapper();
        var registry = new DefaultExtensionRegistry(extensions);
        return new ConfigurationRepository(
            new ProfileLoader(mapper, new ProfileValidator()),
            new CategoryLoader(mapper, new CategoryValidator(registry))
        );
    }

    private static CategoryLoader categoryLoader(List<Extension> extensions) {
        return new CategoryLoader(new JsonConfigurationMapper(), new CategoryValidator(new DefaultExtensionRegistry(extensions)));
    }

    private static Path fixture(String name) {
        return Path.of("src/test/resources/fixtures/config").resolve(name);
    }

    private record TestExtension(ExtensionDescriptor descriptor) implements Extension {
        TestExtension(String id) {
            this(new ExtensionDescriptor(new ExtensionId(id), ExtensionType.MATCHER, id, "", "1.0", List.of()));
        }

        TestExtension(String id, List<ExtensionParameterDescriptor> parameters) {
            this(new ExtensionDescriptor(new ExtensionId(id), ExtensionType.MATCHER, id, "", "1.0", parameters));
        }
    }
}
