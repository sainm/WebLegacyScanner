package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.model.CodeElement;
import io.github.sainm.weblegacyscan.core.model.Issue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Rule evaluator interface - decouples Scanner from rule engine
 */
public interface RuleEvaluator {
    
    /**
     * Load rules from configuration directory
     */
    void loadRules(Path configDir) throws IOException;

    /**
     * Evaluate code element
     * @param element code element
     * @param filePath file path (for rule matching)
     * @return list of detected issues
     */
    List<Issue> evaluate(CodeElement element, String filePath);

    /**
     * Evaluate multiple code elements
     */
    default List<Issue> evaluateAll(List<CodeElement> elements, String filePath) {
        return elements.stream()
            .flatMap(e -> evaluate(e, filePath).stream())
            .toList();
    }
}
