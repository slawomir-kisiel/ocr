package pl.sk.ocr.configurator.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import pl.sk.ocr.config.dto.*;

public final class CategoryDraftEditor {

    public CategoryDto newCategory(String id, String displayName) {
        var normalizedId = normalizeRequired(id, "category id", "new-category");
        var normalizedDisplayName = normalizeRequired(displayName, "display name", normalizedId);
        return new CategoryDto(
            "1.0",
            normalizedId,
            "1.0",
            normalizedDisplayName,
            "",
            null,
            new PageSelectionDto("SINGLE", 1, null, null, null),
            new OcrSettingsDto("pol", null),
            new IdentificationDto(List.of(new ConditionGroupDto(List.of(new ConditionDto(1, "", null, new ExtensionRefDto("text", java.util.Map.of()), null))))),
            new GeometryDto(0, 0, new GeometryStrategyDto("NONE", List.of())),
            List.of(),
            List.of()
        );
    }

    public CategoryDto updateCategoryMetadata(CategoryDto draft, String id, String displayName, String description, String version) {
        requireDraft(draft);
        return replace(
            draft,
            normalizeText(id),
            normalizeText(version),
            normalizeText(displayName),
            description == null ? "" : description
        );
    }

    public CategoryDto updatePages(CategoryDto draft, PageSelectionDto pages) {
        requireDraft(draft);
        return replace(draft, draft.id(), draft.version(), draft.displayName(), draft.description(), pages, draft.ocr(),
            draft.identification(), draft.geometry(), draft.anchors(), draft.fields());
    }

    public CategoryDto updateOcr(CategoryDto draft, OcrSettingsDto ocr) {
        requireDraft(draft);
        return replace(draft, draft.id(), draft.version(), draft.displayName(), draft.description(), draft.pages(), ocr,
            draft.identification(), draft.geometry(), draft.anchors(), draft.fields());
    }

    public CategoryDto updateGeometry(CategoryDto draft, GeometryDto geometry) {
        requireDraft(draft);
        return replace(draft, draft.id(), draft.version(), draft.displayName(), draft.description(), draft.pages(), draft.ocr(),
            draft.identification(), geometry, draft.anchors(), draft.fields());
    }

    public CategoryDto addIdentificationGroup(CategoryDto draft, ConditionGroupDto group) {
        requireDraft(draft);
        var identification = identification(draft);
        return withIdentificationGroups(draft, add(identification.groups(), requireNonNull(group, "condition group")));
    }

    public CategoryDto removeIdentificationGroup(CategoryDto draft, int index) {
        requireDraft(draft);
        return withIdentificationGroups(draft, remove(identification(draft).groups(), index, "identification group"));
    }

    public CategoryDto moveIdentificationGroup(CategoryDto draft, int fromIndex, int toIndex) {
        requireDraft(draft);
        return withIdentificationGroups(draft, move(identification(draft).groups(), fromIndex, toIndex, "identification group"));
    }

    public CategoryDto addCondition(CategoryDto draft, int groupIndex, ConditionDto condition) {
        requireDraft(draft);
        return updateGroup(draft, groupIndex, group -> new ConditionGroupDto(add(group.conditions(), requireNonNull(condition, "condition"))));
    }

    public CategoryDto replaceCondition(CategoryDto draft, int groupIndex, int conditionIndex, ConditionDto condition) {
        requireDraft(draft);
        requireNonNull(condition, "condition");
        return updateGroup(draft, groupIndex, group -> new ConditionGroupDto(replace(group.conditions(), conditionIndex, condition, "condition")));
    }

    public CategoryDto removeCondition(CategoryDto draft, int groupIndex, int conditionIndex) {
        requireDraft(draft);
        return updateGroup(draft, groupIndex, group -> new ConditionGroupDto(remove(group.conditions(), conditionIndex, "condition")));
    }

    public CategoryDto moveCondition(CategoryDto draft, int groupIndex, int fromIndex, int toIndex) {
        requireDraft(draft);
        return updateGroup(draft, groupIndex, group -> new ConditionGroupDto(move(group.conditions(), fromIndex, toIndex, "condition")));
    }

    public CategoryDto addAnchor(CategoryDto draft, AnchorDto anchor) {
        requireDraft(draft);
        requireUniqueId(draft.anchors(), requireNonNull(anchor, "anchor").id(), "anchor");
        return withAnchors(draft, add(draft.anchors(), anchor));
    }

    public CategoryDto replaceAnchor(CategoryDto draft, int index, AnchorDto anchor) {
        requireDraft(draft);
        requireNonNull(anchor, "anchor");
        requireUniqueId(draft.anchors(), anchor.id(), "anchor", index);
        return withAnchors(draft, replace(draft.anchors(), index, anchor, "anchor"));
    }

    public CategoryDto removeAnchor(CategoryDto draft, int index) {
        requireDraft(draft);
        return withAnchors(draft, remove(draft.anchors(), index, "anchor"));
    }

    public CategoryDto moveAnchor(CategoryDto draft, int fromIndex, int toIndex) {
        requireDraft(draft);
        return withAnchors(draft, move(draft.anchors(), fromIndex, toIndex, "anchor"));
    }

    public CategoryDto addField(CategoryDto draft, FieldDto field) {
        requireDraft(draft);
        requireUniqueId(draft.fields(), requireNonNull(field, "field").id(), "field");
        return withFields(draft, add(draft.fields(), field));
    }

    public CategoryDto replaceField(CategoryDto draft, int index, FieldDto field) {
        requireDraft(draft);
        requireNonNull(field, "field");
        requireUniqueId(draft.fields(), field.id(), "field", index);
        return withFields(draft, replace(draft.fields(), index, field, "field"));
    }

    public CategoryDto removeField(CategoryDto draft, int index) {
        requireDraft(draft);
        return withFields(draft, remove(draft.fields(), index, "field"));
    }

    public CategoryDto moveField(CategoryDto draft, int fromIndex, int toIndex) {
        requireDraft(draft);
        return withFields(draft, move(draft.fields(), fromIndex, toIndex, "field"));
    }

    public CategoryDto updateFieldOcr(CategoryDto draft, int fieldIndex, OcrSettingsDto ocr) {
        return updateField(draft, fieldIndex, field -> copyField(field, field.region(), field.output(), ocr,
            field.imageProcessors(), field.transformers(), field.validators()));
    }

    public CategoryDto updateFieldOutput(CategoryDto draft, int fieldIndex, OutputDto output) {
        return updateField(draft, fieldIndex, field -> copyField(field, field.region(), output, field.ocr(),
            field.imageProcessors(), field.transformers(), field.validators()));
    }

    public CategoryDto updateFieldRegion(CategoryDto draft, int fieldIndex, RegionDto region) {
        return updateField(draft, fieldIndex, field -> copyField(field, region, field.output(), field.ocr(),
            field.imageProcessors(), field.transformers(), field.validators()));
    }

    public CategoryDto addImageProcessor(CategoryDto draft, int fieldIndex, ExtensionRefDto step) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.IMAGE_PROCESSORS, steps -> add(steps, requireNonNull(step, "image processor")));
    }

    public CategoryDto removeImageProcessor(CategoryDto draft, int fieldIndex, int stepIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.IMAGE_PROCESSORS, steps -> remove(steps, stepIndex, "image processor"));
    }

    public CategoryDto moveImageProcessor(CategoryDto draft, int fieldIndex, int fromIndex, int toIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.IMAGE_PROCESSORS, steps -> move(steps, fromIndex, toIndex, "image processor"));
    }

    public CategoryDto duplicateImageProcessor(CategoryDto draft, int fieldIndex, int stepIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.IMAGE_PROCESSORS, steps -> duplicate(steps, stepIndex, "image processor"));
    }

    public CategoryDto addTransformer(CategoryDto draft, int fieldIndex, ExtensionRefDto step) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.TRANSFORMERS, steps -> add(steps, requireNonNull(step, "transformer")));
    }

    public CategoryDto removeTransformer(CategoryDto draft, int fieldIndex, int stepIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.TRANSFORMERS, steps -> remove(steps, stepIndex, "transformer"));
    }

    public CategoryDto moveTransformer(CategoryDto draft, int fieldIndex, int fromIndex, int toIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.TRANSFORMERS, steps -> move(steps, fromIndex, toIndex, "transformer"));
    }

    public CategoryDto duplicateTransformer(CategoryDto draft, int fieldIndex, int stepIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.TRANSFORMERS, steps -> duplicate(steps, stepIndex, "transformer"));
    }

    public CategoryDto addValidator(CategoryDto draft, int fieldIndex, ExtensionRefDto step) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.VALIDATORS, steps -> add(steps, requireNonNull(step, "validator")));
    }

    public CategoryDto removeValidator(CategoryDto draft, int fieldIndex, int stepIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.VALIDATORS, steps -> remove(steps, stepIndex, "validator"));
    }

    public CategoryDto moveValidator(CategoryDto draft, int fieldIndex, int fromIndex, int toIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.VALIDATORS, steps -> move(steps, fromIndex, toIndex, "validator"));
    }

    public CategoryDto duplicateValidator(CategoryDto draft, int fieldIndex, int stepIndex) {
        return updateFieldPipeline(draft, fieldIndex, Pipeline.VALIDATORS, steps -> duplicate(steps, stepIndex, "validator"));
    }

    private CategoryDto updateGroup(CategoryDto draft, int groupIndex, Function<ConditionGroupDto, ConditionGroupDto> updater) {
        var groups = identification(draft).groups();
        requireIndex(groups, groupIndex, "identification group");
        return withIdentificationGroups(draft, replace(groups, groupIndex, updater.apply(groups.get(groupIndex)), "identification group"));
    }

    private CategoryDto updateField(CategoryDto draft, int fieldIndex, Function<FieldDto, FieldDto> updater) {
        requireDraft(draft);
        var fields = list(draft.fields());
        requireIndex(fields, fieldIndex, "field");
        return replaceField(draft, fieldIndex, updater.apply(fields.get(fieldIndex)));
    }

    private CategoryDto updateFieldPipeline(CategoryDto draft, int fieldIndex, Pipeline pipeline,
                                            Function<List<ExtensionRefDto>, List<ExtensionRefDto>> updater) {
        return updateField(draft, fieldIndex, field -> switch (pipeline) {
            case IMAGE_PROCESSORS -> copyField(field, field.region(), field.output(), field.ocr(),
                updater.apply(field.imageProcessors()), field.transformers(), field.validators());
            case TRANSFORMERS -> copyField(field, field.region(), field.output(), field.ocr(),
                field.imageProcessors(), updater.apply(field.transformers()), field.validators());
            case VALIDATORS -> copyField(field, field.region(), field.output(), field.ocr(),
                field.imageProcessors(), field.transformers(), updater.apply(field.validators()));
        });
    }

    private CategoryDto withIdentificationGroups(CategoryDto draft, List<ConditionGroupDto> groups) {
        return replace(draft, draft.id(), draft.version(), draft.displayName(), draft.description(), draft.pages(), draft.ocr(),
            new IdentificationDto(groups), draft.geometry(), draft.anchors(), draft.fields());
    }

    private CategoryDto withAnchors(CategoryDto draft, List<AnchorDto> anchors) {
        return replace(draft, draft.id(), draft.version(), draft.displayName(), draft.description(), draft.pages(), draft.ocr(),
            draft.identification(), draft.geometry(), anchors, draft.fields());
    }

    private CategoryDto withFields(CategoryDto draft, List<FieldDto> fields) {
        return replace(draft, draft.id(), draft.version(), draft.displayName(), draft.description(), draft.pages(), draft.ocr(),
            draft.identification(), draft.geometry(), draft.anchors(), fields);
    }

    private CategoryDto replace(CategoryDto draft, String id, String version, String displayName, String description) {
        return replace(draft, id, version, displayName, description, draft.pages(), draft.ocr(), draft.identification(),
            draft.geometry(), draft.anchors(), draft.fields());
    }

    private CategoryDto replace(CategoryDto draft, String id, String version, String displayName, String description,
                                PageSelectionDto pages, OcrSettingsDto ocr, IdentificationDto identification,
                                GeometryDto geometry, List<AnchorDto> anchors, List<FieldDto> fields) {
        return new CategoryDto(draft.schemaVersion(), id, version, displayName, description, draft.referenceDocuments(), pages, ocr,
            identification, geometry, list(anchors), list(fields));
    }

    private FieldDto copyField(FieldDto field, RegionDto region, OutputDto output, OcrSettingsDto ocr,
                               List<ExtensionRefDto> imageProcessors, List<ExtensionRefDto> transformers,
                               List<ExtensionRefDto> validators) {
        return new FieldDto(field.id(), field.displayName(), field.page(), region, field.required(), ocr, output,
            list(imageProcessors), list(transformers), list(validators));
    }

    private IdentificationDto identification(CategoryDto draft) {
        return draft.identification() == null ? new IdentificationDto(List.of()) : new IdentificationDto(list(draft.identification().groups()));
    }

    private static <T> List<T> add(List<T> source, T value) {
        var copy = new ArrayList<>(list(source));
        copy.add(value);
        return List.copyOf(copy);
    }

    private static <T> List<T> replace(List<T> source, int index, T value, String elementName) {
        var copy = new ArrayList<>(list(source));
        requireIndex(copy, index, elementName);
        copy.set(index, value);
        return List.copyOf(copy);
    }

    private static <T> List<T> remove(List<T> source, int index, String elementName) {
        var copy = new ArrayList<>(list(source));
        requireIndex(copy, index, elementName);
        copy.remove(index);
        return List.copyOf(copy);
    }

    private static <T> List<T> move(List<T> source, int fromIndex, int toIndex, String elementName) {
        var copy = new ArrayList<>(list(source));
        requireIndex(copy, fromIndex, elementName);
        requireIndex(copy, toIndex, elementName);
        if (fromIndex == toIndex) {
            return List.copyOf(copy);
        }
        var value = copy.remove(fromIndex);
        copy.add(toIndex, value);
        return List.copyOf(copy);
    }

    private static <T> List<T> duplicate(List<T> source, int index, String elementName) {
        var copy = new ArrayList<>(list(source));
        requireIndex(copy, index, elementName);
        copy.add(index + 1, copy.get(index));
        return List.copyOf(copy);
    }

    private static <T> List<T> list(List<T> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    private static void requireDraft(CategoryDto draft) {
        requireNonNull(draft, "category draft");
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new DraftMutationException("Missing " + name);
        }
        return value;
    }

    private static void requireIndex(List<?> values, int index, String elementName) {
        if (index < 0 || index >= values.size()) {
            throw new DraftMutationException("Invalid " + elementName + " index " + index + " for size " + values.size());
        }
    }

    private static String normalizeRequired(String value, String name, String defaultValue) {
        if (value == null || value.isBlank()) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new DraftMutationException("Missing " + name);
        }
        return value.trim();
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static void requireUniqueId(List<? extends Object> elements, String id, String elementName) {
        requireUniqueId(elements, id, elementName, -1);
    }

    private static void requireUniqueId(List<? extends Object> elements, String id, String elementName, int allowedIndex) {
        var normalizedId = normalizeRequired(id, elementName + " id", null);
        var copy = list(elements);
        for (int i = 0; i < copy.size(); i++) {
            if (i == allowedIndex) {
                continue;
            }
            var existingId = switch (copy.get(i)) {
                case AnchorDto anchor -> anchor.id();
                case FieldDto field -> field.id();
                default -> null;
            };
            if (Objects.equals(existingId, normalizedId)) {
                throw new DraftMutationException("Duplicate " + elementName + " id: " + normalizedId);
            }
        }
    }

    private enum Pipeline {
        IMAGE_PROCESSORS,
        TRANSFORMERS,
        VALIDATORS
    }
}
