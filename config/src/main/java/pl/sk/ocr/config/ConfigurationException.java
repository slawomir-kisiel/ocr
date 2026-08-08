package pl.sk.ocr.config;

import java.util.List;

public class ConfigurationException extends RuntimeException {
    private final List<ConfigurationProblem> problems;

    public ConfigurationException(List<ConfigurationProblem> problems) {
        super("Configuration is invalid: " + problems);
        this.problems = List.copyOf(problems);
    }

    public List<ConfigurationProblem> problems() {
        return problems;
    }
}
