package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Property-based tests for File Pattern Rule Matching.
 * 
 * Property 9: File Pattern Rule Matching
 * For any Rule with file pattern restrictions, the Rule SHALL only produce 
 * Issues for files matching the specified patterns.
 * 
 * Validates: Requirements 4.10, 4.11
 */
class FilePatternRuleMatchingPropertyTest {

    /**
     * Property 9: File Pattern Rule Matching
     * 
     * For any Rule with file pattern restrictions, the Rule SHALL only produce
     * Issues for files matching the specified patterns.
     * 
     * This is enforced at the RuleEngine level via getRulesForFile(), which filters
     * rules based on file patterns before evaluation.
     * 
     * Feature: web-legacy-scan, Property 9: File Pattern Rule Matching
     * Validates: Requirements 4.10, 4.11
     */
    @Property(tries = 100)
    void rulesWithFilePatternsOnlyMatchSpecifiedFiles(
            @ForAll("ruleWithFilePatterns") RuleWithFileContext context
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(context.rule());
        RuleRegistry registry = new RuleRegistry();
        registry.register(rule);
        
        // Get rules applicable to this file
        List<Rule> applicableRules = registry.getRulesForFile(context.filePath());
        
        // Check if the file path matches the rule's file patterns
        boolean shouldMatch = rule.matchesFile(context.filePath());
        
        if (shouldMatch) {
            // If file matches, rule should be in applicable rules
            assert applicableRules.contains(rule) :
                "Rule '" + context.rule().id() + "' with file patterns " + context.rule().filePatterns() +
                " should be applicable for matching file '" + context.filePath() + "'";
        } else {
            // If file doesn't match patterns, rule should NOT be in applicable rules
            assert !applicableRules.contains(rule) :
                "Rule '" + context.rule().id() + "' with file patterns " + context.rule().filePatterns() +
                " should NOT be applicable for non-matching file '" + context.filePath() + "'";
        }
    }

    /**
     * Property: Rules with matching file patterns produce issues
     * 
     * For any Rule with file pattern restrictions, when evaluated against a file
     * that matches the patterns, the Rule SHALL produce Issues for matching elements.
     * 
     * Feature: web-legacy-scan, Property 9: File Pattern Rule Matching
     * Validates: Requirements 4.10, 4.11
     */
    @Property(tries = 100)
    void rulesWithMatchingFilePatternsProduceIssues(
            @ForAll("ruleWithMatchingFile") RuleWithFileContext context
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(context.rule());
        
        // Verify file matches
        assert rule.matchesFile(context.filePath()) : 
            "File '" + context.filePath() + "' should match patterns " + context.rule().filePatterns();
        
        // Create matching element
        HTMLElement element = createHTMLElement(context.rule().pattern(), context.filePath());
        Optional<Issue> result = rule.evaluate(element);
        
        assert result.isPresent() : 
            "Rule '" + context.rule().id() + "' should produce issue for matching file '" + context.filePath() + "'";
    }

    /**
     * Property: Rules without file patterns match all files
     * 
     * For any Rule without file pattern restrictions (empty filePatterns),
     * the Rule SHALL match all files.
     * 
     * Feature: web-legacy-scan, Property 9: File Pattern Rule Matching
     * Validates: Requirements 4.10, 4.11
     */
    @Property(tries = 100)
    void rulesWithoutFilePatternsMatchAllFiles(
            @ForAll("ruleWithoutFilePatterns") RuleDefinition rule,
            @ForAll("anyFilePath") String filePath
    ) {
        RuleFactory factory = new RuleFactory();
        Rule ruleInstance = factory.createRule(rule);
        
        // Rule without file patterns should match any file
        assert ruleInstance.matchesFile(filePath) : 
            "Rule without file patterns should match any file, but didn't match '" + filePath + "'";
    }

    /**
     * Property: RuleRegistry filters rules by file path
     * 
     * For any set of rules with different file patterns, getRulesForFile()
     * SHALL return only rules that match the specified file path.
     * 
     * Feature: web-legacy-scan, Property 9: File Pattern Rule Matching
     * Validates: Requirements 4.10, 4.11
     */
    @Property(tries = 100)
    void registryFiltersRulesByFilePath(
            @ForAll("mixedFilePatternRules") List<RuleDefinition> definitions,
            @ForAll("anyFilePath") String filePath
    ) {
        RuleFactory factory = new RuleFactory();
        RuleRegistry registry = new RuleRegistry();
        
        // Register all rules
        for (RuleDefinition def : definitions) {
            Rule rule = factory.createRule(def);
            registry.register(rule);
        }
        
        // Get rules for file
        List<Rule> matchingRules = registry.getRulesForFile(filePath);
        
        // Verify all returned rules match the file
        for (Rule rule : matchingRules) {
            assert rule.matchesFile(filePath) : 
                "Rule '" + rule.getId() + "' returned by getRulesForFile() should match file '" + filePath + "'";
            assert rule.isEnabled() :
                "Rule '" + rule.getId() + "' returned by getRulesForFile() should be enabled";
        }
        
        // Verify no matching rules were excluded
        long expectedCount = definitions.stream()
                .filter(RuleDefinition::enabled)
                .filter(def -> def.matchesFile(filePath))
                .count();
        
        assert matchingRules.size() == expectedCount :
            "Expected " + expectedCount + " matching rules for file '" + filePath + 
            "', but got " + matchingRules.size();
    }

    /**
     * Property: Glob patterns work correctly
     * 
     * For any Rule with glob patterns, the matchesFile() method SHALL correctly
     * match files according to glob semantics.
     * 
     * Feature: web-legacy-scan, Property 9: File Pattern Rule Matching
     * Validates: Requirements 4.10, 4.11
     */
    @Property(tries = 100)
    void globPatternsMatchCorrectly(
            @ForAll("globPatternTestCase") GlobPatternTestCase testCase
    ) {
        RuleDefinition rule = createRuleWithFilePatterns(testCase.patterns());
        
        boolean matches = rule.matchesFile(testCase.filePath());
        
        assert matches == testCase.shouldMatch() :
            "File '" + testCase.filePath() + "' with patterns " + testCase.patterns() +
            " should " + (testCase.shouldMatch() ? "match" : "not match") +
            ", but got " + (matches ? "match" : "no match");
    }

    // Helper records
    record RuleWithFileContext(RuleDefinition rule, String filePath) {}
    record GlobPatternTestCase(List<String> patterns, String filePath, boolean shouldMatch) {}

    @Provide
    Arbitrary<RuleWithFileContext> ruleWithFilePatterns() {
        return Combinators.combine(
                Arbitraries.of("font", "center", "marquee"),
                filePatternList(),
                Arbitraries.of(
                        "src/main/webapp/index.html",
                        "src/test/resources/test.html",
                        "vendor/lib/component.html",
                        "node_modules/package/file.html",
                        "app/views/page.jsp",
                        "public/styles.css"
                )
        ).as((tagName, patterns, filePath) -> {
            RuleDefinition rule = RuleDefinition.builder()
                    .id("html-deprecated-tag-" + tagName)
                    .category(RuleCategory.HTML_DEPRECATED_TAG)
                    .severity(SeverityLevel.WARNING)
                    .pattern(tagName)
                    .description("Deprecated HTML tag <" + tagName + ">")
                    .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                    .mdnReference("https://developer.mozilla.org/")
                    .enabled(true)
                    .filePatterns(patterns)
                    .build();
            return new RuleWithFileContext(rule, filePath);
        });
    }

    @Provide
    Arbitrary<RuleWithFileContext> ruleWithMatchingFile() {
        // Generate rules with patterns that definitely match the file
        return Arbitraries.of(
                new RuleWithFileContext(
                        createRuleWithFilePatterns(List.of("**/*.html")),
                        "src/main/webapp/index.html"
                ),
                new RuleWithFileContext(
                        createRuleWithFilePatterns(List.of("**/*.jsp")),
                        "app/views/page.jsp"
                ),
                new RuleWithFileContext(
                        createRuleWithFilePatterns(List.of("src/**/*.html")),
                        "src/main/webapp/index.html"
                ),
                new RuleWithFileContext(
                        createRuleWithFilePatterns(List.of("*.html")),
                        "index.html"
                ),
                new RuleWithFileContext(
                        createRuleWithFilePatterns(List.of("**/*")),
                        "any/path/file.html"
                )
        );
    }

    @Provide
    Arbitrary<RuleDefinition> ruleWithoutFilePatterns() {
        return Arbitraries.of("font", "center", "marquee")
                .map(tagName -> RuleDefinition.builder()
                        .id("html-deprecated-tag-" + tagName)
                        .category(RuleCategory.HTML_DEPRECATED_TAG)
                        .severity(SeverityLevel.WARNING)
                        .pattern(tagName)
                        .description("Deprecated HTML tag <" + tagName + ">")
                        .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                        .mdnReference("https://developer.mozilla.org/")
                        .enabled(true)
                        .filePatterns(List.of())  // Empty file patterns
                        .build());
    }

    @Provide
    Arbitrary<String> anyFilePath() {
        return Arbitraries.of(
                "index.html",
                "src/main/webapp/index.html",
                "src/test/resources/test.html",
                "vendor/lib/component.html",
                "node_modules/package/file.html",
                "app/views/page.jsp",
                "public/styles.css",
                "scripts/app.js",
                "deep/nested/path/file.html"
        );
    }

    @Provide
    Arbitrary<List<RuleDefinition>> mixedFilePatternRules() {
        return Arbitraries.integers().between(2, 5).flatMap(size -> {
            List<Arbitrary<RuleDefinition>> ruleArbitraries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final int index = i;
                Arbitrary<RuleDefinition> ruleArb = Combinators.combine(
                        Arbitraries.of("font", "center", "marquee", "blink"),
                        Arbitraries.of(true, false),
                        filePatternListOrEmpty()
                ).as((tagName, enabled, patterns) -> RuleDefinition.builder()
                        .id("test-rule-" + index + "-" + tagName)
                        .category(RuleCategory.HTML_DEPRECATED_TAG)
                        .severity(SeverityLevel.WARNING)
                        .pattern(tagName)
                        .description("Test rule for " + tagName)
                        .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                        .mdnReference("https://developer.mozilla.org/")
                        .enabled(enabled)
                        .filePatterns(patterns)
                        .build());
                ruleArbitraries.add(ruleArb);
            }
            return Combinators.combine(ruleArbitraries).as(list -> list);
        });
    }

    @Provide
    Arbitrary<GlobPatternTestCase> globPatternTestCase() {
        return Arbitraries.of(
                // ** matches any path
                new GlobPatternTestCase(List.of("**/*.html"), "src/main/index.html", true),
                new GlobPatternTestCase(List.of("**/*.html"), "index.html", true),
                new GlobPatternTestCase(List.of("**/*.html"), "deep/nested/path/file.html", true),
                new GlobPatternTestCase(List.of("**/*.html"), "file.jsp", false),
                
                // * matches single path segment
                new GlobPatternTestCase(List.of("*.html"), "index.html", true),
                new GlobPatternTestCase(List.of("*.html"), "src/index.html", false),
                
                // Specific directory patterns
                new GlobPatternTestCase(List.of("src/**/*.html"), "src/main/index.html", true),
                new GlobPatternTestCase(List.of("src/**/*.html"), "test/main/index.html", false),
                
                // Multiple patterns (OR logic)
                new GlobPatternTestCase(List.of("**/*.html", "**/*.jsp"), "file.html", true),
                new GlobPatternTestCase(List.of("**/*.html", "**/*.jsp"), "file.jsp", true),
                new GlobPatternTestCase(List.of("**/*.html", "**/*.jsp"), "file.css", false),
                
                // Exclusion-like patterns (vendor, node_modules)
                new GlobPatternTestCase(List.of("src/**/*.html"), "vendor/lib/file.html", false),
                new GlobPatternTestCase(List.of("src/**/*.html"), "node_modules/pkg/file.html", false)
        );
    }

    private Arbitrary<List<String>> filePatternList() {
        return Arbitraries.of(
                List.of("**/*.html"),
                List.of("**/*.jsp"),
                List.of("src/**/*.html"),
                List.of("**/*.html", "**/*.jsp"),
                List.of("*.html")
        );
    }

    private Arbitrary<List<String>> filePatternListOrEmpty() {
        return Arbitraries.of(
                List.of(),  // Empty - matches all
                List.of("**/*.html"),
                List.of("**/*.jsp"),
                List.of("src/**/*.html"),
                List.of("**/*.html", "**/*.jsp")
        );
    }

    private RuleDefinition createRuleWithFilePatterns(List<String> patterns) {
        return RuleDefinition.builder()
                .id("html-deprecated-tag-font")
                .category(RuleCategory.HTML_DEPRECATED_TAG)
                .severity(SeverityLevel.WARNING)
                .pattern("font")
                .description("Deprecated HTML tag <font>")
                .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                .mdnReference("https://developer.mozilla.org/")
                .enabled(true)
                .filePatterns(patterns)
                .build();
    }

    private HTMLElement createHTMLElement(String tagName, String filePath) {
        Path path = Path.of(filePath);
        Location location = Location.of(path, 1, 1, 1, tagName.length() + 2);
        
        return HTMLElement.builder()
                .tagName(tagName)
                .tagNameLocation(location)
                .location(location)
                .rawContent("<" + tagName + ">")
                .elementType(HTMLElementType.OPEN_TAG)
                .build();
    }
}
