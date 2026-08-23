package pl.sk.ocr.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import pl.sk.ocr.config.runtime.CategoriesMode;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.domain.identifier.CategoryId;

public final class ConfigurationRepository {
    private final ProfileLoader profileLoader;
    private final CategoryLoader categoryLoader;

    public ConfigurationRepository(ProfileLoader profileLoader, CategoryLoader categoryLoader) {
        this.profileLoader = profileLoader;
        this.categoryLoader = categoryLoader;
    }

    public RuntimeConfiguration load(Path profilePath) {
        var profile = profileLoader.load(profilePath);
        var categories = profile.categoryFiles().isEmpty()
            ? loadCategories(profile.categoriesDirectory())
            : loadCategoryFiles(profile.categoryFiles());
        var active = profile.categoriesMode() == CategoriesMode.ALL
            ? categories
            : categories.stream().filter(category -> profile.activeCategories().contains(category.id())).toList();
        validateActiveCategories(profile.activeCategories(), active);
        return new RuntimeConfiguration(profile, active);
    }

    private List<CategoryRuntimeConfiguration> loadCategoryFiles(List<Path> files) {
        return files.stream()
            .map(categoryLoader::load)
            .toList();
    }

    private List<CategoryRuntimeConfiguration> loadCategories(Path directory) {
        try (var files = Files.list(directory)) {
            return files
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .map(categoryLoader::load)
                .toList();
        } catch (IOException e) {
            throw new ConfigurationException(List.of(new ConfigurationProblem(
                "CATEGORY_DIRECTORY_UNAVAILABLE",
                directory.toString(),
                e.getMessage()
            )));
        }
    }

    private void validateActiveCategories(List<CategoryId> requested, List<CategoryRuntimeConfiguration> loaded) {
        var problems = new ArrayList<ConfigurationProblem>();
        var loadedIds = loaded.stream().map(CategoryRuntimeConfiguration::id).toList();
        for (CategoryId id : requested) {
            if (!loadedIds.contains(id)) {
                problems.add(new ConfigurationProblem("UNKNOWN_CATEGORY", "$.categories.active", "Unknown category: " + id.value()));
            }
        }
        if (!problems.isEmpty()) {
            throw new ConfigurationException(problems);
        }
    }
}
