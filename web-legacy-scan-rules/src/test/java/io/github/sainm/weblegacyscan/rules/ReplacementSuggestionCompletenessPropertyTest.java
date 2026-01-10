package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Property-based tests for Replacement Suggestion Completeness.
 * 
 * Property 2: Replacement Suggestion Completeness
 * For any Issue detected by the Scanner, the Issue SHALL contain a non-null 
 * ReplacementSuggestion with a valid MDN_Reference URL.
 * 
 * Validates: Requirements 1.2, 2.2, 3.2
 */
class ReplacementSuggestionCompletenessPropertyTest {

    // MDN URL pattern for validation
    private static final Pattern MDN_URL_PATTERN = Pattern.compile(
        "^https://developer\\.mozilla\\.org/[a-zA-Z-]+/docs/.*$"
    );

    // Known deprecated HTML tags (Requirement 1.2)
    private static final List<String> DEPRECATED_HTML_TAGS = List.of(
        "font", "center", "marquee", "blink", "basefont", "big", 
        "strike", "tt", "frame", "frameset", "noframes", "applet", "acronym", "dir"
    );

    // Known deprecated HTML attributes (Requirement 1.2)
    private static final List<String> DEPRECATED_HTML_ATTRS = List.of(
        "align", "bgcolor", "border", "language"
    );

    // Known deprecated CSS properties (Requirement 2.2)
    private static final List<String> DEPRECATED_CSS_PROPS = List.of(
        "clip", "zoom"
    );

    // Known deprecated JS APIs (Requirement 3.2)
    private static final List<String> DEPRECATED_JS_APIS = List.of(
        "escape", "unescape", "document.write", "document.writeln", "eval"
    );

    /**
     * Property 2: HTML Deprecated Tag Issues Have Complete Replacement Suggestions
     * 
     * For any Issue detected for a deprecated HTML tag, the Issue SHALL contain
     * a non-null ReplacementSuggestion with description and modernAlternative.
     * 
     * Feature: web-legacy-scan, Property 2: Replacement Suggestion Completeness
     * Validates: Requirements 1.2
     */
    @Property(tries = 100)
    void htmlDeprecatedTagIssuesHaveCompleteSuggestions(
            @ForAll("deprecatedHtmlTagElement") HTMLElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated HTML tag <" + pair.element().tagName() + "> should be detected";
        
        Issue issue = result.get();
        
        // Property 2: Verify ReplacementSuggestion is non-null
        assert issue.suggestion() != null :
            "Issue for deprecated HTML tag <" + pair.element().tagName() + "> must have a non-null ReplacementSuggestion";
        
        // Verify suggestion has required fields
        ReplacementSuggestion suggestion = issue.suggestion();
        assert suggestion.description() != null && !suggestion.description().isBlank() :
            "ReplacementSuggestion.description must be non-null and non-blank";
        assert suggestion.modernAlternative() != null && !suggestion.modernAlternative().isBlank() :
            "ReplacementSuggestion.modernAlternative must be non-null and non-blank";
        
        // Verify MDN reference is valid
        assert issue.mdnReference() != null && !issue.mdnReference().isBlank() :
            "Issue must have a non-null and non-blank MDN reference";
        assert MDN_URL_PATTERN.matcher(issue.mdnReference()).matches() :
            "MDN reference URL must be valid: " + issue.mdnReference();
    }

    /**
     * Property 2: HTML Deprecated Attribute Issues Have Complete Replacement Suggestions
     * 
     * For any Issue detected for a deprecated HTML attribute, the Issue SHALL contain
     * a non-null ReplacementSuggestion with description and modernAlternative.
     * 
     * Feature: web-legacy-scan, Property 2: Replacement Suggestion Completeness
     * Validates: Requirements 1.2
     */
    @Property(tries = 100)
    void htmlDeprecatedAttrIssuesHaveCompleteSuggestions(
            @ForAll("deprecatedHtmlAttrElement") HTMLAttrElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated HTML attribute '" + pair.attrName() + "' should be detected";
        
        Issue issue = result.get();
        
        // Property 2: Verify ReplacementSuggestion is non-null
        assert issue.suggestion() != null :
            "Issue for deprecated HTML attribute '" + pair.attrName() + "' must have a non-null ReplacementSuggestion";
        
        // Verify suggestion has required fields
        ReplacementSuggestion suggestion = issue.suggestion();
        assert suggestion.description() != null && !suggestion.description().isBlank() :
            "ReplacementSuggestion.description must be non-null and non-blank";
        assert suggestion.modernAlternative() != null && !suggestion.modernAlternative().isBlank() :
            "ReplacementSuggestion.modernAlternative must be non-null and non-blank";
        
        // Verify MDN reference is valid
        assert issue.mdnReference() != null && !issue.mdnReference().isBlank() :
            "Issue must have a non-null and non-blank MDN reference";
        assert MDN_URL_PATTERN.matcher(issue.mdnReference()).matches() :
            "MDN reference URL must be valid: " + issue.mdnReference();
    }

    /**
     * Property 2: CSS Deprecated Property Issues Have Complete Replacement Suggestions
     * 
     * For any Issue detected for a deprecated CSS property, the Issue SHALL contain
     * a non-null ReplacementSuggestion with description and modernAlternative.
     * 
     * Feature: web-legacy-scan, Property 2: Replacement Suggestion Completeness
     * Validates: Requirements 2.2
     */
    @Property(tries = 100)
    void cssDeprecatedPropIssuesHaveCompleteSuggestions(
            @ForAll("deprecatedCssElement") CSSElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated CSS property '" + pair.element().property() + "' should be detected";
        
        Issue issue = result.get();
        
        // Property 2: Verify ReplacementSuggestion is non-null
        assert issue.suggestion() != null :
            "Issue for deprecated CSS property '" + pair.element().property() + "' must have a non-null ReplacementSuggestion";
        
        // Verify suggestion has required fields
        ReplacementSuggestion suggestion = issue.suggestion();
        assert suggestion.description() != null && !suggestion.description().isBlank() :
            "ReplacementSuggestion.description must be non-null and non-blank";
        assert suggestion.modernAlternative() != null && !suggestion.modernAlternative().isBlank() :
            "ReplacementSuggestion.modernAlternative must be non-null and non-blank";
        
        // Verify MDN reference is valid
        assert issue.mdnReference() != null && !issue.mdnReference().isBlank() :
            "Issue must have a non-null and non-blank MDN reference";
        assert MDN_URL_PATTERN.matcher(issue.mdnReference()).matches() :
            "MDN reference URL must be valid: " + issue.mdnReference();
    }

    /**
     * Property 2: JS Deprecated API Issues Have Complete Replacement Suggestions
     * 
     * For any Issue detected for a deprecated JavaScript API, the Issue SHALL contain
     * a non-null ReplacementSuggestion with description and modernAlternative.
     * 
     * Feature: web-legacy-scan, Property 2: Replacement Suggestion Completeness
     * Validates: Requirements 3.2
     */
    @Property(tries = 100)
    void jsDeprecatedApiIssuesHaveCompleteSuggestions(
            @ForAll("deprecatedJsElement") JSElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        // Verify issue is detected
        assert result.isPresent() : 
            "Deprecated JS API '" + pair.element().identifier() + "' should be detected";
        
        Issue issue = result.get();
        
        // Property 2: Verify ReplacementSuggestion is non-null
        assert issue.suggestion() != null :
            "Issue for deprecated JS API '" + pair.element().identifier() + "' must have a non-null ReplacementSuggestion";
        
        // Verify suggestion has required fields
        ReplacementSuggestion suggestion = issue.suggestion();
        assert suggestion.description() != null && !suggestion.description().isBlank() :
            "ReplacementSuggestion.description must be non-null and non-blank";
        assert suggestion.modernAlternative() != null && !suggestion.modernAlternative().isBlank() :
            "ReplacementSuggestion.modernAlternative must be non-null and non-blank";
        
        // Verify MDN reference is valid
        assert issue.mdnReference() != null && !issue.mdnReference().isBlank() :
            "Issue must have a non-null and non-blank MDN reference";
        assert MDN_URL_PATTERN.matcher(issue.mdnReference()).matches() :
            "MDN reference URL must be valid: " + issue.mdnReference();
    }

    /**
     * Property 2: All Default Rules Have Complete Replacement Suggestions
     * 
     * For any default rule created by RuleFactory, when it detects an issue,
     * the Issue SHALL contain a non-null ReplacementSuggestion with valid MDN reference.
     * 
     * Feature: web-legacy-scan, Property 2: Replacement Suggestion Completeness
     * Validates: Requirements 1.2, 2.2, 3.2
     */
    @Property(tries = 100)
    void defaultRulesProduceIssuesWithCompleteSuggestions(
            @ForAll("anyDeprecatedElementWithDefaultRule") ElementWithDefaultRule pair
    ) {
        Optional<Issue> result = pair.rule().evaluate(pair.element());
        
        if (result.isPresent()) {
            Issue issue = result.get();
            
            // Property 2: Verify ReplacementSuggestion is non-null
            assert issue.suggestion() != null :
                "Issue from default rule must have a non-null ReplacementSuggestion";
            
            // Verify suggestion has required fields
            ReplacementSuggestion suggestion = issue.suggestion();
            assert suggestion.description() != null && !suggestion.description().isBlank() :
                "ReplacementSuggestion.description must be non-null and non-blank";
            assert suggestion.modernAlternative() != null && !suggestion.modernAlternative().isBlank() :
                "ReplacementSuggestion.modernAlternative must be non-null and non-blank";
            
            // Verify MDN reference is valid
            assert issue.mdnReference() != null && !issue.mdnReference().isBlank() :
                "Issue must have a non-null and non-blank MDN reference";
            assert MDN_URL_PATTERN.matcher(issue.mdnReference()).matches() :
                "MDN reference URL must be valid: " + issue.mdnReference();
        }
    }

    /**
     * Property 2: Suggestion Description Is Meaningful
     * 
     * For any detected Issue, the ReplacementSuggestion.description SHALL contain
     * meaningful guidance (at least 10 characters).
     * 
     * Feature: web-legacy-scan, Property 2: Replacement Suggestion Completeness
     * Validates: Requirements 1.2, 2.2, 3.2
     */
    @Property(tries = 100)
    void suggestionDescriptionIsMeaningful(
            @ForAll("anyDeprecatedElement") ElementWithRule pair
    ) {
        RuleFactory factory = new RuleFactory();
        Rule rule = factory.createRule(pair.rule());
        
        Optional<Issue> result = rule.evaluate(pair.element());
        
        if (result.isPresent()) {
            Issue issue = result.get();
            
            if (issue.suggestion() != null) {
                String description = issue.suggestion().description();
                assert description.length() >= 10 :
                    "Suggestion description should be meaningful (at least 10 chars), got: '" + description + "'";
            }
        }
    }

    // Helper records
    record HTMLElementWithRule(HTMLElement element, RuleDefinition rule) {}
    record HTMLAttrElementWithRule(HTMLElement element, RuleDefinition rule, String attrName) {}
    record CSSElementWithRule(CSSElement element, RuleDefinition rule) {}
    record JSElementWithRule(JSElement element, RuleDefinition rule) {}
    record ElementWithRule(CodeElement element, RuleDefinition rule) {}
    record ElementWithDefaultRule(CodeElement element, Rule rule) {}

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
                    .suggestion(ReplacementSuggestion.of("Use CSS styling instead of deprecated <" + tagName + "> tag", "CSS"))
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
                    .suggestion(ReplacementSuggestion.of("Use CSS instead of deprecated '" + attrName + "' attribute", "CSS"))
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
            
            String alternative = property.equals("clip") ? "clip-path" : "transform: scale()";
            RuleDefinition rule = RuleDefinition.builder()
                    .id("css-deprecated-prop-" + property)
                    .category(RuleCategory.CSS_DEPRECATED_PROP)
                    .severity(SeverityLevel.WARNING)
                    .pattern(property)
                    .description("Deprecated CSS property '" + property + "'")
                    .suggestion(ReplacementSuggestion.of("Use " + alternative + " instead of deprecated '" + property + "'", alternative))
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
            
            String alternative = switch (api) {
                case "escape" -> "encodeURIComponent()";
                case "unescape" -> "decodeURIComponent()";
                case "document.write", "document.writeln" -> "DOM manipulation methods";
                case "eval" -> "safer alternatives like JSON.parse()";
                default -> "modern alternative";
            };
            
            RuleDefinition rule = RuleDefinition.builder()
                    .id("js-deprecated-api-" + api.replace(".", "-"))
                    .category(RuleCategory.JS_DEPRECATED_API)
                    .severity(SeverityLevel.WARNING)
                    .pattern(api)
                    .description("Deprecated JavaScript API '" + api + "'")
                    .suggestion(ReplacementSuggestion.of("Use " + alternative + " instead of deprecated '" + api + "'", alternative))
                    .mdnReference("https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/" + api.replace(".", "/"))
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
    Arbitrary<ElementWithDefaultRule> anyDeprecatedElementWithDefaultRule() {
        RuleFactory factory = new RuleFactory();
        List<Rule> defaultRules = factory.createDefaultRules();
        
        return Arbitraries.oneOf(
                // HTML deprecated tags with matching default rules
                Arbitraries.of(DEPRECATED_HTML_TAGS).flatMap(tagName -> {
                    Path testPath = Path.of("test.html");
                    Location location = Location.of(testPath, 1, 1, 1, tagName.length() + 2);
                    
                    HTMLElement element = HTMLElement.builder()
                            .tagName(tagName)
                            .tagNameLocation(location)
                            .location(location)
                            .rawContent("<" + tagName + ">")
                            .elementType(HTMLElementType.OPEN_TAG)
                            .build();
                    
                    // Find matching default rule
                    Rule matchingRule = defaultRules.stream()
                            .filter(r -> r.getId().equals("html-deprecated-tag-" + tagName))
                            .findFirst()
                            .orElse(defaultRules.get(0));
                    
                    return Arbitraries.just(new ElementWithDefaultRule(element, matchingRule));
                }),
                // CSS deprecated properties with matching default rules
                Arbitraries.of(DEPRECATED_CSS_PROPS).flatMap(property -> {
                    Path testPath = Path.of("test.css");
                    Location location = Location.of(testPath, 1, 1, 1, property.length() + 10);
                    
                    CSSElement element = CSSElement.builder()
                            .type(CSSElementType.DECLARATION)
                            .property(property)
                            .propertyLocation(location)
                            .value("auto")
                            .valueLocation(location)
                            .location(location)
                            .rawContent(property + ": auto;")
                            .build();
                    
                    // Find matching default rule
                    Rule matchingRule = defaultRules.stream()
                            .filter(r -> r.getId().equals("css-deprecated-prop-" + property))
                            .findFirst()
                            .orElse(defaultRules.get(0));
                    
                    return Arbitraries.just(new ElementWithDefaultRule(element, matchingRule));
                }),
                // JS deprecated APIs with matching default rules
                Arbitraries.of(DEPRECATED_JS_APIS).flatMap(api -> {
                    Path testPath = Path.of("test.js");
                    Location location = Location.of(testPath, 1, 1, 1, api.length() + 2);
                    
                    JSElement element = JSElement.builder()
                            .type(api.contains(".") ? JSElementType.METHOD_CALL : JSElementType.FUNCTION_CALL)
                            .identifier(api)
                            .identifierLocation(location)
                            .location(location)
                            .rawContent(api + "()")
                            .build();
                    
                    // Find matching default rule
                    Rule matchingRule = defaultRules.stream()
                            .filter(r -> r.getId().equals("js-deprecated-api-" + api.replace(".", "-")))
                            .findFirst()
                            .orElse(defaultRules.get(0));
                    
                    return Arbitraries.just(new ElementWithDefaultRule(element, matchingRule));
                })
        );
    }
}
