package pl.sk.ocr.configurator.validation;

import java.util.List;
import pl.sk.ocr.config.CategoryValidator;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.extension.api.ExtensionRegistry;

public final class DraftValidationService {
    private final ExtensionRegistry extensionRegistry;

    public DraftValidationService(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public List<DraftValidationProblem> validate(CategoryDto draft) {
        if (draft == null) {
            return List.of(new DraftValidationProblem("DRAFT_EMPTY", "$", "No category draft is open"));
        }
        return new CategoryValidator(extensionRegistry).validate(draft).stream()
            .map(problem -> new DraftValidationProblem(problem.code(), problem.path(), problem.message()))
            .toList();
    }
}
