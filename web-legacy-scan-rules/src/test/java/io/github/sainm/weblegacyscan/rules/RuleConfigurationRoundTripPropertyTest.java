package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.RuleCategory;
import io.github.sainm.weblegacyscan.core.model.ReplacementSuggestion;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Property-based tests for Rule Configuration Round-Trip.
 * 
 * Property 6: Rule Configuration Round-Trip
 * For any valid RuleDefinition, serializing to JSON and deserializing back 
 * SHALL produce an equivalent RuleDefinition.
 * 
 * Validates: Requirements 4.1, 4.2
 */
class RuleConfigurationRoundTripPropertyTest {

    private final RuleLoader ruleLoader = new RuleLoader();

    /**
     * Property 6: Rule Configuration Round-Trip
     * 
     * For any valid RuleDefinition, serializing to JSON and deserializing back
     * SHALL produce an equivalent RuleDefinition.
     * 
     * Feature: web-legacy-scan, Property 6: Rule Configuration Round-Trip
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    void ruleDefinitionRoundTrip(
            @ForAll("validRuleDefinitions") RuleDefinition original
    ) throws IOException {
        // Serialize to JSON
        String json = ruleLoader.serializeRules(List.of(original));
        
        // Create temp directory and write JSON
        Path tempDir = Files.createTempDirectory("rule-test");
        Path rulesFile = tempDir.resolve("rules.json");
        Files.writeString(rulesFile, json);
        
        try {
            // Deserialize back
            List<RuleDefinition> loaded = ruleLoader.loadRules(tempDir);
            
            // Verify round-trip
            assert loaded.size() == 1 : "Should load exactly one rule";
            RuleDefinition roundTripped = loaded.get(0);
            
            assertRuleDefinitionsEquivalent(original, roundTripped);
        } finally {
            // Cleanup
            Files.deleteIfExists(rulesFile);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Property: Multiple rules round-trip preserves all rules
     * 
     * For any list of valid RuleDefinitions, serializing to JSON and deserializing back
     * SHALL preserve all rules with their properties.
     * 
     * Feature: web-legacy-scan, Property 6: Rule Configuration Round-Trip
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 50)
    void multipleRulesRoundTrip(
            @ForAll @Size(min = 1, max = 10) List<@From("validRuleDefinitions") RuleDefinition> originalRules
    ) throws IOException {
        // Serialize to JSON
        String json = ruleLoader.serializeRules(originalRules);
        
        // Create temp directory and write JSON
        Path tempDir = Files.createTempDirectory("rule-test");
        Path rulesFile = tempDir.resolve("rules.json");
        Files.writeString(rulesFile, json);
        
        try {
            // Deserialize back
            List<RuleDefinition> loaded = ruleLoader.loadRules(tempDir);
            
            // Verify count
            assert loaded.size() == originalRules.size() : 
                "Should load same number of rules: expected " + originalRules.size() + ", got " + loaded.size();
            
            // Verify each rule (order should be preserved)
            for (int i = 0; i < originalRules.size(); i++) {
                assertRuleDefinitionsEquivalent(originalRules.get(i), loaded.get(i));
            }
        } finally {
            // Cleanup
            Files.deleteIfExists(rulesFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Provide
    Arbitrary<RuleDefinition> validRuleDefinitions() {
        // First combine: basic fields (id, category, severity, pattern, description)
        Arbitrary<String> ids = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(s -> "rule-" + s);
        
        Arbitrary<RuleCategory> categories = Arbitraries.of(RuleCategory.values());
        
        Arbitrary<SeverityLevel> severities = Arbitraries.of(SeverityLevel.values());
        
        Arbitrary<String> patterns = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .injectNull(0.2);
        
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(5)
                .ofMaxLength(100);
        
        // Second combine: optional fields (mdnReference, enabled, filePatterns, suggestion)
        Arbitrary<String> mdnReferences = Arbitraries.of(
                "https://developer.mozilla.org/en-US/docs/Web/HTML",
                "https://developer.mozilla.org/en-US/docs/Web/CSS",
                "https://developer.mozilla.org/en-US/docs/Web/JavaScript",
                null
        );
        
        Arbitrary<Boolean> enabledFlags = Arbitraries.of(true, false);
        
        Arbitrary<List<String>> filePatternLists = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(s -> "**/*." + s)
                .list()
                .ofMaxSize(3);
        
        Arbitrary<ReplacementSuggestion> suggestions = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(100).injectNull(0.3)
        ).as(ReplacementSuggestion::new).injectNull(0.2);
        
        // Combine first group (5 params)
        Arbitrary<BasicRuleFields> basicFields = Combinators.combine(
                ids, categories, severities, patterns, descriptions
        ).as(BasicRuleFields::new);
        
        // Combine second group (4 params)
        Arbitrary<OptionalRuleFields> optionalFields = Combinators.combine(
                mdnReferences, enabledFlags, filePatternLists, suggestions
        ).as(OptionalRuleFields::new);
        
        // Final combine (2 params)
        return Combinators.combine(basicFields, optionalFields)
                .as((basic, optional) -> RuleDefinition.builder()
                        .id(basic.id)
                        .category(basic.category)
                        .severity(basic.severity)
                        .pattern(basic.pattern)
                        .description(basic.description)
                        .mdnReference(optional.mdnReference)
                        .enabled(optional.enabled)
                        .filePatterns(optional.filePatterns)
                        .suggestion(optional.suggestion)
                        .build()
                );
    }

    // Helper record for basic rule fields
    private record BasicRuleFields(
            String id,
            RuleCategory category,
            SeverityLevel severity,
            String pattern,
            String description
    ) {}

    // Helper record for optional rule fields
    private record OptionalRuleFields(
            String mdnReference,
            boolean enabled,
            List<String> filePatterns,
            ReplacementSuggestion suggestion
    ) {}

    private void assertRuleDefinitionsEquivalent(RuleDefinition expected, RuleDefinition actual) {
        assert expected.id().equals(actual.id()) : 
            "ID mismatch: expected '" + expected.id() + "', got '" + actual.id() + "'";
        assert expected.category() == actual.category() : 
            "Category mismatch: expected " + expected.category() + ", got " + actual.category();
        assert expected.severity() == actual.severity() : 
            "Severity mismatch: expected " + expected.severity() + ", got " + actual.severity();
        assert objectsEqual(expected.pattern(), actual.pattern()) : 
            "Pattern mismatch: expected '" + expected.pattern() + "', got '" + actual.pattern() + "'";
        assert expected.description().equals(actual.description()) : 
            "Description mismatch: expected '" + expected.description() + "', got '" + actual.description() + "'";
        assert objectsEqual(expected.mdnReference(), actual.mdnReference()) : 
            "MDN reference mismatch: expected '" + expected.mdnReference() + "', got '" + actual.mdnReference() + "'";
        assert expected.enabled() == actual.enabled() : 
            "Enabled mismatch: expected " + expected.enabled() + ", got " + actual.enabled();
        assert expected.filePatterns().equals(actual.filePatterns()) : 
            "File patterns mismatch: expected " + expected.filePatterns() + ", got " + actual.filePatterns();
        
        // Check suggestion
        if (expected.suggestion() == null) {
            assert actual.suggestion() == null : 
                "Suggestion should be null but got: " + actual.suggestion();
        } else {
            assert actual.suggestion() != null : 
                "Suggestion should not be null";
            assert expected.suggestion().description().equals(actual.suggestion().description()) : 
                "Suggestion description mismatch";
            assert expected.suggestion().modernAlternative().equals(actual.suggestion().modernAlternative()) : 
                "Suggestion modernAlternative mismatch";
            assert objectsEqual(expected.suggestion().codeExample(), actual.suggestion().codeExample()) : 
                "Suggestion codeExample mismatch";
        }
    }

    private boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
