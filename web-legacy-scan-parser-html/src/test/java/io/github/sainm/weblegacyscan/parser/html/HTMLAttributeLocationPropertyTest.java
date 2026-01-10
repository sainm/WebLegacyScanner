package io.github.sainm.weblegacyscan.parser.html;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.ParseResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Property-based tests for HTML Parser Attribute Location Tracking.
 * 
 * Property 22: HTML Attribute Location Tracking
 * For any HTMLElement with attributes, each AttributeInfo SHALL have valid nameLocation,
 * and if the attribute has a value, a valid valueLocation.
 * 
 * Validates: Requirements 13.2
 */
class HTMLAttributeLocationPropertyTest {

    private static final Path TEST_FILE = Path.of("test.html");
    private final JerichoHTMLParser parser = new JerichoHTMLParser();

    /**
     * Property 22: HTML Attribute Location Tracking - Parser produces valid name locations
     * 
     * For any HTML element with attributes parsed by JerichoHTMLParser,
     * each attribute SHALL have a valid nameLocation with startLine >= 1 and startColumn >= 1.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void parsedAttributesMustHaveValidNameLocation(
            @ForAll("htmlWithAttributes") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof HTMLElement htmlElement) {
                for (Map.Entry<String, AttributeInfo> entry : htmlElement.attributes().entrySet()) {
                    AttributeInfo attr = entry.getValue();
                    
                    // nameLocation must be valid
                    assert attr.nameLocation() != null : 
                        "Attribute '" + attr.name() + "' nameLocation must not be null";
                    assert attr.nameLocation().startLine() >= 1 : 
                        "Attribute '" + attr.name() + "' nameLocation startLine must be >= 1, got: " + attr.nameLocation().startLine();
                    assert attr.nameLocation().startColumn() >= 1 : 
                        "Attribute '" + attr.name() + "' nameLocation startColumn must be >= 1, got: " + attr.nameLocation().startColumn();
                }
            }
        }
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Parser produces valid value locations
     * 
     * For any HTML element with attributes that have values, parsed by JerichoHTMLParser,
     * each attribute with a value SHALL have a valid valueLocation.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void parsedAttributesWithValueMustHaveValidValueLocation(
            @ForAll("htmlWithValuedAttributes") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof HTMLElement htmlElement) {
                for (Map.Entry<String, AttributeInfo> entry : htmlElement.attributes().entrySet()) {
                    AttributeInfo attr = entry.getValue();
                    
                    if (attr.hasValue()) {
                        assert attr.valueLocation() != null : 
                            "Attribute '" + attr.name() + "' with value must have non-null valueLocation";
                        assert attr.valueLocation().startLine() >= 1 : 
                            "Attribute '" + attr.name() + "' valueLocation startLine must be >= 1";
                        assert attr.valueLocation().startColumn() >= 1 : 
                            "Attribute '" + attr.name() + "' valueLocation startColumn must be >= 1";
                    }
                }
            }
        }
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Parser produces valid full locations
     * 
     * For any HTML element with attributes parsed by JerichoHTMLParser,
     * each attribute SHALL have a valid fullLocation.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void parsedAttributesMustHaveValidFullLocation(
            @ForAll("htmlWithAttributes") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof HTMLElement htmlElement) {
                for (Map.Entry<String, AttributeInfo> entry : htmlElement.attributes().entrySet()) {
                    AttributeInfo attr = entry.getValue();
                    
                    // fullLocation must be valid
                    assert attr.fullLocation() != null : 
                        "Attribute '" + attr.name() + "' fullLocation must not be null";
                    assert attr.fullLocation().startLine() >= 1 : 
                        "Attribute '" + attr.name() + "' fullLocation startLine must be >= 1";
                    assert attr.fullLocation().startColumn() >= 1 : 
                        "Attribute '" + attr.name() + "' fullLocation startColumn must be >= 1";
                }
            }
        }
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Name location points to correct content
     * 
     * For any parsed attribute, the nameLocation's sourceSnippet SHALL match the attribute name.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void attributeNameLocationSnippetMatchesName(
            @ForAll("htmlWithAttributes") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof HTMLElement htmlElement) {
                for (Map.Entry<String, AttributeInfo> entry : htmlElement.attributes().entrySet()) {
                    AttributeInfo attr = entry.getValue();
                    
                    if (attr.nameLocation().sourceSnippet() != null) {
                        assert attr.nameLocation().sourceSnippet().equalsIgnoreCase(attr.name()) : 
                            "nameLocation snippet '" + attr.nameLocation().sourceSnippet() + 
                            "' should match attribute name '" + attr.name() + "'";
                    }
                }
            }
        }
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Location offsets are consistent
     * 
     * For any parsed attribute, the startOffset and endOffset SHALL be consistent
     * with the content length.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void attributeLocationOffsetsAreConsistent(
            @ForAll("htmlWithAttributes") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof HTMLElement htmlElement) {
                for (Map.Entry<String, AttributeInfo> entry : htmlElement.attributes().entrySet()) {
                    AttributeInfo attr = entry.getValue();
                    
                    // Check nameLocation offsets
                    if (attr.nameLocation().startOffset() >= 0 && attr.nameLocation().endOffset() >= 0) {
                        assert attr.nameLocation().endOffset() >= attr.nameLocation().startOffset() :
                            "nameLocation endOffset must be >= startOffset";
                        assert attr.nameLocation().startOffset() < htmlContent.length() :
                            "nameLocation startOffset must be within content bounds";
                    }
                    
                    // Check fullLocation offsets
                    if (attr.fullLocation().startOffset() >= 0 && attr.fullLocation().endOffset() >= 0) {
                        assert attr.fullLocation().endOffset() >= attr.fullLocation().startOffset() :
                            "fullLocation endOffset must be >= startOffset";
                        assert attr.fullLocation().startOffset() < htmlContent.length() :
                            "fullLocation startOffset must be within content bounds";
                    }
                }
            }
        }
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> htmlWithAttributes() {
        return Combinators.combine(
            tagName(),
            attributeList()
        ).as((tag, attrs) -> "<" + tag + " " + attrs + "></" + tag + ">");
    }

    @Provide
    Arbitrary<String> htmlWithValuedAttributes() {
        return Combinators.combine(
            tagName(),
            valuedAttributeList()
        ).as((tag, attrs) -> "<" + tag + " " + attrs + "></" + tag + ">");
    }

    private Arbitrary<String> tagName() {
        return Arbitraries.of("div", "span", "p", "a", "img", "input", "button", "form", "table", "tr", "td");
    }

    private Arbitrary<String> attributeName() {
        return Arbitraries.of("id", "class", "style", "href", "src", "alt", "title", "name", "value", "type", "data-id", "aria-label");
    }

    private Arbitrary<String> attributeValue() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(1)
            .ofMaxLength(20)
            .map(s -> s.replaceAll("[\"'<>]", ""));
    }

    private Arbitrary<String> singleAttribute() {
        return Combinators.combine(
            attributeName(),
            attributeValue()
        ).as((name, value) -> name + "=\"" + value + "\"");
    }

    private Arbitrary<String> attributeList() {
        return singleAttribute()
            .list()
            .ofMinSize(1)
            .ofMaxSize(5)
            .map(attrs -> String.join(" ", attrs));
    }

    private Arbitrary<String> valuedAttributeList() {
        return singleAttribute()
            .list()
            .ofMinSize(1)
            .ofMaxSize(5)
            .map(attrs -> String.join(" ", attrs));
    }
}
