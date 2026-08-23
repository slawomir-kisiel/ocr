package pl.sk.ocr.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.dto.CategoryReferenceDocumentDto;
import pl.sk.ocr.config.dto.CategoryReferenceDocumentsDto;
import pl.sk.ocr.config.dto.CsvOutputDto;
import pl.sk.ocr.config.dto.DirectoriesDto;
import pl.sk.ocr.config.dto.OcrSettingsDto;
import pl.sk.ocr.config.dto.PageSelectionDto;
import pl.sk.ocr.config.dto.ProcessingDto;
import pl.sk.ocr.config.dto.ProfileCategoriesDto;
import pl.sk.ocr.config.dto.ProfileDto;
import pl.sk.ocr.config.dto.ProfileOutputDto;
import pl.sk.ocr.config.dto.ProfilePreprocessingDto;
import pl.sk.ocr.config.dto.TraceDto;

class ProjectPackageServiceTest {
    @TempDir
    Path temp;

    @Test
    void exportsProfilePackageWithRewrittenCategoryAndReferenceDocumentPaths() throws Exception {
        var mapper = new JsonConfigurationMapper();
        var service = new ProjectPackageService(mapper);
        var categoryDir = temp.resolve("source/categories");
        var documentDir = temp.resolve("source/documents");
        Files.createDirectories(categoryDir);
        Files.createDirectories(documentDir);
        var categoryPath = categoryDir.resolve("invoice-a.json");
        var documentPath = documentDir.resolve("sample.pdf");
        Files.writeString(documentPath, "pdf");
        var category = category("invoice-a", "../documents/sample.pdf");
        mapper.write(categoryPath, category);
        var profile = profile("../source/categories", List.of("../source/categories/invoice-a.json"));
        var zipPath = temp.resolve("package.zip");

        var result = service.exportPackage(zipPath, profile, List.of(new ProjectPackageService.CategorySource(categoryPath, category)), true);

        assertThat(result.categoryCount()).isEqualTo(1);
        assertThat(result.referenceDocumentCount()).isEqualTo(1);
        assertThat(result.missingReferenceDocuments()).isEmpty();
        try (var zip = new ZipFile(zipPath.toFile())) {
            assertThat(zip.getEntry("profile.json")).isNotNull();
            assertThat(zip.getEntry("categories/invoice-a.json")).isNotNull();
            assertThat(zip.getEntry("documents/invoice-a/sample.pdf")).isNotNull();
        }
        var imported = service.importPackage(zipPath, temp.resolve("imported"));
        var importedProfile = mapper.read(imported.profilePath(), ProfileDto.class);
        var importedCategory = mapper.read(imported.profilePath().getParent().resolve("categories/invoice-a.json"), CategoryDto.class);
        assertThat(importedProfile.categories().directory()).isEqualTo("categories");
        assertThat(importedProfile.categories().files()).containsExactly("categories/invoice-a.json");
        assertThat(importedCategory.referenceDocuments().documents().getFirst().path())
            .isEqualTo("../documents/invoice-a/sample.pdf");
        assertThat(Files.readString(imported.profilePath().getParent().resolve("documents/invoice-a/sample.pdf"))).isEqualTo("pdf");
    }

    private ProfileDto profile(String categoriesDirectory, List<String> files) {
        return new ProfileDto(
            "1.0",
            "default",
            "1.0",
            "Default",
            "",
            new ProfileCategoriesDto(categoriesDirectory, "EXPLICIT", List.of("invoice-a"), files),
            new ProfilePreprocessingDto(List.of()),
            new DirectoriesDto("./input", "./success", "./error"),
            new ProcessingDto(1, null),
            new OcrSettingsDto("pol", null),
            new TraceDto("OFF"),
            new ProfileOutputDto(new CsvOutputDto("./result.csv", "UTF-8", ";", "\"", true, false))
        );
    }

    private CategoryDto category(String id, String documentPath) {
        return new CategoryDto(
            "1.0",
            id,
            "1.0",
            id,
            "",
            new CategoryReferenceDocumentsDto("sample", List.of(new CategoryReferenceDocumentDto("sample", documentPath, "Sample", ""))),
            new PageSelectionDto("SINGLE", 1, null, null, null),
            new OcrSettingsDto("pol", null),
            null,
            null,
            List.of(),
            List.of()
        );
    }
}
