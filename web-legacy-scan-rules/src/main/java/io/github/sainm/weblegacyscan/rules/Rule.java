package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;

import java.util.Optional;

/**
 * Rule interface (Chain of Responsibility pattern)
 */
public interface Rule {
    
    /**
     * Get rule ID
     */
    String getId();

    /**
     * Get rule category
     */
    RuleCategory getCategory();

    /**
     * Get severity level
     */
    SeverityLevel getSeverity();

    /**
     * Evaluate if code element violates the rule
     * @param element code element
     * @return Issue if rule is violated; otherwise empty
     */
    Optional<Issue> evaluate(CodeElement element);

    /**
     * Get next rule (chain of responsibility)
     */
    Optional<Rule> getNext();

    /**
     * Set next rule
     */
    void setNext(Rule next);

    /**
     * Check if rule is enabled
     */
    boolean isEnabled();

    /**
     * Check if rule applies to specified file
     */
    boolean matchesFile(String filePath);
}
