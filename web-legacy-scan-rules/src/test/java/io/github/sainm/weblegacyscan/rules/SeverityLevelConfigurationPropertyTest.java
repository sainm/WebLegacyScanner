package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Property-based tests for Severity Level Configuration.
 * 
 * Property 8: Severity Level Configuration
 * For any Rule with a custom Severity_Level configured, all Issues produced 
 * by that rule SHALL have the configured Severity_Level.
 * 
 * Validates: Requirements 4.5
 */
class SeverityLevelConfigurationPropertyTest {

    /**
     * Property 8: Severity Level Configuration
     * 
     * For any Rule with a custom Severity_Level configured, all Issues produced
     * by that rule SHALL have the configured Severity_Level.
     * 
     * Feature: web-legacy-scan, Property 8: Severity Level Configuration
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void issuesHaveConfiguredSeverityLevel(
            @ForAll("ruleWithMatchingElement") RuleElementPair pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        // Evaluate the element
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // If an issue is produced, it must have the configured severity level
        if (result.isPresent()) {
            Issue issue = result.get();
            SeverityLevel configuredSeverity = pair.rule().severity();
            
            assert issue.severity() == configuredSeverity :
                "Issue severity should be " + configuredSeverity + 
                " (configured), but got " + issue.severity();
        }
    }

    /**
     * Property: All severity levels can be configured
     * 
     * For any SeverityLevel value, a Rule can be configured with that severity
     * and Issues produced will have that severity.
     * 
     * Feature: web-legacy-scan, Property 8: Severity Level Configuration
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void allSeverityLevelsCanBeConfigured(
            @ForAll("severityLevel") SeverityLevel severity,
            @ForAll("deprecatedHtmlTag") String tagName
    ) {
        // Create rule with specific severity
        RuleDefinition rule = RuleDefinition.builder()
                .id("test-rule-" + tagName + "-" + severity.name())
                .category(RuleCategory.HTML_DEPRECATED_TAG)
                .severity(severity)
                .pattern(tagName)
                .description("Test rule for " + tagName)
                .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                .mdnReference("https://developer.mozilla.org/")
                .enabled(true)
                .build();
        
        // Create matching element
        HTMLElement element = createHTMLElement(tagName);
        
        // Create and evaluate rule
        RuleFactory factory = new RuleFactory();
        Rule ruleInstance = factory.createRule(rule);
        Optional<Issue> result = ruleInstance.evaluate(element);
        
        // Verify issue has configured severity
        assert result.isPresent() : "Rule should produce an issue for matching element";
        assert result.get().severity() == severity :
            "Issue severity should be " + severity + ", but got " + result.get().severity();
    }

    /**
     * Property: Severity level is preserved through rule creation
     * 
     * For any RuleDefinition with a specific severity, the created Rule
     * SHALL report the same severity via getSeverity().
     * 
     * Feature: web-legacy-scan, Property 8: Severity Level Configuration
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void severityPreservedThroughRuleCreation(
            @ForAll("ruleDefinitionWithAnySeverity") RuleDefinition definition
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(definition);
        
        assert rule.getSeverity() == definition.severity() :
            "Rule severity should be " + definition.severity() + 
            " (from definition), but got " + rule.getSeverity();
    }

    /**
     * Property: Different rules can have different severity levels
     * 
     * For any two rules with different configured severities, their produced
     * Issues SHALL have their respective configured severities.
     * 
     * Feature: web-legacy-scan, Property 8: Severity Level Configuration
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void differentRulesHaveDifferentSeverities(
            @ForAll("twoRulesWithDifferentSeverities") TwoRulePair rulePair
    ) {
        RuleFactory factory = new RuleFactory();
        
        Rule rule1 = factory.createRule(rulePair.rule1());
        Rule rule2 = factory.createRule(rulePair.rule2());
        
        Optional<Issue> result1 = rule1.evaluate(rulePair.element1());
        Optional<Issue> result2 = rule2.evaluate(rulePair.element2());
        
        assert result1.isPresent() : "First rule should produce an issue";
        assert result2.isPresent() : "Second rule should produce an issue";
        
        assert result1.get().severity() == rulePair.rule1().severity() :
            "First issue severity mismatch";
        assert result2.get().severity() == rulePair.rule2().severity() :
            "Second issue severity mismatch";
        
        // Verify they are actually different
        assert result1.get().severity() != result2.get().severity() :
            "Issues should have different severities";
    }

    // Helper record for pairing rules with matching elements
    record RuleElementPair(RuleDefinition rule, CodeElement element) {}

    // Helper record for two rules with different severities
    record TwoRulePair(
            RuleDefinition rule1, CodeElement element1,
            RuleDefinition rule2, CodeElement element2
    ) {}

    @Provide
    Arbitrary<SeverityLevel> severityLevel() {
        return Arbitraries.of(SeverityLevel.values());
    }

    @Provide
    Arbitrary<String> deprecatedHtmlTag() {
        return Arbitraries.of("font", "center", "marquee", "blink", "big", "strike");
    }

    @Provide
    Arbitrary<RuleElementPair> ruleWithMatchingElement() {
        return Combinators.combine(
                Arbitraries.of(SeverityLevel.values()),
                Arbitraries.of("font", "center", "marquee", "blink", "big", "strike")
        ).as((severity, tagName) -> {
            RuleDefinition rule = RuleDefinition.builder()
                    .id("html-deprecated-tag-" + tagName)
                    .category(RuleCategory.HTML_DEPRECATED_TAG)
                    .severity(severity)
                    .pattern(tagName)
                    .description("Deprecated HTML tag <" + tagName + ">")
                    .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                    .mdnReference("https://developer.mozilla.org/en-US/docs/Web/HTML/Element/" + tagName)
                    .enabled(true)
                    .build();
            
            HTMLElement element = createHTMLElement(tagName);
            return new RuleElementPair(rule, element);
        });
    }

    @Provide
    Arbitrary<RuleDefinition> ruleDefinitionWithAnySeverity() {
        return Combinators.combine(
                Arbitraries.of(RuleCategory.values()),
                Arbitraries.of(SeverityLevel.values()),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        ).as((category, severity, pattern) -> RuleDefinition.builder()
                .id("test-rule-" + category.name().toLowerCase() + "-" + pattern)
                .category(category)
                .severity(severity)
                .pattern(pattern)
                .description("Test rule for " + pattern)
                .suggestion(ReplacementSuggestion.of("Use modern alternative", "modern"))
                .mdnReference("https://developer.mozilla.org/")
                .enabled(true)
                .build());
    }

    @Provide
    Arbitrary<TwoRulePair> twoRulesWithDifferentSeverities() {
        // Generate pairs of different severity levels
        return Arbitraries.of(
                new SeverityLevel[]{SeverityLevel.ERROR, SeverityLevel.WARNING},
                new SeverityLevel[]{SeverityLevel.ERROR, SeverityLevel.INFO},
                new SeverityLevel[]{SeverityLevel.WARNING, SeverityLevel.INFO}
        ).flatMap(severities -> 
            Combinators.combine(
                    Arbitraries.of("font", "center", "marquee"),
                    Arbitraries.of("blink", "big", "strike")
            ).as((tag1, tag2) -> {
                RuleDefinition rule1 = RuleDefinition.builder()
                        .id("html-deprecated-tag-" + tag1)
                        .category(RuleCategory.HTML_DEPRECATED_TAG)
                        .severity(severities[0])
                        .pattern(tag1)
                        .description("Deprecated HTML tag <" + tag1 + ">")
                        .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                        .mdnReference("https://developer.mozilla.org/")
                        .enabled(true)
                        .build();
                
                RuleDefinition rule2 = RuleDefinition.builder()
                        .id("html-deprecated-tag-" + tag2)
                        .category(RuleCategory.HTML_DEPRECATED_TAG)
                        .severity(severities[1])
                        .pattern(tag2)
                        .description("Deprecated HTML tag <" + tag2 + ">")
                        .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                        .mdnReference("https://developer.mozilla.org/")
                        .enabled(true)
                        .build();
                
                return new TwoRulePair(
                        rule1, createHTMLElement(tag1),
                        rule2, createHTMLElement(tag2)
                );
            })
        );
    }

    private static HTMLElement createHTMLElement(String tagName) {
        Path testPath = Path.of("test.html");
        Location location = Location.of(testPath, 1, 1, 1, tagName.length() + 2);
        
        return HTMLElement.builder()
                .tagName(tagName)
                .tagNameLocation(location)
                .location(location)
                .rawContent("<" + tagName + ">")
                .elementType(HTMLElementType.OPEN_TAG)
                .build();
    }
}
