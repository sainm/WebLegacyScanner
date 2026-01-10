package io.github.sainm.weblegacyscan.rules.impl;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.AbstractRule;
import io.github.sainm.weblegacyscan.rules.RuleDefinition;

import java.util.Optional;

/**
 * CSS deprecated property rule
 */
public class CSSDeprecatedPropRule extends AbstractRule {

    public CSSDeprecatedPropRule(RuleDefinition definition) {
        super(definition);
    }

    @Override
    protected Optional<Issue> doEvaluate(CodeElement element) {
        if (!(element instanceof CSSElement cssElement)) {
            return Optional.empty();
        }

        if (cssElement.type() != CSSElementType.DECLARATION) {
            return Optional.empty();
        }

        String property = cssElement.property();
        String pattern = definition.pattern();

        if (pattern != null && property != null && property.equalsIgnoreCase(pattern)) {
            Location location = cssElement.propertyLocation() != null 
                ? cssElement.propertyLocation() 
                : cssElement.location();
            
            return Optional.of(Issue.builder()
                .ruleId(getId())
                .category(getCategory())
                .severity(getSeverity())
                .location(location)
                .description("Deprecated CSS property '" + property + "': " + definition.description())
                .suggestion(definition.suggestion())
                .mdnReference(definition.mdnReference())
                .build());
        }

        return Optional.empty();
    }
}
