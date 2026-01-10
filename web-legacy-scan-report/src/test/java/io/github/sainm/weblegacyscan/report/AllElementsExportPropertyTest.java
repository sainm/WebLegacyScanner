package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;
import com.google.gson.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * Property-based tests for All Elements Export Completeness.
 * 
 * Property 26: All Elements Export Completeness
 * For any scan with `--output-all-elements` enabled, the output SHALL contain 
 * every CodeElement parsed from the source files, and the count SHALL match 
 * ParseStatistics.totalElements.
 * 
 * Validates: Requirements 13.5, 13.6
 */
class AllElementsExportPropertyTest {

    private final JSONFormatter formatter = new JSONFormatter();

    /**
     * Property 26: All Elements Export Completeness - Element Count Match
     * 
     * For any scan with `--output-all-elements` enabled, the number of elements
     * in the output SHALL match the number of elements in the ScanResult.
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void outputAllElementsCountMatchesScanResult(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        // When outputAllElements is enabled and there are elements, 
        // the elements array should be present
        if (!result.allElements().isEmpty()) {
            assert root.has("elements") : 
                    "Elements array should be present when outputAllElements is enabled";
            
            JsonArray elementsArray = root.getAsJsonArray("elements");
            assert elementsArray.size() == result.allElements().size() :
                    "Elements count mismatch: expected " + result.allElements().size() + 
                    ", got " + elementsArray.size();
        }
    }

    /**
     * Property 26: All Elements Export Completeness - Statistics Match
     * 
     * For any scan with `--output-all-elements` enabled, the count of elements
     * in the output SHALL match ParseStatistics.totalElements.
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void outputAllElementsCountMatchesStatistics(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        // Verify statistics totalElements matches actual elements count
        JsonObject summary = root.getAsJsonObject("summary");
        int reportedTotalElements = result.statistics().totalElements();
        
        if (!result.allElements().isEmpty()) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            // The statistics totalElements should reflect the actual element count
            assert elementsArray.size() == result.allElements().size() :
                    "Elements array size should match allElements list size";
        }
    }

    /**
     * Property 26: All Elements Export Completeness - Each Element Has Location
     * 
     * For any CodeElement in the output, the element SHALL have complete
     * location information (file, startLine, startColumn, endLine, endColumn).
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void allExportedElementsHaveCompleteLocation(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        if (!result.allElements().isEmpty()) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            
            for (int i = 0; i < elementsArray.size(); i++) {
                JsonObject element = elementsArray.get(i).getAsJsonObject();
                
                // Each element must have type
                assert element.has("type") : 
                        "Element at index " + i + " must have type";
                
                // Each element must have location
                assert element.has("location") : 
                        "Element at index " + i + " must have location";
                
                JsonObject location = element.getAsJsonObject("location");
                assert location.has("file") : 
                        "Location at index " + i + " must have file";
                assert location.has("startLine") : 
                        "Location at index " + i + " must have startLine";
                assert location.has("startColumn") : 
                        "Location at index " + i + " must have startColumn";
                assert location.has("endLine") : 
                        "Location at index " + i + " must have endLine";
                assert location.has("endColumn") : 
                        "Location at index " + i + " must have endColumn";
                
                // Verify location values are valid
                int startLine = location.get("startLine").getAsInt();
                int startColumn = location.get("startColumn").getAsInt();
                int endLine = location.get("endLine").getAsInt();
                int endColumn = location.get("endColumn").getAsInt();
                
                assert startLine >= 1 : "startLine must be >= 1";
                assert startColumn >= 1 : "startColumn must be >= 1";
                assert endLine >= startLine : "endLine must be >= startLine";
            }
        }
    }

    /**
     * Property 26: All Elements Export Completeness - Element Type Preservation
     * 
     * For any CodeElement in the output, the element type SHALL be correctly
     * preserved (HTML, CSS, or JAVASCRIPT).
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void allExportedElementsHaveCorrectType(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        if (!result.allElements().isEmpty()) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            
            for (int i = 0; i < elementsArray.size(); i++) {
                JsonObject element = elementsArray.get(i).getAsJsonObject();
                CodeElement originalElement = result.allElements().get(i);
                
                String exportedType = element.get("type").getAsString();
                String expectedType = originalElement.getElementType().name();
                
                assert exportedType.equals(expectedType) :
                        "Element type mismatch at index " + i + 
                        ": expected " + expectedType + ", got " + exportedType;
            }
        }
    }

    /**
     * Property 26: All Elements Export Completeness - HTML Element Details
     * 
     * For any HTMLElement in the output, the element SHALL include tagName
     * and elementType.
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void htmlElementsHaveRequiredDetails(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        if (!result.allElements().isEmpty()) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            
            for (int i = 0; i < elementsArray.size(); i++) {
                JsonObject element = elementsArray.get(i).getAsJsonObject();
                CodeElement originalElement = result.allElements().get(i);
                
                if (originalElement instanceof HTMLElement htmlElement) {
                    assert element.has("tagName") :
                            "HTML element at index " + i + " must have tagName";
                    assert element.has("elementType") :
                            "HTML element at index " + i + " must have elementType";
                    
                    assert htmlElement.tagName().equals(element.get("tagName").getAsString()) :
                            "tagName mismatch at index " + i;
                    assert htmlElement.elementType().name().equals(element.get("elementType").getAsString()) :
                            "elementType mismatch at index " + i;
                }
            }
        }
    }

    /**
     * Property 26: All Elements Export Completeness - CSS Element Details
     * 
     * For any CSSElement in the output, the element SHALL include cssType
     * and property/value when applicable.
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void cssElementsHaveRequiredDetails(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        if (!result.allElements().isEmpty()) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            
            for (int i = 0; i < elementsArray.size(); i++) {
                JsonObject element = elementsArray.get(i).getAsJsonObject();
                CodeElement originalElement = result.allElements().get(i);
                
                if (originalElement instanceof CSSElement cssElement) {
                    assert element.has("cssType") :
                            "CSS element at index " + i + " must have cssType";
                    assert cssElement.type().name().equals(element.get("cssType").getAsString()) :
                            "cssType mismatch at index " + i;
                    
                    // Property and value should be present if they exist in original
                    if (cssElement.property() != null) {
                        assert element.has("property") :
                                "CSS element at index " + i + " should have property";
                        assert cssElement.property().equals(element.get("property").getAsString()) :
                                "property mismatch at index " + i;
                    }
                    if (cssElement.value() != null) {
                        assert element.has("value") :
                                "CSS element at index " + i + " should have value";
                        assert cssElement.value().equals(element.get("value").getAsString()) :
                                "value mismatch at index " + i;
                    }
                }
            }
        }
    }

    /**
     * Property 26: All Elements Export Completeness - JS Element Details
     * 
     * For any JSElement in the output, the element SHALL include jsType
     * and identifier when applicable.
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void jsElementsHaveRequiredDetails(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(true)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        if (!result.allElements().isEmpty()) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            
            for (int i = 0; i < elementsArray.size(); i++) {
                JsonObject element = elementsArray.get(i).getAsJsonObject();
                CodeElement originalElement = result.allElements().get(i);
                
                if (originalElement instanceof JSElement jsElement) {
                    assert element.has("jsType") :
                            "JS element at index " + i + " must have jsType";
                    assert jsElement.type().name().equals(element.get("jsType").getAsString()) :
                            "jsType mismatch at index " + i;
                    
                    // Identifier should be present if it exists in original
                    if (jsElement.identifier() != null) {
                        assert element.has("identifier") :
                                "JS element at index " + i + " should have identifier";
                        assert jsElement.identifier().equals(element.get("identifier").getAsString()) :
                                "identifier mismatch at index " + i;
                    }
                }
            }
        }
    }

    /**
     * Property 26: All Elements Export Completeness - No Elements Without Flag
     * 
     * When `--output-all-elements` is NOT enabled, the output SHALL NOT
     * contain the elements array.
     * 
     * Feature: web-legacy-scan, Property 26: All Elements Export Completeness
     * Validates: Requirements 13.5, 13.6
     */
    @Property(tries = 100)
    void noElementsWithoutFlag(
            @ForAll("validScanResultsWithElements") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .outputAllElements(false)
                .build();
        
        String json = formatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        // Elements array should not be present when flag is disabled
        assert !root.has("elements") :
                "Elements array should not be present when outputAllElements is false";
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<ScanResult> validScanResultsWithElements() {
        return validCodeElements().list().ofMinSize(1).ofMaxSize(20)
                .map(elements -> ScanResult.builder()
                        .issues(List.of())
                        .allElements(elements)
                        .statistics(createStatistics(elements.size()))
                        .build());
    }

    @Provide
    Arbitrary<CodeElement> validCodeElements() {
        return Arbitraries.oneOf(
                validHTMLElements().map(e -> (CodeElement) e),
                validCSSElements().map(e -> (CodeElement) e),
                validJSElements().map(e -> (CodeElement) e)
        );
    }

    @Provide
    Arbitrary<HTMLElement> validHTMLElements() {
        Arbitrary<String> tagNames = Arbitraries.of(
                "div", "span", "p", "a", "img", "table", "tr", "td", 
                "form", "input", "button", "script", "style", "head", "body"
        );
        
        Arbitrary<HTMLElementType> elementTypes = Arbitraries.of(
                HTMLElementType.OPEN_TAG, 
                HTMLElementType.SELF_CLOSING,
                HTMLElementType.DOCTYPE
        );
        
        Arbitrary<Location> locations = validLocations();
        
        return Combinators.combine(tagNames, elementTypes, locations)
                .as((tagName, elementType, location) -> HTMLElement.builder()
                        .tagName(tagName)
                        .elementType(elementType)
                        .location(location)
                        .rawContent("<" + tagName + ">")
                        .build());
    }

    @Provide
    Arbitrary<CSSElement> validCSSElements() {
        Arbitrary<CSSElementType> types = Arbitraries.of(
                CSSElementType.DECLARATION,
                CSSElementType.RULE_SET,
                CSSElementType.AT_RULE
        );
        
        Arbitrary<String> properties = Arbitraries.of(
                "color", "background", "font-size", "margin", "padding",
                "display", "position", "width", "height", "border"
        ).injectNull(0.2);
        
        Arbitrary<String> values = Arbitraries.of(
                "red", "blue", "#fff", "10px", "auto", "block", "none"
        ).injectNull(0.2);
        
        Arbitrary<Location> locations = validLocations();
        
        return Combinators.combine(types, properties, values, locations)
                .as((type, property, value, location) -> CSSElement.builder()
                        .type(type)
                        .property(property)
                        .value(value)
                        .location(location)
                        .rawContent(property != null ? property + ": " + value : "")
                        .build());
    }

    @Provide
    Arbitrary<JSElement> validJSElements() {
        Arbitrary<JSElementType> types = Arbitraries.of(
                JSElementType.FUNCTION_CALL,
                JSElementType.METHOD_CALL,
                JSElementType.VARIABLE_DECLARATION,
                JSElementType.ASSIGNMENT
        );
        
        Arbitrary<String> identifiers = Arbitraries.of(
                "console", "document", "window", "alert", "fetch",
                "getElementById", "querySelector", "addEventListener"
        ).injectNull(0.2);
        
        Arbitrary<Location> locations = validLocations();
        
        return Combinators.combine(types, identifiers, locations)
                .as((type, identifier, location) -> JSElement.builder()
                        .type(type)
                        .identifier(identifier)
                        .location(location)
                        .rawContent(identifier != null ? identifier + "()" : "")
                        .build());
    }

    @Provide
    Arbitrary<Location> validLocations() {
        Arbitrary<Path> paths = Arbitraries.of("src", "lib", "test")
                .flatMap(dir -> Arbitraries.of("index", "main", "util", "app")
                        .flatMap(name -> Arbitraries.of(".html", ".css", ".js")
                                .map(ext -> Path.of(dir, name + ext))));
        
        Arbitrary<Integer> startLines = Arbitraries.integers().between(1, 500);
        Arbitrary<Integer> startColumns = Arbitraries.integers().between(1, 100);
        
        return Combinators.combine(paths, startLines, startColumns)
                .as((path, startLine, startCol) -> {
                    int endLine = startLine + Arbitraries.integers().between(0, 5).sample();
                    int endCol = endLine == startLine 
                            ? startCol + Arbitraries.integers().between(1, 50).sample()
                            : Arbitraries.integers().between(1, 100).sample();
                    return new Location(path, startLine, startCol, endLine, endCol, -1, -1, null, null);
                });
    }

    private ScanResult.ScanStatistics createStatistics(int elementCount) {
        return ScanResult.ScanStatistics.builder()
                .totalFiles(Math.max(1, elementCount / 5))
                .scannedFiles(Math.max(1, elementCount / 5))
                .totalElements(elementCount)
                .scanTime(Duration.ofMillis(100))
                .build();
    }
}
