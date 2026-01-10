package io.github.sainm.weblegacyscan.rules.impl;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.AbstractRule;
import io.github.sainm.weblegacyscan.rules.RuleDefinition;

import java.util.Optional;

/**
 * HTML 废弃属性规�?
 */
public class HTMLDeprecatedAttrRule extends AbstractRule {

    public HTMLDeprecatedAttrRule(RuleDefinition definition) {
        super(definition);
    }

    @Override
    protected Optional<Issue> doEvaluate(CodeElement element) {
        if (!(element instanceof HTMLElement htmlElement)) {
            return Optional.empty();
        }

        String pattern = definition.pattern();
        if (pattern == null) return Optional.empty();

        AttributeInfo attr = htmlElement.getAttribute(pattern);
        if (attr != null) {
            return Optional.of(Issue.builder()
                .ruleId(getId())
                .category(getCategory())
                .severity(getSeverity())
                .location(attr.fullLocation())
                .description("Deprecated HTML attribute '" + pattern + "': " + definition.description())
                .suggestion(definition.suggestion())
                .mdnReference(definition.mdnReference())
                .build());
        }

        return Optional.empty();
    }
}
