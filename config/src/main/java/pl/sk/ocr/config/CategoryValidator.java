package pl.sk.ocr.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import pl.sk.ocr.config.dto.*;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.extension.api.ExtensionParameterDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameterType;
import pl.sk.ocr.extension.api.ExtensionRegistry;
import pl.sk.ocr.extension.api.ExtensionType;

public final class CategoryValidator implements ConfigurationValidator<CategoryDto> {
    private final ExtensionRegistry extensionRegistry;

    public CategoryValidator(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public List<ConfigurationProblem> validate(CategoryDto dto) {
        var problems = new ArrayList<ConfigurationProblem>();
        required(dto.schemaVersion(), "$.schemaVersion", problems);
        required(dto.id(), "$.id", problems);
        required(dto.version(), "$.version", problems);
        required(dto.displayName(), "$.displayName", problems);
        if (!"1.0".equals(dto.schemaVersion())) {
            problems.add(problem("CONFIGURATION_SCHEMA_UNSUPPORTED", "$.schemaVersion", "Only schemaVersion 1.0 is supported"));
        }
        validatePages(dto.pages(), "$.pages", problems);
        validateReferenceDocuments(dto.referenceDocuments(), problems);
        validateIdentification(dto.identification(), problems);
        validateAnchors(dto.anchors(), dto.geometry(), problems);
        validateFields(dto.fields(), problems);
        return problems;
    }

    private void validateReferenceDocuments(CategoryReferenceDocumentsDto dto, List<ConfigurationProblem> problems) {
        if (dto == null || dto.documents() == null || dto.documents().isEmpty()) {
            return;
        }
        var ids = new HashSet<String>();
        for (int i = 0; i < dto.documents().size(); i++) {
            var document = dto.documents().get(i);
            if (document == null) {
                problems.add(problem("REFERENCE_DOCUMENT_INVALID", "$.referenceDocuments.documents[" + i + "]", "Reference document is required"));
                continue;
            }
            required(document.id(), "$.referenceDocuments.documents[" + i + "].id", problems);
            required(document.path(), "$.referenceDocuments.documents[" + i + "].path", problems);
            if (document.id() != null && !document.id().isBlank() && !ids.add(document.id())) {
                problems.add(problem("DUPLICATE_ID", "$.referenceDocuments.documents[" + i + "].id", "Duplicate reference document id"));
            }
        }
        if (dto.active() != null && !dto.active().isBlank() && !ids.contains(dto.active())) {
            problems.add(problem("REFERENCE_DOCUMENT_INVALID", "$.referenceDocuments.active", "Unknown active reference document: " + dto.active()));
        }
    }

    private void validateIdentification(IdentificationDto dto, List<ConfigurationProblem> problems) {
        if (dto == null || dto.groups() == null || dto.groups().isEmpty()) {
            problems.add(problem("CATEGORY_IDENTIFICATION_REQUIRED", "$.identification.groups", "At least one identification group is required"));
            return;
        }
        for (int i = 0; i < dto.groups().size(); i++) {
            var group = dto.groups().get(i);
            if (group.conditions() == null || group.conditions().isEmpty()) {
                problems.add(problem("CATEGORY_IDENTIFICATION_REQUIRED", "$.identification.groups[" + i + "].conditions", "At least one condition is required"));
                continue;
            }
            for (int j = 0; j < group.conditions().size(); j++) {
                var condition = group.conditions().get(j);
                if (isTextCondition(condition.type())) {
                    validateExtension(condition.matcher(), ExtensionType.MATCHER,
                        "$.identification.groups[" + i + "].conditions[" + j + "].matcher", problems);
                } else if ("QR".equals(condition.type()) || "BARCODE".equals(condition.type())) {
                    validateExtension(condition.detector(), ExtensionType.DETECTOR,
                        "$.identification.groups[" + i + "].conditions[" + j + "].detector", problems);
                }
            }
        }
    }

    private boolean isTextCondition(String type) {
        return "TEXT".equals(type) || "TEXT_FUZZY".equals(type);
    }

    private void validateAnchors(List<AnchorDto> anchors, GeometryDto geometry, List<ConfigurationProblem> problems) {
        if (anchors == null) {
            return;
        }
        var ids = new HashSet<String>();
        for (int i = 0; i < anchors.size(); i++) {
            var anchor = anchors.get(i);
            if (!ids.add(anchor.id())) {
                problems.add(problem("DUPLICATE_ID", "$.anchors[" + i + "].id", "Duplicate anchor id"));
            }
            positive(anchor.page(), "$.anchors[" + i + "].page", problems);
            validateExtension(anchor.detector(), ExtensionType.DETECTOR, "$.anchors[" + i + "].detector", problems);
            validateRegion(anchor.searchRegion(), "$.anchors[" + i + "].searchRegion", problems);
            if (anchor.referenceFeature() != null) {
                validateRegion(anchor.referenceFeature().bounds(), "$.anchors[" + i + "].referenceFeature.bounds", problems);
            }
        }
        validateGeometryAnchorReferences(anchors, geometry, problems);
    }

    private void validateGeometryAnchorReferences(List<AnchorDto> anchors, GeometryDto geometry, List<ConfigurationProblem> problems) {
        if (geometry == null || geometry.strategy() == null || geometry.strategy().anchors() == null) {
            return;
        }
        var ids = anchors.stream().map(AnchorDto::id).collect(java.util.stream.Collectors.toSet());
        for (int i = 0; i < geometry.strategy().anchors().size(); i++) {
            var anchorId = geometry.strategy().anchors().get(i);
            if (!ids.contains(anchorId)) {
                problems.add(problem("UNKNOWN_ANCHOR", "$.geometry.strategy.anchors[" + i + "]", "Unknown anchor: " + anchorId));
            }
        }
    }

    private void validateFields(List<FieldDto> fields, List<ConfigurationProblem> problems) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        var ids = new HashSet<String>();
        var columns = new HashSet<String>();
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            if (!ids.add(field.id())) {
                problems.add(problem("DUPLICATE_ID", "$.fields[" + i + "].id", "Duplicate field id"));
            }
            positive(field.page(), "$.fields[" + i + "].page", problems);
            validateRegion(field.region(), "$.fields[" + i + "].region", problems);
            if (field.output() != null && Boolean.TRUE.equals(field.output().exported())) {
                required(field.output().columnName(), "$.fields[" + i + "].output.columnName", problems);
                if (field.output().columnName() != null && !columns.add(field.output().columnName())) {
                    problems.add(problem("DUPLICATE_OUTPUT_COLUMN", "$.fields[" + i + "].output.columnName", "Duplicate output column"));
                }
            }
            validateExtensions(field.imageProcessors(), ExtensionType.IMAGE_PROCESSOR, "$.fields[" + i + "].imageProcessors", problems);
            validateExtensions(field.transformers(), ExtensionType.VALUE_TRANSFORMER, "$.fields[" + i + "].transformers", problems);
            validateExtensions(field.validators(), ExtensionType.VALIDATOR, "$.fields[" + i + "].validators", problems);
            validateOcr(field.ocr(), "$.fields[" + i + "].ocr", problems);
        }
    }

    private void validatePages(PageSelectionDto pages, String path, List<ConfigurationProblem> problems) {
        if (pages == null || pages.type() == null) {
            problems.add(problem("INVALID_PAGE_LIMITS", path, "Page selection is required"));
            return;
        }
        switch (pages.type()) {
            case "SINGLE" -> positive(pages.page(), path + ".page", problems);
            case "RANGE" -> {
                positive(pages.from(), path + ".from", problems);
                positive(pages.to(), path + ".to", problems);
                if (pages.from() != null && pages.to() != null && pages.from() > pages.to()) {
                    problems.add(problem("INVALID_PAGE_LIMITS", path, "Range from must be <= to"));
                }
            }
            case "EXPLICIT" -> {
                if (pages.pages() == null || pages.pages().isEmpty()) {
                    problems.add(problem("INVALID_PAGE_LIMITS", path + ".pages", "Explicit pages must not be empty"));
                } else {
                    var unique = new HashSet<Integer>();
                    for (Integer page : pages.pages()) {
                        positive(page, path + ".pages", problems);
                        if (!unique.add(page)) {
                            problems.add(problem("INVALID_PAGE_LIMITS", path + ".pages", "Duplicate page"));
                        }
                    }
                }
            }
            case "ALL" -> { }
            default -> problems.add(problem("INVALID_PAGE_LIMITS", path + ".type", "Unsupported page selection type"));
        }
    }

    private void validateExtensions(List<ExtensionRefDto> refs, ExtensionType expectedType, String path, List<ConfigurationProblem> problems) {
        if (refs != null) {
            for (int i = 0; i < refs.size(); i++) {
                validateExtension(refs.get(i), expectedType, path + "[" + i + "]", problems);
            }
        }
    }

    private void validateExtension(ExtensionRefDto ref, ExtensionType expectedType, String path, List<ConfigurationProblem> problems) {
        if (ref == null) {
            return;
        }
        if (ref.id() == null || ref.id().isBlank()) {
            problems.add(problem("EXTENSION_NOT_FOUND", path, "Extension id is required"));
            return;
        }
        try {
            var id = new ExtensionId(ref.id());
            var extension = extensionRegistry.find(id);
            if (extension.isEmpty()) {
                problems.add(problem("EXTENSION_NOT_FOUND", path + ".id", "Unknown extension: " + ref.id()));
            } else {
                var descriptor = extension.get().descriptor();
                if (descriptor.type() != expectedType) {
                    problems.add(problem("EXTENSION_TYPE_INVALID", path + ".id",
                        "Extension " + ref.id() + " has type " + descriptor.type() + " but " + expectedType + " is required"));
                    return;
                }
                for (ExtensionParameterDescriptor parameter : descriptor.parameters()) {
                    validateExtensionParameter(ref, parameter, path + ".parameters." + parameter.name(), problems);
                }
            }
        } catch (IllegalArgumentException e) {
            problems.add(problem("EXTENSION_NOT_FOUND", path + ".id", e.getMessage()));
        }
    }

    private void validateExtensionParameter(ExtensionRefDto ref, ExtensionParameterDescriptor descriptor, String path,
                                            List<ConfigurationProblem> problems) {
        var parameters = ref.parameters();
        var value = parameters == null ? null : parameters.get(descriptor.name());
        if (value == null) {
            if (descriptor.required()) {
                problems.add(problem("EXTENSION_PARAMETERS_INVALID", path, "Required extension parameter is missing"));
            }
            return;
        }
        if (!matchesType(value, descriptor.type())) {
            problems.add(problem("EXTENSION_PARAMETERS_INVALID", path, "Extension parameter has invalid type"));
        }
    }

    private boolean matchesType(Object value, ExtensionParameterType type) {
        return switch (type) {
            case STRING, ENUM, REGEX -> value instanceof String;
            case INTEGER -> value instanceof Integer;
            case DECIMAL -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
        };
    }

    private void validateRegion(RegionDto region, String path, List<ConfigurationProblem> problems) {
        if (region == null) {
            return;
        }
        if (region.width() <= 0 || region.height() <= 0) {
            problems.add(problem("INVALID_REGION", path, "Region width and height must be positive"));
        }
        if (region.x() < 0 || region.y() < 0) {
            problems.add(problem("INVALID_REGION", path, "Region coordinates must be non-negative"));
        }
    }

    private void validateOcr(OcrSettingsDto ocr, String path, List<ConfigurationProblem> problems) {
        if (ocr != null && ocr.language() != null && ocr.language().isBlank()) {
            problems.add(problem("OCR_LANGUAGE_INVALID", path + ".language", "OCR language must not be blank"));
        }
    }

    private static void positive(Integer value, String path, List<ConfigurationProblem> problems) {
        if (value == null || value < 1) {
            problems.add(problem("INVALID_PAGE_LIMITS", path, "Page must be >= 1"));
        }
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
