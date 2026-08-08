package pl.sk.ocr.domain.config;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.FieldId;

public record CategoryConfiguration(
    CategoryId id,
    ConfigurationVersion version,
    String displayName,
    List<FieldId> fields
) {
    public CategoryConfiguration {
        id = Validation.requireNonNull(id, "category id");
        version = Validation.requireNonNull(version, "configuration version");
        displayName = Validation.requireText(displayName, "display name");
        fields = List.copyOf(Validation.requireNoNulls(fields == null ? List.of() : fields, "fields"));
    }
}
