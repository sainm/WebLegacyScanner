package io.github.sainm.weblegacyscan.rules.impl;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.AbstractRule;
import io.github.sainm.weblegacyscan.rules.RuleDefinition;

import java.util.Optional;

/**
 * JavaScript 废弃 API 规则
 */
public class JSDeprecatedAPIRule extends AbstractRule {

    public JSDeprecatedAPIRule(RuleDefinition definition) {
        super(definition);
    }

    @Override
    protected Optional<Issue> doEvaluate(CodeElement element) {
        if (!(element instanceof JSElement jsElement)) {
            return Optional.empty();
        }

        if (jsElement.type() != JSElementType.FUNCTION_CALL && 
            jsElement.type() != JSElementType.METHOD_CALL) {
            return Optional.empty();
        }

        String identifier = jsElement.identifier();
        String pattern = definition.pattern();

        if (pattern != null && identifier != null) {
            // 检查完全匹配或�?pattern 结尾（如 document.write�?
            if (identifier.equals(pattern) || identifier.endsWith("." + pattern)) {
                Location location = jsElement.identifierLocation() != null 
                    ? jsElement.identifierLocation() 
                    : jsElement.location();
                
                return Optional.of(Issue.builder()
                    .ruleId(getId())
                    .category(getCategory())
                    .severity(getSeverity())
                    .location(location)
                    .description("Deprecated JavaScript API '" + identifier + "': " + definition.description())
                    .suggestion(definition.suggestion())
                    .mdnReference(definition.mdnReference())
                    .build());
            }
        }

        return Optional.empty();
    }
}
