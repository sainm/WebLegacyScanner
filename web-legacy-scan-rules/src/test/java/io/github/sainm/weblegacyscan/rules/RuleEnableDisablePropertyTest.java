package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Property-based tests for Rule Enable/Disable Correctness.
 * 
 * Property 7: Rule Enable/Disable Correctness
 * For any Rule that is disabled in configuration, the RuleEngine SHALL not 
 * produce any Issues for that rule, regardless of the input source files.
 * 
 * Validates: Requirements 4.4
 */
class RuleEnableDisablePropertyTest {

    /**
     * Property 7: Rule Enable/Disable Correctness
     * 
     * For any Rule that is disabled in configuration, the RuleEngine SHALL not
     * produce any Issues for that rule, regardless of the input source files.
     * 
     * Feature: web-legacy-scan, Property 7: Rule Enable/Disable Correctness
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void disabledRulesProduceNoIssues(
            @ForAll("disabledRuleDefinitions") RuleDefinition disabledRule,
            @ForAll("matchingCodeElements") CodeElement element
    ) {
        // Create rule from disabled definition
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(disabledRule);
        
        // Verify rule is disabled
        assert !rule.isEnabled() : "Rule should be disabled";
        
        // Evaluate the element - should produce no issues
        Optional<Issue> result = rule.evaluate(element);
        
        assert result.isEmpty() : 
            "Disabled rule '" + disabledRule.id() + "' should not produce any issues, but got: " + result;
    }

    /**
     * Property: Enabled rules can produce issues for matching elements
     * 
     * For any Rule that is enabled in configuration and matches the element pattern,
     * the Rule SHALL produce an Issue when evaluated against a matching element.
     * 
     * Feature: web-legacy-scan, Property 7: Rule Enable/Disable Correctness
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void enabledRulesCanProduceIssues(
            @ForAll("enabledRuleWithMatchingElement") RuleElementPair pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        // Verify rule is enabled
        assert rule.isEnabled() : "Rule should be enabled";
        
        // Evaluate the element - should produce an issue for matching element
        Optional<Issue> result = rule.evaluate(pair.element());
        
        assert result.isPresent() : 
            "Enabled rule '" + pair.rule().id() + "' should produce an issue for matching element";
        
        // Verify the issue has correct rule ID
        assert result.get().ruleId().equals(pair.rule().id()) :
            "Issue should have rule ID '" + pair.rule().id() + "' but got '" + result.get().ruleId() + "'";
    }

    /**
     * Property: Toggling rule enabled state changes behavior
     * 
     * For any Rule, toggling the enabled state should change whether it produces issues.
     * 
     * Feature: web-legacy-scan, Property 7: Rule Enable/Disable Correctness
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void togglingEnabledStateChangesBehavior(
            @ForAll("enabledRuleWithMatchingElement") RuleElementPair pair
    ) {
        RuleFactory factory = new RuleFactory();
        
        // Create enabled rule and verify it produces issues
        Rule enabledRule = factory.createRule(pair.rule());
        Optional<Issue> enabledResult = enabledRule.evaluate(pair.element());
        assert enabledResult.isPresent() : "Enabled rule should produce issue";
        
        // Create disabled version of the same rule
        RuleDefinition disabledDef = RuleDefinition.builder()
                .id(pair.rule().id())
                .category(pair.rule().category())
                .severity(pair.rule().severity())
                .pattern(pair.rule().pattern())
                .description(pair.rule().description())
                .suggestion(pair.rule().suggestion())
                .mdnReference(pair.rule().mdnReference())
                .enabled(false)  // Disabled
                .filePatterns(pair.rule().filePatterns())
                .build();
        
        Rule disabledRule = factory.createRule(disabledDef);
        Optional<Issue> disabledResult = disabledRule.evaluate(pair.element());
        
        assert disabledResult.isEmpty() : 
            "Disabled rule should not produce issue, but got: " + disabledResult;
    }

    /**
     * Property: RuleRegistry filters out disabled rules
     * 
     * For any set of rules with mixed enabled states, the RuleRegistry.getEnabledRules()
     * SHALL return only enabled rules.
     * 
     * Feature: web-legacy-scan, Property 7: Rule Enable/Disable Correctness
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void registryFiltersDisabledRules(
            @ForAll("uniqueIdMixedEnabledRuleDefinitions") List<RuleDefinition> definitions
    ) {
        RuleFactory factory = new RuleFactory();
        RuleRegistry registry = new RuleRegistry();
        
        // Register all rules
        for (RuleDefinition def : definitions) {
            Rule rule = factory.createRule(def);
            registry.register(rule);
        }
        
        // Get enabled rules
        List<Rule> enabledRules = registry.getEnabledRules();
        
        // Count expected enabled rules
        long expectedEnabledCount = definitions.stream()
                .filter(RuleDefinition::enabled)
                .count();
        
        assert enabledRules.size() == expectedEnabledCount :
            "Expected " + expectedEnabledCount + " enabled rules, but got " + enabledRules.size();
        
        // Verify all returned rules are enabled
        for (Rule rule : enabledRules) {
            assert rule.isEnabled() : 
                "Rule '" + rule.getId() + "' in enabled list should be enabled";
        }
    }

    // Helper record for pairing rules with matching elements
    record RuleElementPair(RuleDefinition rule, CodeElement element) {}

    @Provide
    Arbitrary<RuleDefinition> disabledRuleDefinitions() {
        return Combinators.combine(
                Arbitraries.of(RuleCategory.values()),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        ).as((category, pattern) -> createRuleDefinition(category, pattern, false));
    }

    @Provide
    Arbitrary<RuleDefinition> mixedEnabledRuleDefinitions() {
        return Combinators.combine(
                Arbitraries.of(RuleCategory.values()),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                Arbitraries.of(true, false)
        ).as((category, pattern, enabled) -> createRuleDefinition(category, pattern, enabled));
    }

    @Provide
    Arbitrary<List<RuleDefinition>> uniqueIdMixedEnabledRuleDefinitions() {
        // Generate a list of rules with unique IDs by using index-based IDs
        return Arbitraries.integers().between(1, 10).flatMap(size -> {
            List<Arbitrary<RuleDefinition>> ruleArbitraries = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                final int index = i;
                Arbitrary<RuleDefinition> ruleArb = Combinators.combine(
                        Arbitraries.of(RuleCategory.values()),
                        Arbitraries.of(true, false)
                ).as((category, enabled) -> createRuleDefinitionWithUniqueId(category, "pattern" + index, enabled, index));
                ruleArbitraries.add(ruleArb);
            }
            return Combinators.combine(ruleArbitraries).as(list -> list);
        });
    }

    @Provide
    Arbitrary<RuleElementPair> enabledRuleWithMatchingElement() {
        // Generate HTML deprecated tag rules with matching elements
        return Arbitraries.of("font", "center", "marquee", "blink", "big", "strike")
                .map(tagName -> {
                    RuleDefinition rule = RuleDefinition.builder()
                            .id("html-deprecated-tag-" + tagName)
                            .category(RuleCategory.HTML_DEPRECATED_TAG)
                            .severity(SeverityLevel.WARNING)
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
    Arbitrary<CodeElement> matchingCodeElements() {
        // Generate various code elements that could match rules
        return Arbitraries.oneOf(
                htmlElements(),
                cssElements(),
                jsElements()
        );
    }

    private Arbitrary<CodeElement> htmlElements() {
        return Arbitraries.of("font", "center", "div", "span", "p", "marquee")
                .map(this::createHTMLElement);
    }

    private Arbitrary<CodeElement> cssElements() {
        return Arbitraries.of("clip", "zoom", "color", "background")
                .map(this::createCSSElement);
    }

    private Arbitrary<CodeElement> jsElements() {
        return Arbitraries.of("escape", "unescape", "eval", "console.log")
                .map(this::createJSElement);
    }

    private RuleDefinition createRuleDefinition(RuleCategory category, String pattern, boolean enabled) {
        return RuleDefinition.builder()
                .id("test-rule-" + category.name().toLowerCase() + "-" + pattern)
                .category(category)
                .severity(SeverityLevel.WARNING)
                .pattern(pattern)
                .description("Test rule for " + pattern)
                .suggestion(ReplacementSuggestion.of("Use modern alternative", "modern"))
                .mdnReference("https://developer.mozilla.org/")
                .enabled(enabled)
                .build();
    }

    private RuleDefinition createRuleDefinitionWithUniqueId(RuleCategory category, String pattern, boolean enabled, int uniqueIndex) {
        return RuleDefinition.builder()
                .id("test-rule-" + uniqueIndex + "-" + category.name().toLowerCase())
                .category(category)
                .severity(SeverityLevel.WARNING)
                .pattern(pattern)
                .description("Test rule for " + pattern)
                .suggestion(ReplacementSuggestion.of("Use modern alternative", "modern"))
                .mdnReference("https://developer.mozilla.org/")
                .enabled(enabled)
                .build();
    }

    private HTMLElement createHTMLElement(String tagName) {
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

    private CSSElement createCSSElement(String property) {
        Path testPath = Path.of("test.css");
        Location location = Location.of(testPath, 1, 1, 1, property.length() + 10);
        
        return new CSSElement(
                CSSElementType.DECLARATION,
                null,  // selector
                null,  // selectorLocation
                property,
                location,  // propertyLocation
                "value",
                location,  // valueLocation
                location,
                property + ": value;",
                List.of()
        );
    }

    private JSElement createJSElement(String identifier) {
        Path testPath = Path.of("test.js");
        Location location = Location.of(testPath, 1, 1, 1, identifier.length() + 2);
        
        return new JSElement(
                JSElementType.FUNCTION_CALL,
                identifier,
                location,
                List.of(),
                location,
                identifier + "()",
                List.of()
        );
    }
}
