package io.github.sainm.weblegacyscan.rules.impl;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.AbstractRule;
import io.github.sainm.weblegacyscan.rules.RuleDefinition;

import java.util.Optional;

/**
 * HTML 废弃标签规则
 */
public class HTMLDeprecatedTagRule extends AbstractRule {

    public HTMLDeprecatedTagRule(RuleDefinition definition) {
        super(definition);
    }

    @Override
    protected Optional<Issue> doEvaluate(CodeElement element) {
        if (!(element instanceof HTMLElement htmlElement)) {
            return Optional.empty();
        }

        String tagName = htmlElement.tagName().toLowerCase();
        String pattern = definition.pattern();

        if (pattern != null && tagName.equals(pattern.toLowerCase())) {
            return Optional.of(createIssue(element, 
                "Deprecated HTML tag <" + tagName + ">: " + definition.description()));
        }

        return Optional.empty();
    }
}
