package io.github.sainm.weblegacyscan.parser.css;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.ParseResult;
import net.jqwik.api.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Property-based tests for CSS Parser Declaration Location Tracking.
 * 
 * Property 23: CSS Declaration Location Tracking
 * For any CSSElement of type DECLARATION, the element SHALL have valid propertyLocation 
 * and valueLocation in addition to the full declaration location.
 * 
 * Validates: Requirements 13.3
 */
class CSSDeclarationLocationPropertyTest {

    private static final Path TEST_FILE = Path.of("test.css");
    private final PhCSSParser parser = new PhCSSParser();

    /**
     * Property 23: CSS Declaration Location Tracking - Declarations have valid property locations
     * 
     * For any CSSElement of type DECLARATION parsed by PhCSSParser,
     * the element SHALL have a valid propertyLocation with startLine >= 1 and startColumn >= 1.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void declarationsMustHaveValidPropertyLocation(
            @ForAll("cssWithDeclarations") String cssContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.CSS, StandardCharsets.UTF_8, cssContent.length());
        ParseResult result = parser.parseContent(cssContent, sourceFile);
        
        List<CSSElement> declarations = collectDeclarations(result.elements());
        
        for (CSSElement decl : declarations) {
            assert decl.propertyLocation() != null : 
                "Declaration propertyLocation must not be null for property: " + decl.property();
            assert decl.propertyLocation().startLine() >= 1 : 
                "Declaration propertyLocation startLine must be >= 1, got: " + decl.propertyLocation().startLine();
            assert decl.propertyLocation().startColumn() >= 1 : 
                "Declaration propertyLocation startColumn must be >= 1, got: " + decl.propertyLocation().startColumn();
        }
    }

    /**
     * Property 23: CSS Declaration Location Tracking - Declarations have valid value locations
     * 
     * For any CSSElement of type DECLARATION parsed by PhCSSParser,
     * the element SHALL have a valid valueLocation with startLine >= 1 and startColumn >= 1.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void declarationsMustHaveValidValueLocation(
            @ForAll("cssWithDeclarations") String cssContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.CSS, StandardCharsets.UTF_8, cssContent.length());
        ParseResult result = parser.parseContent(cssContent, sourceFile);
        
        List<CSSElement> declarations = collectDeclarations(result.elements());
        
        for (CSSElement decl : declarations) {
            assert decl.valueLocation() != null : 
                "Declaration valueLocation must not be null for property: " + decl.property();
            assert decl.valueLocation().startLine() >= 1 : 
                "Declaration valueLocation startLine must be >= 1, got: " + decl.valueLocation().startLine();
            assert decl.valueLocation().startColumn() >= 1 : 
                "Declaration valueLocation startColumn must be >= 1, got: " + decl.valueLocation().startColumn();
        }
    }


    /**
     * Property 23: CSS Declaration Location Tracking - Declarations have valid full locations
     * 
     * For any CSSElement of type DECLARATION parsed by PhCSSParser,
     * the element SHALL have a valid location (full declaration location).
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void declarationsMustHaveValidFullLocation(
            @ForAll("cssWithDeclarations") String cssContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.CSS, StandardCharsets.UTF_8, cssContent.length());
        ParseResult result = parser.parseContent(cssContent, sourceFile);
        
        List<CSSElement> declarations = collectDeclarations(result.elements());
        
        for (CSSElement decl : declarations) {
            assert decl.location() != null : 
                "Declaration location must not be null for property: " + decl.property();
            assert decl.location().startLine() >= 1 : 
                "Declaration location startLine must be >= 1, got: " + decl.location().startLine();
            assert decl.location().startColumn() >= 1 : 
                "Declaration location startColumn must be >= 1, got: " + decl.location().startColumn();
            assert decl.location().endLine() >= decl.location().startLine() :
                "Declaration location endLine must be >= startLine";
        }
    }

    /**
     * Property 23: CSS Declaration Location Tracking - Property location offsets are valid
     * 
     * For any CSSElement of type DECLARATION, the propertyLocation offsets SHALL be
     * within the bounds of the CSS content.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void declarationPropertyLocationOffsetsAreValid(
            @ForAll("cssWithDeclarations") String cssContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.CSS, StandardCharsets.UTF_8, cssContent.length());
        ParseResult result = parser.parseContent(cssContent, sourceFile);
        
        List<CSSElement> declarations = collectDeclarations(result.elements());
        
        for (CSSElement decl : declarations) {
            Location propLoc = decl.propertyLocation();
            if (propLoc.startOffset() >= 0 && propLoc.endOffset() >= 0) {
                assert propLoc.endOffset() >= propLoc.startOffset() :
                    "propertyLocation endOffset must be >= startOffset";
                assert propLoc.startOffset() < cssContent.length() :
                    "propertyLocation startOffset must be within content bounds";
            }
        }
    }

    /**
     * Property 23: CSS Declaration Location Tracking - Value location offsets are valid
     * 
     * For any CSSElement of type DECLARATION, the valueLocation offsets SHALL be
     * within the bounds of the CSS content.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void declarationValueLocationOffsetsAreValid(
            @ForAll("cssWithDeclarations") String cssContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.CSS, StandardCharsets.UTF_8, cssContent.length());
        ParseResult result = parser.parseContent(cssContent, sourceFile);
        
        List<CSSElement> declarations = collectDeclarations(result.elements());
        
        for (CSSElement decl : declarations) {
            Location valLoc = decl.valueLocation();
            if (valLoc.startOffset() >= 0 && valLoc.endOffset() >= 0) {
                assert valLoc.endOffset() >= valLoc.startOffset() :
                    "valueLocation endOffset must be >= startOffset";
                assert valLoc.startOffset() < cssContent.length() :
                    "valueLocation startOffset must be within content bounds";
            }
        }
    }

    // ========== Helper Methods ==========

    private List<CSSElement> collectDeclarations(List<CodeElement> elements) {
        return elements.stream()
            .filter(e -> e instanceof CSSElement)
            .map(e -> (CSSElement) e)
            .flatMap(css -> collectDeclarationsRecursive(css).stream())
            .toList();
    }

    private List<CSSElement> collectDeclarationsRecursive(CSSElement element) {
        java.util.ArrayList<CSSElement> result = new java.util.ArrayList<>();
        
        if (element.type() == CSSElementType.DECLARATION) {
            result.add(element);
        }
        
        if (element.children() != null) {
            for (CSSElement child : element.children()) {
                result.addAll(collectDeclarationsRecursive(child));
            }
        }
        
        return result;
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> cssWithDeclarations() {
        return Combinators.combine(
            selector(),
            declarationList()
        ).as((sel, decls) -> sel + " { " + decls + " }");
    }

    private Arbitrary<String> selector() {
        return Arbitraries.of(
            ".class", "#id", "div", "span", "p", "a", "body", "html",
            ".container", "#main", "header", "footer", "nav", "section"
        );
    }

    private Arbitrary<String> cssProperty() {
        return Arbitraries.of(
            "color", "background", "margin", "padding", "font-size",
            "width", "height", "display", "position", "border",
            "text-align", "font-weight", "line-height", "opacity"
        );
    }

    private Arbitrary<String> cssValue() {
        return Arbitraries.of(
            "red", "blue", "green", "#fff", "#000", "10px", "20px",
            "100%", "auto", "none", "block", "flex", "relative",
            "1em", "2rem", "bold", "normal", "center", "left"
        );
    }

    private Arbitrary<String> singleDeclaration() {
        return Combinators.combine(
            cssProperty(),
            cssValue()
        ).as((prop, val) -> prop + ": " + val + ";");
    }

    private Arbitrary<String> declarationList() {
        return singleDeclaration()
            .list()
            .ofMinSize(1)
            .ofMaxSize(5)
            .map(decls -> String.join(" ", decls));
    }
}
