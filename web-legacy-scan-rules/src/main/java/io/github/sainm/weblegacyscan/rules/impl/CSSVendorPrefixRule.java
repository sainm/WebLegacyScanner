package io.github.sainm.weblegacyscan.rules.impl;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.AbstractRule;
import io.github.sainm.weblegacyscan.rules.RuleDefinition;

import java.util.Optional;
import java.util.Set;

/**
 * CSS Vendor Prefix 规则
 */
public class CSSVendorPrefixRule extends AbstractRule {

    private static final Set<String> VENDOR_PREFIXES = Set.of(
        "-webkit-", "-moz-", "-ms-", "-o-"
    );

    public CSSVendorPrefixRule(RuleDefinition definition) {
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
        if (property == null) return Optional.empty();

        for (String prefix : VENDOR_PREFIXES) {
            if (property.startsWith(prefix)) {
                String standardProperty = property.substring(prefix.length());
                
                return Optional.of(Issue.builder()
                    .ruleId(getId())
                    .category(getCategory())
                    .severity(getSeverity())
                    .location(cssElement.propertyLocation() != null 
                        ? cssElement.propertyLocation() 
                        : cssElement.location())
                    .description("Vendor-prefixed CSS property '" + property + 
                        "'. Consider using standard property '" + standardProperty + "'")
                    .suggestion(ReplacementSuggestion.of(
                        "Use standard property", 
                        standardProperty))
                    .mdnReference(definition.mdnReference())
                    .build());
            }
        }

        return Optional.empty();
    }
}
