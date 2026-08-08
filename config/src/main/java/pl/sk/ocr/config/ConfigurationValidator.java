package pl.sk.ocr.config;

import java.util.List;

public interface ConfigurationValidator<T> {
    List<ConfigurationProblem> validate(T value);
}
