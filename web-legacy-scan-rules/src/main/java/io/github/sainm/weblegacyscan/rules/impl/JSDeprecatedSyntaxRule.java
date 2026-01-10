package io.github.sainm.weblegacyscan.rules.impl;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.AbstractRule;
import io.github.sainm.weblegacyscan.rules.RuleDefinition;

import java.util.Optional;

/**
 * JavaScript deprecated syntax rule
 */
public class JSDeprecatedSyntaxRule extends AbstractRule {

    public JSDeprecatedSyntaxRule(RuleDefinition definition) {
        super(definition);
    }

    @Override
    protected Optional<Issue> doEvaluate(CodeElement element) {
        if (!(element instanceof JSElement jsElement)) {
            return Optional.empty();
        }

        String pattern = definition.pattern();
        if (pattern == null) return Optional.empty();

        // Check var keyword
        if (pattern.equals("var") && jsElement.type() == JSElementType.VARIABLE_DECLARATION) {
            String identifier = jsElement.identifier();
            if (identifier != null && identifier.startsWith("var ")) {
                return Optional.of(createIssue(element, 
                    "Use 'let' or 'const' instead of 'var'"));
            }
        }

        return Optional.empty();
    }
}
