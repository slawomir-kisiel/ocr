package pl.sk.ocr.config;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import pl.sk.ocr.config.dto.*;
import pl.sk.ocr.config.runtime.*;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.trace.TraceMode;

final class ConfigurationMappers {
    private ConfigurationMappers() {
    }

    static CategoryRuntimeConfiguration category(CategoryDto dto) {
        var ocr = ocr(dto.ocr(), OcrSettings.defaults());
        return new CategoryRuntimeConfiguration(
            new CategoryId(dto.id()),
            new ConfigurationVersion(dto.version()),
            dto.displayName(),
            pages(dto.pages()),
            ocr,
            geometry(dto.geometry()),
            groups(dto.identification()),
            dto.anchors().stream().map(ConfigurationMappers::anchor).toList(),
            dto.fields().stream().map(field -> field(field, ocr)).toList()
        );
    }

    static ProfileRuntimeConfiguration profile(ProfileDto dto, Path profilePath) {
        var profileDir = profilePath.toAbsolutePath().getParent();
        var categories = dto.categories();
        var mode = categories.mode() == null ? CategoriesMode.EXPLICIT : CategoriesMode.valueOf(categories.mode());
        var active = categories.active() == null ? List.<CategoryId>of() : categories.active().stream().map(CategoryId::new).toList();
        var workers = dto.processing().workers() == null ? 1 : dto.processing().workers();
        var queueCapacity = dto.processing().queueCapacity() == null ? workers * 4 : dto.processing().queueCapacity();
        var traceMode = dto.trace() == null || dto.trace().mode() == null ? TraceMode.OFF : TraceMode.valueOf(dto.trace().mode());
        return new ProfileRuntimeConfiguration(
            dto.id(),
            new ConfigurationVersion(dto.version()),
            resolve(profileDir, categories.directory()),
            mode,
            active,
            directories(dto.directories(), profileDir),
            new ProcessingConfiguration(workers, queueCapacity),
            ocr(dto.ocr(), OcrSettings.defaults()),
            traceMode,
            csv(dto.output().csv(), profileDir)
        );
    }

    private static DirectoriesConfiguration directories(DirectoriesDto dto, Path base) {
        return new DirectoriesConfiguration(resolve(base, dto.input()), resolve(base, dto.success()), resolve(base, dto.error()));
    }

    private static CsvOutputConfiguration csv(CsvOutputDto dto, Path base) {
        return new CsvOutputConfiguration(
            resolve(base, dto.file()),
            Charset.forName(dto.charset() == null ? "UTF-8" : dto.charset()),
            dto.delimiter() == null ? ";" : dto.delimiter(),
            dto.quote() == null ? "\"" : dto.quote(),
            dto.includeHeader() == null || dto.includeHeader(),
            dto.overwrite() != null && dto.overwrite()
        );
    }

    private static Path resolve(Path base, String value) {
        var path = Path.of(value);
        return path.isAbsolute() ? path.normalize() : base.resolve(path).normalize();
    }

    private static OcrSettings ocr(OcrSettingsDto dto, OcrSettings defaults) {
        if (dto == null) {
            return defaults;
        }
        return new OcrSettings(dto.language() == null ? defaults.language() : dto.language(), dto.datapath() == null ? defaults.datapath() : dto.datapath());
    }

    private static PageSelection pages(PageSelectionDto dto) {
        return switch (dto.type()) {
            case "SINGLE" -> new SinglePageSelection(dto.page());
            case "RANGE" -> new RangePageSelection(dto.from(), dto.to());
            case "EXPLICIT" -> new ExplicitPageSelection(dto.pages());
            case "ALL" -> new AllPageSelection();
            default -> throw new IllegalArgumentException("Unsupported page selection type: " + dto.type());
        };
    }

    private static List<IdentificationGroup> groups(IdentificationDto dto) {
        return dto.groups().stream()
            .map(group -> new IdentificationGroup(group.conditions().stream()
                .map(condition -> new IdentificationCondition(
                    condition.type(),
                    condition.page(),
                    condition.expectedText(),
                    extension(condition.matcher()),
                    extension(condition.detector()),
                    region(condition.searchRegion())
                ))
                .toList()))
            .toList();
    }

    private static AnchorDefinition anchor(AnchorDto dto) {
        return new AnchorDefinition(
            new AnchorId(dto.id()),
            dto.page(),
            extension(dto.detector()),
            dto.required() == null || dto.required(),
            dto.referenceFeature() == null ? null : region(dto.referenceFeature().bounds()),
            region(dto.searchRegion())
        );
    }

    private static GeometryConfiguration geometry(GeometryDto dto) {
        if (dto == null) {
            return new GeometryConfiguration(0, 0, "NONE", List.of());
        }
        var strategy = dto.strategy();
        var anchorIds = strategy == null || strategy.anchors() == null
            ? List.<AnchorId>of()
            : strategy.anchors().stream().map(AnchorId::new).toList();
        return new GeometryConfiguration(
            dto.referenceWidth() == null ? 0 : dto.referenceWidth(),
            dto.referenceHeight() == null ? 0 : dto.referenceHeight(),
            strategy == null || strategy.type() == null ? "NONE" : strategy.type(),
            anchorIds
        );
    }

    private static FieldDefinition field(FieldDto dto, OcrSettings categoryOcr) {
        var output = dto.output();
        return new FieldDefinition(
            new FieldId(dto.id()),
            dto.displayName(),
            dto.page(),
            region(dto.region()),
            dto.required() == null || dto.required(),
            ocr(dto.ocr(), categoryOcr),
            output != null && Boolean.TRUE.equals(output.exported()),
            output == null ? null : output.columnName(),
            extensions(dto.imageProcessors()),
            extensions(dto.transformers()),
            extensions(dto.validators())
        );
    }

    private static List<ExtensionRef> extensions(List<ExtensionRefDto> refs) {
        return refs == null ? List.of() : refs.stream().map(ConfigurationMappers::extension).toList();
    }

    private static ExtensionRef extension(ExtensionRefDto dto) {
        return dto == null ? null : new ExtensionRef(new ExtensionId(dto.id()), dto.parameters());
    }

    private static Region region(RegionDto dto) {
        return dto == null ? null : new Region(dto.x(), dto.y(), dto.width(), dto.height());
    }
}
