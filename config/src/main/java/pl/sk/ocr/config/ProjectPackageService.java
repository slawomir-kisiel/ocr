package pl.sk.ocr.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.dto.CategoryReferenceDocumentDto;
import pl.sk.ocr.config.dto.CategoryReferenceDocumentsDto;
import pl.sk.ocr.config.dto.ProfileCategoriesDto;
import pl.sk.ocr.config.dto.ProfileDto;

public final class ProjectPackageService {
    public static final String PROFILE_ENTRY = "profile.json";
    private static final String CATEGORIES_DIR = "categories";
    private static final String DOCUMENTS_DIR = "documents";

    private final JsonConfigurationMapper mapper;

    public ProjectPackageService(JsonConfigurationMapper mapper) {
        this.mapper = mapper;
    }

    public ExportResult exportPackage(Path targetZip, ProfileDto profile, List<CategorySource> categories, boolean includeReferenceDocuments) {
        try {
            if (targetZip.getParent() != null) {
                Files.createDirectories(targetZip.getParent());
            }
            var packageCategories = packageCategories(categories, includeReferenceDocuments);
            var packageProfile = packageProfile(profile, packageCategories);
            var missingDocuments = new ArrayList<Path>();
            try (var output = new ZipOutputStream(Files.newOutputStream(targetZip))) {
                writeString(output, PROFILE_ENTRY, mapper.write(packageProfile));
                for (var category : packageCategories) {
                    writeString(output, category.entryName(), mapper.write(category.category()));
                    for (var document : category.documents()) {
                        if (Files.isRegularFile(document.sourcePath())) {
                            writeFile(output, document.entryName(), document.sourcePath());
                        } else {
                            missingDocuments.add(document.sourcePath());
                        }
                    }
                }
            }
            return new ExportResult(targetZip, packageCategories.size(), packageCategories.stream()
                .mapToInt(category -> category.documents().size()).sum(), List.copyOf(missingDocuments));
        } catch (IOException e) {
            throw new ConfigurationException(List.of(new ConfigurationProblem(
                "PACKAGE_EXPORT_FAILED",
                targetZip.toString(),
                e.getMessage()
            )));
        }
    }

    public ImportResult importPackage(Path zipPath, Path targetDirectory) {
        try {
            Files.createDirectories(targetDirectory);
            try (var zip = new ZipFile(zipPath.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    var target = resolveZipEntry(targetDirectory, entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        if (target.getParent() != null) {
                            Files.createDirectories(target.getParent());
                        }
                        try (var input = zip.getInputStream(entry)) {
                            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            var profilePath = targetDirectory.resolve(PROFILE_ENTRY).normalize();
            if (!Files.isRegularFile(profilePath)) {
                throw new IOException("Package does not contain " + PROFILE_ENTRY);
            }
            return new ImportResult(profilePath);
        } catch (IOException e) {
            throw new ConfigurationException(List.of(new ConfigurationProblem(
                "PACKAGE_IMPORT_FAILED",
                zipPath.toString(),
                e.getMessage()
            )));
        }
    }

    public Path extractProfileToTemp(Path zipPath) {
        try {
            var directory = Files.createTempDirectory("ocr-profile-package-");
            var imported = importPackage(zipPath, directory);
            deleteOnExit(directory);
            return imported.profilePath();
        } catch (IOException e) {
            throw new ConfigurationException(List.of(new ConfigurationProblem(
                "PACKAGE_IMPORT_FAILED",
                zipPath.toString(),
                e.getMessage()
            )));
        }
    }

    public boolean isPackage(Path path) {
        return path != null && path.getFileName() != null
            && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private ProfileDto packageProfile(ProfileDto profile, List<PackageCategory> categories) {
        var active = categories.stream()
            .map(category -> category.category().id())
            .toList();
        var files = categories.stream()
            .map(PackageCategory::entryName)
            .toList();
        return new ProfileDto(
            profile.schemaVersion(),
            profile.id(),
            profile.version(),
            profile.displayName(),
            profile.description(),
            new ProfileCategoriesDto(CATEGORIES_DIR, "EXPLICIT", active, files),
            profile.preprocessing(),
            profile.directories(),
            profile.processing(),
            profile.ocr(),
            profile.trace(),
            profile.output()
        );
    }

    private List<PackageCategory> packageCategories(List<CategorySource> categories, boolean includeReferenceDocuments) {
        var usedCategoryEntries = new LinkedHashMap<String, Integer>();
        var packaged = new ArrayList<PackageCategory>();
        for (var source : categories) {
            var category = source.category();
            var categoryFile = uniqueEntry(CATEGORIES_DIR + "/" + safeName(category.id(), "category") + ".json", usedCategoryEntries);
            var documents = includeReferenceDocuments ? packageDocuments(source, category) : List.<PackageDocument>of();
            var rewritten = includeReferenceDocuments ? rewriteReferenceDocuments(category, documents) : category;
            packaged.add(new PackageCategory(categoryFile, rewritten, documents));
        }
        return List.copyOf(packaged);
    }

    private List<PackageDocument> packageDocuments(CategorySource source, CategoryDto category) {
        if (category.referenceDocuments() == null || category.referenceDocuments().documents() == null) {
            return List.of();
        }
        var usedDocumentEntries = new LinkedHashMap<String, Integer>();
        var documents = new ArrayList<PackageDocument>();
        for (var document : category.referenceDocuments().documents()) {
            if (document == null || document.path() == null || document.path().isBlank()) {
                continue;
            }
            var sourcePath = resolveReferenceDocument(source.path(), document.path());
            var fileName = sourcePath == null || sourcePath.getFileName() == null
                ? safeName(document.id(), "document")
                : sourcePath.getFileName().toString();
            var entry = uniqueEntry(DOCUMENTS_DIR + "/" + safeName(category.id(), "category") + "/" + safeFileName(fileName), usedDocumentEntries);
            documents.add(new PackageDocument(document.id(), sourcePath, entry));
        }
        return List.copyOf(documents);
    }

    private CategoryDto rewriteReferenceDocuments(CategoryDto category, List<PackageDocument> documents) {
        if (category.referenceDocuments() == null || category.referenceDocuments().documents() == null || documents.isEmpty()) {
            return category;
        }
        var byId = new LinkedHashMap<String, PackageDocument>();
        for (var document : documents) {
            byId.put(document.id(), document);
        }
        var rewrittenDocuments = category.referenceDocuments().documents().stream()
            .map(document -> {
                var packaged = byId.get(document.id());
                if (packaged == null) {
                    return document;
                }
                return new CategoryReferenceDocumentDto(document.id(), "../" + packaged.entryName(), document.displayName(), document.description());
            })
            .toList();
        return new CategoryDto(
            category.schemaVersion(),
            category.id(),
            category.version(),
            category.displayName(),
            category.description(),
            new CategoryReferenceDocumentsDto(category.referenceDocuments().active(), rewrittenDocuments),
            category.pages(),
            category.ocr(),
            category.identification(),
            category.geometry(),
            category.anchors(),
            category.fields()
        );
    }

    private Path resolveReferenceDocument(Path categoryPath, String value) {
        var path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        if (categoryPath != null && categoryPath.toAbsolutePath().getParent() != null) {
            return categoryPath.toAbsolutePath().getParent().resolve(path).normalize();
        }
        return path.toAbsolutePath().normalize();
    }

    private void writeString(ZipOutputStream output, String entryName, String content) throws IOException {
        output.putNextEntry(new ZipEntry(entryName));
        output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private void writeFile(ZipOutputStream output, String entryName, Path source) throws IOException {
        output.putNextEntry(new ZipEntry(entryName));
        try (var input = Files.newInputStream(source)) {
            input.transferTo(output);
        }
        output.closeEntry();
    }

    private Path resolveZipEntry(Path targetDirectory, String entryName) throws IOException {
        var target = targetDirectory.resolve(entryName).normalize();
        if (!target.startsWith(targetDirectory.normalize())) {
            throw new IOException("Unsafe zip entry: " + entryName);
        }
        return target;
    }

    private void deleteOnExit(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            paths.forEach(path -> path.toFile().deleteOnExit());
        }
    }

    private String uniqueEntry(String candidate, Map<String, Integer> used) {
        var normalized = candidate.replace('\\', '/');
        var count = used.getOrDefault(normalized, 0);
        used.put(normalized, count + 1);
        if (count == 0) {
            return normalized;
        }
        var dot = normalized.lastIndexOf('.');
        if (dot < normalized.lastIndexOf('/')) {
            return normalized + "-" + (count + 1);
        }
        return normalized.substring(0, dot) + "-" + (count + 1) + normalized.substring(dot);
    }

    private String safeName(String value, String fallback) {
        var normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    private String safeFileName(String value) {
        var normalized = value == null ? "" : value.replace('\\', '/');
        var slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        normalized = normalized.replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "document" : normalized;
    }

    public record CategorySource(Path path, CategoryDto category) {
    }

    public record ExportResult(Path targetZip, int categoryCount, int referenceDocumentCount, List<Path> missingReferenceDocuments) {
    }

    public record ImportResult(Path profilePath) {
    }

    private record PackageCategory(String entryName, CategoryDto category, List<PackageDocument> documents) {
    }

    private record PackageDocument(String id, Path sourcePath, String entryName) {
    }
}
