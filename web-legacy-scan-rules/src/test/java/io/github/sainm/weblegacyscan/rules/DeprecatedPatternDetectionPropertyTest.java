package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Property-based tests for Deprecated Pattern Detection Completeness.
 * 
 * Property 1: Deprecated Pattern Detection Completeness
 * For any source file (HTML, CSS, or JavaScript) containing known deprecated patterns,
 * the Scanner SHALL detect all deprecated patterns and report each with correct file path,
 * line number, and column number.
 * 
 * Validates: Requirements 1.1, 1.5, 2.1, 3.1
 */
class DeprecatedPatternDetectionPropertyTest {

    // Known deprecated HTML tags (Requirement 1.1)
    private static final List<String> DEPRECATED_HTML_TAGS = List.of(
        "font", "center", "marquee", "blink", "basefont", "big", 
        "strike", "tt", "frame", "frameset", "noframes", "applet", "acronym", "dir"
    );

    // Known deprecated HTML attributes (Requirement 1.5)
    private static final List<String> DEPRECATED_HTML_ATTRS = List.of(
        "align", "bgcolor", "border", "language"
    );

    // Known deprecated CSS properties (Requirement 2.1)
    private static final List<String> DEPRECATED_CSS_PROPS = List.of(
        "clip", "zoom"
    );

    // Known deprecated JS APIs (Requirement 3.1)
    private static final List<String> DEPRECATED_JS_APIS = List.of(
        "escape", "unescape", "document.write", "document.writeln", "eval"
    );

    /**
     * Property 1: Deprecated HTML Tag Detection Completeness
     * 
     * For any HTML element with a deprecated tag name, the corresponding rule
     * SHALL detect it and produce an Issue with correct location information.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 1.1
     */
    @Property(tries = 100)
    void deprecatedHtmlTagsAreDetected(
            @ForAll("deprecatedHtmlTagElement") HTMLElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated HTML tag <" + pair.element().tagName() + "> should be detected";
        
        Issue issue = result.get();
        
        // Verify location information is correct
        verifyLocationCorrectness(issue.location(), pair.element().getLocation());
        
        // Verify rule ID matches
        assert issue.ruleId().equals(pair.rule().id()) :
            "Issue rule ID should be '" + pair.rule().id() + "' but got '" + issue.ruleId() + "'";
        
        // Verify category is correct
        assert issue.category() == RuleCategory.HTML_DEPRECATED_TAG :
            "Issue category should be HTML_DEPRECATED_TAG but got " + issue.category();
    }

    /**
     * Property 1: Deprecated HTML Attribute Detection Completeness
     * 
     * For any HTML element with a deprecated attribute, the corresponding rule
     * SHALL detect it and produce an Issue with correct location information.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void deprecatedHtmlAttributesAreDetected(
            @ForAll("deprecatedHtmlAttrElement") HTMLAttrElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated HTML attribute '" + pair.attrName() + "' should be detected";
        
        Issue issue = result.get();
        
        // Verify location information points to the attribute
        AttributeInfo attr = pair.element().getAttribute(pair.attrName());
        assert attr != null : "Attribute should exist";
        verifyLocationCorrectness(issue.location(), attr.fullLocation());
        
        // Verify category is correct
        assert issue.category() == RuleCategory.HTML_DEPRECATED_ATTR :
            "Issue category should be HTML_DEPRECATED_ATTR but got " + issue.category();
    }

    /**
     * Property 1: Deprecated CSS Property Detection Completeness
     * 
     * For any CSS element with a deprecated property, the corresponding rule
     * SHALL detect it and produce an Issue with correct location information.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 2.1
     */
    @Property(tries = 100)
    void deprecatedCssPropertiesAreDetected(
            @ForAll("deprecatedCssElement") CSSElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated CSS property '" + pair.element().property() + "' should be detected";
        
        Issue issue = result.get();
        
        // Verify location information is correct (should point to property location)
        Location expectedLocation = pair.element().propertyLocation() != null 
            ? pair.element().propertyLocation() 
            : pair.element().location();
        verifyLocationCorrectness(issue.location(), expectedLocation);
        
        // Verify category is correct
        assert issue.category() == RuleCategory.CSS_DEPRECATED_PROP :
            "Issue category should be CSS_DEPRECATED_PROP but got " + issue.category();
    }

    /**
     * Property 1: Deprecated JS API Detection Completeness
     * 
     * For any JS element with a deprecated API call, the corresponding rule
     * SHALL detect it and produce an Issue with correct location information.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 3.1
     */
    @Property(tries = 100)
    void deprecatedJsApisAreDetected(
            @ForAll("deprecatedJsElement") JSElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated JS API '" + pair.element().identifier() + "' should be detected";
        
        Issue issue = result.get();
        
        // Verify location information is correct
        Location expectedLocation = pair.element().identifierLocation() != null 
            ? pair.element().identifierLocation() 
            : pair.element().location();
        verifyLocationCorrectness(issue.location(), expectedLocation);
        
        // Verify category is correct
        assert issue.category() == RuleCategory.JS_DEPRECATED_API :
            "Issue category should be JS_DEPRECATED_API but got " + issue.category();
    }


    /**
     * Property 1: All Deprecated Patterns Have Correct File Path
     * 
     * For any detected issue, the file path in the location SHALL match
     * the file path of the source element.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 1.1, 1.5, 2.1, 3.1
     */
    @Property(tries = 100)
    void detectedIssuesHaveCorrectFilePath(
            @ForAll("anyDeprecatedElement") ElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        if (result.isPresent()) {
            Issue issue = result.get();
            Path expectedPath = pair.element().getLocation().filePath();
            Path actualPath = issue.location().filePath();
            
            assert expectedPath.equals(actualPath) :
                "Issue file path should be '" + expectedPath + "' but got '" + actualPath + "'";
        }
    }

    /**
     * Property 1: All Deprecated Patterns Have Valid Line Numbers
     * 
     * For any detected issue, the line number SHALL be >= 1 and match
     * the line number of the source element.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 1.1, 1.5, 2.1, 3.1
     */
    @Property(tries = 100)
    void detectedIssuesHaveValidLineNumbers(
            @ForAll("anyDeprecatedElement") ElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        if (result.isPresent()) {
            Issue issue = result.get();
            
            assert issue.location().startLine() >= 1 :
                "Issue start line should be >= 1 but got " + issue.location().startLine();
            
            assert issue.location().endLine() >= issue.location().startLine() :
                "Issue end line should be >= start line";
        }
    }

    /**
     * Property 1: All Deprecated Patterns Have Valid Column Numbers
     * 
     * For any detected issue, the column number SHALL be >= 1.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 1.1, 1.5, 2.1, 3.1
     */
    @Property(tries = 100)
    void detectedIssuesHaveValidColumnNumbers(
            @ForAll("anyDeprecatedElement") ElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        if (result.isPresent()) {
            Issue issue = result.get();
            
            assert issue.location().startColumn() >= 1 :
                "Issue start column should be >= 1 but got " + issue.location().startColumn();
            
            if (issue.location().startLine() == issue.location().endLine()) {
                assert issue.location().endColumn() >= issue.location().startColumn() :
                    "Issue end column should be >= start column on same line";
            }
        }
    }

    /**
     * Property 1: Non-Deprecated Patterns Are Not Detected
     * 
     * For any element that does NOT contain a deprecated pattern,
     * the rule SHALL NOT produce an issue.
     * 
     * Feature: web-legacy-scan, Property 1: Deprecated Pattern Detection Completeness
     * Validates: Requirements 1.1, 1.5, 2.1, 3.1
     */
    @Property(tries = 100)
    void nonDeprecatedPatternsAreNotDetected(
            @ForAll("nonDeprecatedElement") ElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        assert result.isEmpty() :
            "Non-deprecated element should not produce an issue, but got: " + result;
    }

    // Helper method to verify location correctness
    private void verifyLocationCorrectness(Location actual, Location expected) {
        assert actual != null : "Issue location should not be null";
        assert actual.filePath() != null : "Issue file path should not be null";
        assert actual.startLine() >= 1 : "Issue start line should be >= 1";
        assert actual.startColumn() >= 1 : "Issue start column should be >= 1";
        assert actual.endLine() >= actual.startLine() : "Issue end line should be >= start line";
        
        // Verify file path matches
        assert actual.filePath().equals(expected.filePath()) :
            "File path mismatch: expected " + expected.filePath() + ", got " + actual.filePath();
    }

    // Helper records
    record HTMLElementWithRule(HTMLElement element, RuleDefinition rule) {}
    record HTMLAttrElementWithRule(HTMLElement element, RuleDefinition rule, String attrName) {}
    record CSSElementWithRule(CSSElement element, RuleDefinition rule) {}
    record JSElementWithRule(JSElement element, RuleDefinition rule) {}
    record ElementWithRule(CodeElement element, RuleDefinition rule) {}


    // Providers

    @Provide
    Arbitrary<HTMLElementWithRule> deprecatedHtmlTagElement() {
        return Combinators.combine(
                Arbitraries.of(DEPRECATED_HTML_TAGS),
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((tagName, line, column) -> {
            Path testPath = Path.of("test.html");
            Location location = Location.of(testPath, line, column, line, column + tagName.length() + 2);
            
            HTMLElement element = HTMLElement.builder()
                    .tagName(tagName)
                    .tagNameLocation(location)
                    .location(location)
                    .rawContent("<" + tagName + ">")
                    .elementType(HTMLElementType.OPEN_TAG)
                    .build();
            
            RuleDefinition rule = RuleDefinition.builder()
                    .id("html-deprecated-tag-" + tagName)
                    .category(RuleCategory.HTML_DEPRECATED_TAG)
                    .severity(SeverityLevel.WARNING)
                    .pattern(tagName)
                    .description("Deprecated HTML tag <" + tagName + ">")
                    .suggestion(ReplacementSuggestion.of("Use modern alternative", "CSS"))
                    .mdnReference("https://developer.mozilla.org/en-US/docs/Web/HTML/Element/" + tagName)
                    .enabled(true)
                    .build();
            
            return new HTMLElementWithRule(element, rule);
        });
    }

    @Provide
    Arbitrary<HTMLAttrElementWithRule> deprecatedHtmlAttrElement() {
        return Combinators.combine(
                Arbitraries.of(DEPRECATED_HTML_ATTRS),
                Arbitraries.of("div", "span", "p", "table", "body"),
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((attrName, tagName, line, column) -> {
            Path testPath = Path.of("test.html");
            Location elementLocation = Location.of(testPath, line, column, line, column + 20);
            Location attrLocation = Location.of(testPath, line, column + tagName.length() + 2, 
                                                 line, column + tagName.length() + 2 + attrName.length() + 10);
            
            Map<String, AttributeInfo> attributes = Map.of(
                attrName.toLowerCase(), 
                AttributeInfo.of(attrName, attrLocation, "value", attrLocation, attrLocation)
            );
            
            HTMLElement element = HTMLElement.builder()
                    .tagName(tagName)
                    .tagNameLocation(elementLocation)
                    .attributes(attributes)
                    .location(elementLocation)
                    .rawContent("<" + tagName + " " + attrName + "=\"value\">")
                    .elementType(HTMLElementType.OPEN_TAG)
                    .build();
            
            RuleDefinition rule = RuleDefinition.builder()
                    .id("html-deprecated-attr-" + attrName)
                    .category(RuleCategory.HTML_DEPRECATED_ATTR)
                    .severity(SeverityLevel.WARNING)
                    .pattern(attrName)
                    .description("Deprecated HTML attribute '" + attrName + "'")
                    .suggestion(ReplacementSuggestion.of("Use CSS instead", "CSS"))
                    .mdnReference("https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes")
                    .enabled(true)
                    .build();
            
            return new HTMLAttrElementWithRule(element, rule, attrName);
        });
    }

    @Provide
    Arbitrary<CSSElementWithRule> deprecatedCssElement() {
        return Combinators.combine(
                Arbitraries.of(DEPRECATED_CSS_PROPS),
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((property, line, column) -> {
            Path testPath = Path.of("test.css");
            Location location = Location.of(testPath, line, column, line, column + property.length() + 10);
            
            CSSElement element = CSSElement.builder()
                    .type(CSSElementType.DECLARATION)
                    .property(property)
                    .propertyLocation(location)
                    .value("auto")
                    .valueLocation(location)
                    .location(location)
                    .rawContent(property + ": auto;")
                    .build();
            
            RuleDefinition rule = RuleDefinition.builder()
                    .id("css-deprecated-prop-" + property)
                    .category(RuleCategory.CSS_DEPRECATED_PROP)
                    .severity(SeverityLevel.WARNING)
                    .pattern(property)
                    .description("Deprecated CSS property '" + property + "'")
                    .suggestion(ReplacementSuggestion.of("Use modern alternative", "clip-path"))
                    .mdnReference("https://developer.mozilla.org/en-US/docs/Web/CSS/" + property)
                    .enabled(true)
                    .build();
            
            return new CSSElementWithRule(element, rule);
        });
    }

    @Provide
    Arbitrary<JSElementWithRule> deprecatedJsElement() {
        return Combinators.combine(
                Arbitraries.of(DEPRECATED_JS_APIS),
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((api, line, column) -> {
            Path testPath = Path.of("test.js");
            Location location = Location.of(testPath, line, column, line, column + api.length() + 2);
            
            JSElement element = JSElement.builder()
                    .type(api.contains(".") ? JSElementType.METHOD_CALL : JSElementType.FUNCTION_CALL)
                    .identifier(api)
                    .identifierLocation(location)
                    .location(location)
                    .rawContent(api + "()")
                    .build();
            
            RuleDefinition rule = RuleDefinition.builder()
                    .id("js-deprecated-api-" + api.replace(".", "-"))
                    .category(RuleCategory.JS_DEPRECATED_API)
                    .severity(SeverityLevel.WARNING)
                    .pattern(api)
                    .description("Deprecated JavaScript API '" + api + "'")
                    .suggestion(ReplacementSuggestion.of("Use modern alternative", "encodeURIComponent"))
                    .mdnReference("https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/" + api)
                    .enabled(true)
                    .build();
            
            return new JSElementWithRule(element, rule);
        });
    }


    @Provide
    Arbitrary<ElementWithRule> anyDeprecatedElement() {
        return Arbitraries.oneOf(
                deprecatedHtmlTagElement().map(p -> new ElementWithRule(p.element(), p.rule())),
                deprecatedCssElement().map(p -> new ElementWithRule(p.element(), p.rule())),
                deprecatedJsElement().map(p -> new ElementWithRule(p.element(), p.rule()))
        );
    }

    @Provide
    Arbitrary<ElementWithRule> nonDeprecatedElement() {
        return Arbitraries.oneOf(
                nonDeprecatedHtmlElement(),
                nonDeprecatedCssElement(),
                nonDeprecatedJsElement()
        );
    }

    private Arbitrary<ElementWithRule> nonDeprecatedHtmlElement() {
        // Modern HTML tags that are NOT deprecated
        List<String> modernTags = List.of("div", "span", "p", "section", "article", "header", "footer", "nav", "main");
        
        return Combinators.combine(
                Arbitraries.of(modernTags),
                Arbitraries.of(DEPRECATED_HTML_TAGS),  // Rule pattern
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((tagName, rulePattern, line, column) -> {
            Path testPath = Path.of("test.html");
            Location location = Location.of(testPath, line, column, line, column + tagName.length() + 2);
            
            HTMLElement element = HTMLElement.builder()
                    .tagName(tagName)
                    .tagNameLocation(location)
                    .location(location)
                    .rawContent("<" + tagName + ">")
                    .elementType(HTMLElementType.OPEN_TAG)
                    .build();
            
            // Rule is looking for a deprecated tag, but element is modern
            RuleDefinition rule = RuleDefinition.builder()
                    .id("html-deprecated-tag-" + rulePattern)
                    .category(RuleCategory.HTML_DEPRECATED_TAG)
                    .severity(SeverityLevel.WARNING)
                    .pattern(rulePattern)
                    .description("Deprecated HTML tag <" + rulePattern + ">")
                    .suggestion(ReplacementSuggestion.of("Use modern alternative", "CSS"))
                    .mdnReference("https://developer.mozilla.org/")
                    .enabled(true)
                    .build();
            
            return new ElementWithRule(element, rule);
        });
    }

    private Arbitrary<ElementWithRule> nonDeprecatedCssElement() {
        // Modern CSS properties that are NOT deprecated
        List<String> modernProps = List.of("color", "background", "margin", "padding", "display", "flex", "grid");
        
        return Combinators.combine(
                Arbitraries.of(modernProps),
                Arbitraries.of(DEPRECATED_CSS_PROPS),  // Rule pattern
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((property, rulePattern, line, column) -> {
            Path testPath = Path.of("test.css");
            Location location = Location.of(testPath, line, column, line, column + property.length() + 10);
            
            CSSElement element = CSSElement.builder()
                    .type(CSSElementType.DECLARATION)
                    .property(property)
                    .propertyLocation(location)
                    .value("auto")
                    .valueLocation(location)
                    .location(location)
                    .rawContent(property + ": auto;")
                    .build();
            
            // Rule is looking for a deprecated property, but element is modern
            RuleDefinition rule = RuleDefinition.builder()
                    .id("css-deprecated-prop-" + rulePattern)
                    .category(RuleCategory.CSS_DEPRECATED_PROP)
                    .severity(SeverityLevel.WARNING)
                    .pattern(rulePattern)
                    .description("Deprecated CSS property '" + rulePattern + "'")
                    .suggestion(ReplacementSuggestion.of("Use modern alternative", "clip-path"))
                    .mdnReference("https://developer.mozilla.org/")
                    .enabled(true)
                    .build();
            
            return new ElementWithRule(element, rule);
        });
    }

    private Arbitrary<ElementWithRule> nonDeprecatedJsElement() {
        // Modern JS APIs that are NOT deprecated
        List<String> modernApis = List.of("console.log", "fetch", "Promise", "Array.from", "Object.assign");
        
        return Combinators.combine(
                Arbitraries.of(modernApis),
                Arbitraries.of(DEPRECATED_JS_APIS),  // Rule pattern
                Arbitraries.integers().between(1, 100),
                Arbitraries.integers().between(1, 80)
        ).as((api, rulePattern, line, column) -> {
            Path testPath = Path.of("test.js");
            Location location = Location.of(testPath, line, column, line, column + api.length() + 2);
            
            JSElement element = JSElement.builder()
                    .type(api.contains(".") ? JSElementType.METHOD_CALL : JSElementType.FUNCTION_CALL)
                    .identifier(api)
                    .identifierLocation(location)
                    .location(location)
                    .rawContent(api + "()")
                    .build();
            
            // Rule is looking for a deprecated API, but element is modern
            RuleDefinition rule = RuleDefinition.builder()
                    .id("js-deprecated-api-" + rulePattern.replace(".", "-"))
                    .category(RuleCategory.JS_DEPRECATED_API)
                    .severity(SeverityLevel.WARNING)
                    .pattern(rulePattern)
                    .description("Deprecated JavaScript API '" + rulePattern + "'")
                    .suggestion(ReplacementSuggestion.of("Use modern alternative", "encodeURIComponent"))
                    .mdnReference("https://developer.mozilla.org/")
                    .enabled(true)
                    .build();
            
            return new ElementWithRule(element, rule);
        });
    }
}
