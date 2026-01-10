package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.RuleEvaluator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Rule engine interface
 */
public sealed interface RuleEngine extends RuleEvaluator permits DefaultRuleEngine {
    
    /**
     * Load rules from configuration directory
     */
    @Override
    void loadRules(Path configDir) throws IOException;

    /**
     * Reload rules
     */
    void reloadRules() throws IOException;

    /**
     * Evaluate code element
     * @param element code element
     * @param filePath file path (for rule matching)
     * @return list of detected issues
     */
    @Override
    List<Issue> evaluate(CodeElement element, String filePath);

    /**
     * Evaluate multiple code elements
     */
    @Override
    List<Issue> evaluateAll(List<CodeElement> elements, String filePath);

    /**
     * Get rules by category
     */
    List<Rule> getRulesByCategory(RuleCategory category);

    /**
     * Get all rules
     */
    List<Rule> getAllRules();

    /**
     * Get rule registry
     */
    RuleRegistry getRegistry();
}
