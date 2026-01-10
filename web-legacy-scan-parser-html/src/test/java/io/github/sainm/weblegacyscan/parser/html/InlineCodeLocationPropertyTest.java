package io.github.sainm.weblegacyscan.parser.html;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.ParseResult;
import net.jqwik.api.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Property-based tests for Inline Code Location Mapping.
 * 
 * Property 16: Inline Code Line Number Mapping
 * For any HTML file containing inline script or style blocks, Issues detected in inline code
 * SHALL report line numbers relative to the original HTML file, not the extracted code block.
 * 
 * Property 25: Inline Code Absolute Position Calculation
 * For any CodeElement extracted from inline code (script/style in HTML), calling location.toAbsolute()
 * SHALL return a Location with line numbers relative to the original HTML file, and the calculated
 * position SHALL correctly point to the element in the source file.
 * 
 * Validates: Requirements 11.5, 13.8
 */
class InlineCodeLocationPropertyTest {

    private static final Path TEST_FILE = Path.of("test.html");
    private final JerichoHTMLParser parser = new JerichoHTMLParser();

    /**
     * Property 16: Inline Code Line Number Mapping - Inline script location is within HTML bounds
     * 
     * For any HTML file containing inline script blocks, the extracted JSElement's location
     * SHALL have line numbers that are valid within the original HTML file.
     * 
     * Feature: web-legacy-scan, Property 16: Inline Code Line Number Mapping
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void inlineScriptLocationIsWithinHtmlBounds(
            @ForAll("htmlWithInlineScript") HtmlWithInlineCode htmlData
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, 
                                               htmlData.content().length());
        ParseResult result = parser.parseContent(htmlData.content(), sourceFile);
        
        int totalLines = countLines(htmlData.content());
        
        for (CodeElement element : result.elements()) {
            if (element instanceof JSElement jsElement) {
                Location loc = jsElement.getLocation();
                
                // Line numbers must be within the HTML file bounds
                assert loc.startLine() >= 1 : 
                    "Inline script startLine must be >= 1, got: " + loc.startLine();
                assert loc.startLine() <= totalLines : 
                    "Inline script startLine must be <= total lines (" + totalLines + "), got: " + loc.startLine();
                assert loc.endLine() >= loc.startLine() : 
                    "Inline script endLine must be >= startLine";
            }
        }
    }

    /**
     * Property 16: Inline Code Line Number Mapping - Inline style location is within HTML bounds
     * 
     * For any HTML file containing inline style blocks, the extracted CSSElement's location
     * SHALL have line numbers that are valid within the original HTML file.
     * 
     * Feature: web-legacy-scan, Property 16: Inline Code Line Number Mapping
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void inlineStyleLocationIsWithinHtmlBounds(
            @ForAll("htmlWithInlineStyle") HtmlWithInlineCode htmlData
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, 
                                               htmlData.content().length());
        ParseResult result = parser.parseContent(htmlData.content(), sourceFile);
        
        int totalLines = countLines(htmlData.content());
        
        for (CodeElement element : result.elements()) {
            if (element instanceof CSSElement cssElement) {
                Location loc = cssElement.getLocation();
                
                // Line numbers must be within the HTML file bounds
                assert loc.startLine() >= 1 : 
                    "Inline style startLine must be >= 1, got: " + loc.startLine();
                assert loc.startLine() <= totalLines : 
                    "Inline style startLine must be <= total lines (" + totalLines + "), got: " + loc.startLine();
                assert loc.endLine() >= loc.startLine() : 
                    "Inline style endLine must be >= startLine";
            }
        }
    }

    /**
     * Property 16: Inline Code Line Number Mapping - Inline script line matches expected position
     * 
     * For any HTML file with inline script at a known line, the extracted JSElement's location
     * SHALL report the correct line number relative to the HTML file.
     * 
     * Feature: web-legacy-scan, Property 16: Inline Code Line Number Mapping
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void inlineScriptLineMatchesExpectedPosition(
            @ForAll("htmlWithInlineScript") HtmlWithInlineCode htmlData
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, 
                                               htmlData.content().length());
        ParseResult result = parser.parseContent(htmlData.content(), sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof JSElement jsElement) {
                Location loc = jsElement.getLocation();
                
                // The inline script location should be at the expected script tag line
                // (the content segment starts right after the opening <script> tag)
                assert loc.startLine() == htmlData.expectedInlineStartLine() : 
                    "Inline script should start at line " + htmlData.expectedInlineStartLine() + 
                    ", but got: " + loc.startLine();
            }
        }
    }

    /**
     * Property 16: Inline Code Line Number Mapping - Inline style line matches expected position
     * 
     * For any HTML file with inline style at a known line, the extracted CSSElement's location
     * SHALL report the correct line number relative to the HTML file.
     * 
     * Feature: web-legacy-scan, Property 16: Inline Code Line Number Mapping
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    void inlineStyleLineMatchesExpectedPosition(
            @ForAll("htmlWithInlineStyle") HtmlWithInlineCode htmlData
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, 
                                               htmlData.content().length());
        ParseResult result = parser.parseContent(htmlData.content(), sourceFile);
        
        for (CodeElement element : result.elements()) {
            if (element instanceof CSSElement cssElement) {
                Location loc = cssElement.getLocation();
                
                // The inline style location should be at the expected style tag line
                assert loc.startLine() == htmlData.expectedInlineStartLine() : 
                    "Inline style should start at line " + htmlData.expectedInlineStartLine() + 
                    ", but got: " + loc.startLine();
            }
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute preserves file path
     * 
     * For any Location with an inlineParent, calling toAbsolute() SHALL return a Location
     * with the same filePath as the inlineParent.
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsolutePreservesFilePath(
            @ForAll("locationWithInlineParent") Location location
    ) {
        if (location.inlineParent() != null) {
            Location absolute = location.toAbsolute();
            
            assert absolute.filePath().equals(location.inlineParent().filePath()) :
                "toAbsolute() should preserve inlineParent's filePath";
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute calculates correct line
     * 
     * For any Location with an inlineParent, calling toAbsolute() SHALL return a Location
     * with startLine = inlineParent.startLine + location.startLine - 1.
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsoluteCalculatesCorrectLine(
            @ForAll("locationWithInlineParent") Location location
    ) {
        if (location.inlineParent() != null) {
            Location absolute = location.toAbsolute();
            
            int expectedStartLine = location.inlineParent().startLine() + location.startLine() - 1;
            int expectedEndLine = location.inlineParent().startLine() + location.endLine() - 1;
            
            assert absolute.startLine() == expectedStartLine :
                "toAbsolute() startLine should be " + expectedStartLine + ", got: " + absolute.startLine();
            assert absolute.endLine() == expectedEndLine :
                "toAbsolute() endLine should be " + expectedEndLine + ", got: " + absolute.endLine();
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute calculates correct column for first line
     * 
     * For any Location with an inlineParent where startLine == 1, calling toAbsolute() SHALL return
     * a Location with startColumn = inlineParent.startColumn + location.startColumn - 1.
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsoluteCalculatesCorrectColumnForFirstLine(
            @ForAll("locationWithInlineParentFirstLine") Location location
    ) {
        if (location.inlineParent() != null && location.startLine() == 1) {
            Location absolute = location.toAbsolute();
            
            int expectedStartColumn = location.inlineParent().startColumn() + location.startColumn() - 1;
            
            assert absolute.startColumn() == expectedStartColumn :
                "toAbsolute() startColumn for first line should be " + expectedStartColumn + 
                ", got: " + absolute.startColumn();
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute preserves column for non-first lines
     * 
     * For any Location with an inlineParent where startLine > 1, calling toAbsolute() SHALL return
     * a Location with the same startColumn (column is relative to the line start).
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsolutePreservesColumnForNonFirstLines(
            @ForAll("locationWithInlineParentNonFirstLine") Location location
    ) {
        if (location.inlineParent() != null && location.startLine() > 1) {
            Location absolute = location.toAbsolute();
            
            assert absolute.startColumn() == location.startColumn() :
                "toAbsolute() startColumn for non-first line should be preserved, expected: " + 
                location.startColumn() + ", got: " + absolute.startColumn();
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute calculates correct offset
     * 
     * For any Location with an inlineParent with valid offsets, calling toAbsolute() SHALL return
     * a Location with startOffset = inlineParent.startOffset + location.startOffset.
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsoluteCalculatesCorrectOffset(
            @ForAll("locationWithInlineParentAndOffsets") Location location
    ) {
        if (location.inlineParent() != null && 
            location.inlineParent().startOffset() >= 0 && 
            location.startOffset() >= 0) {
            
            Location absolute = location.toAbsolute();
            
            int expectedStartOffset = location.inlineParent().startOffset() + location.startOffset();
            int expectedEndOffset = location.inlineParent().startOffset() + location.endOffset();
            
            assert absolute.startOffset() == expectedStartOffset :
                "toAbsolute() startOffset should be " + expectedStartOffset + ", got: " + absolute.startOffset();
            assert absolute.endOffset() == expectedEndOffset :
                "toAbsolute() endOffset should be " + expectedEndOffset + ", got: " + absolute.endOffset();
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute removes inlineParent
     * 
     * For any Location with an inlineParent, calling toAbsolute() SHALL return a Location
     * with inlineParent == null (the position is now absolute).
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsoluteRemovesInlineParent(
            @ForAll("locationWithInlineParent") Location location
    ) {
        if (location.inlineParent() != null) {
            Location absolute = location.toAbsolute();
            
            assert absolute.inlineParent() == null :
                "toAbsolute() should return a Location with null inlineParent";
        }
    }

    /**
     * Property 25: Inline Code Absolute Position Calculation - toAbsolute is idempotent for absolute locations
     * 
     * For any Location without an inlineParent, calling toAbsolute() SHALL return the same Location.
     * 
     * Feature: web-legacy-scan, Property 25: Inline Code Absolute Position Calculation
     * Validates: Requirements 13.8
     */
    @Property(tries = 100)
    void toAbsoluteIsIdempotentForAbsoluteLocations(
            @ForAll("absoluteLocation") Location location
    ) {
        Location result = location.toAbsolute();
        
        assert result.equals(location) :
            "toAbsolute() on an absolute location should return the same location";
    }

    // ========== Helper Methods ==========

    private int countLines(String content) {
        if (content.isEmpty()) return 1;
        int lines = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    // ========== Data Classes ==========

    record HtmlWithInlineCode(String content, int expectedInlineStartLine) {}

    // ========== Providers ==========

    @Provide
    Arbitrary<HtmlWithInlineCode> htmlWithInlineScript() {
        return Combinators.combine(
            Arbitraries.integers().between(0, 5),  // prefix lines
            jsCode(),
            Arbitraries.integers().between(0, 3)   // suffix lines
        ).as((prefixLines, jsCode, suffixLines) -> {
            StringBuilder sb = new StringBuilder();
            
            // Add prefix lines
            for (int i = 0; i < prefixLines; i++) {
                sb.append("<div>line ").append(i + 1).append("</div>\n");
            }
            
            // Add script tag - content starts on the same line as the opening tag
            // The content segment begins right after <script>
            int scriptTagLine = prefixLines + 1; // 1-based line number
            sb.append("<script>").append(jsCode).append("</script>\n");
            
            // Add suffix lines
            for (int i = 0; i < suffixLines; i++) {
                sb.append("<div>suffix ").append(i + 1).append("</div>\n");
            }
            
            return new HtmlWithInlineCode(sb.toString(), scriptTagLine);
        });
    }

    @Provide
    Arbitrary<HtmlWithInlineCode> htmlWithInlineStyle() {
        return Combinators.combine(
            Arbitraries.integers().between(0, 5),  // prefix lines
            cssCode(),
            Arbitraries.integers().between(0, 3)   // suffix lines
        ).as((prefixLines, cssCode, suffixLines) -> {
            StringBuilder sb = new StringBuilder();
            
            // Add prefix lines
            for (int i = 0; i < prefixLines; i++) {
                sb.append("<div>line ").append(i + 1).append("</div>\n");
            }
            
            // Add style tag - content starts on the same line as the opening tag
            int styleTagLine = prefixLines + 1; // 1-based line number
            sb.append("<style>").append(cssCode).append("</style>\n");
            
            // Add suffix lines
            for (int i = 0; i < suffixLines; i++) {
                sb.append("<div>suffix ").append(i + 1).append("</div>\n");
            }
            
            return new HtmlWithInlineCode(sb.toString(), styleTagLine);
        });
    }

    @Provide
    Arbitrary<Location> locationWithInlineParent() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 10),  // parent startLine
            Arbitraries.integers().between(1, 50),  // parent startColumn
            Arbitraries.integers().between(1, 5),   // relative startLine
            Arbitraries.integers().between(1, 20)   // relative startColumn
        ).as((parentLine, parentCol, relLine, relCol) -> {
            Location parent = new Location(
                TEST_FILE, parentLine, parentCol, parentLine + 10, parentCol + 100,
                100, 500, null, null
            );
            return new Location(
                TEST_FILE, relLine, relCol, relLine + 2, relCol + 10,
                0, 50, null, parent
            );
        });
    }

    @Provide
    Arbitrary<Location> locationWithInlineParentFirstLine() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 10),  // parent startLine
            Arbitraries.integers().between(1, 50),  // parent startColumn
            Arbitraries.integers().between(1, 20)   // relative startColumn
        ).as((parentLine, parentCol, relCol) -> {
            Location parent = new Location(
                TEST_FILE, parentLine, parentCol, parentLine + 10, parentCol + 100,
                100, 500, null, null
            );
            // startLine = 1 (first line of inline code)
            return new Location(
                TEST_FILE, 1, relCol, 1, relCol + 10,
                0, 50, null, parent
            );
        });
    }

    @Provide
    Arbitrary<Location> locationWithInlineParentNonFirstLine() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 10),  // parent startLine
            Arbitraries.integers().between(1, 50),  // parent startColumn
            Arbitraries.integers().between(2, 10),  // relative startLine (> 1)
            Arbitraries.integers().between(1, 20)   // relative startColumn
        ).as((parentLine, parentCol, relLine, relCol) -> {
            Location parent = new Location(
                TEST_FILE, parentLine, parentCol, parentLine + 10, parentCol + 100,
                100, 500, null, null
            );
            return new Location(
                TEST_FILE, relLine, relCol, relLine + 2, relCol + 10,
                0, 50, null, parent
            );
        });
    }

    @Provide
    Arbitrary<Location> locationWithInlineParentAndOffsets() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 10),   // parent startLine
            Arbitraries.integers().between(1, 50),   // parent startColumn
            Arbitraries.integers().between(100, 500), // parent startOffset
            Arbitraries.integers().between(1, 5),    // relative startLine
            Arbitraries.integers().between(1, 20),   // relative startColumn
            Arbitraries.integers().between(0, 100)   // relative startOffset
        ).as((parentLine, parentCol, parentOffset, relLine, relCol, relOffset) -> {
            Location parent = new Location(
                TEST_FILE, parentLine, parentCol, parentLine + 10, parentCol + 100,
                parentOffset, parentOffset + 400, null, null
            );
            return new Location(
                TEST_FILE, relLine, relCol, relLine + 2, relCol + 10,
                relOffset, relOffset + 50, null, parent
            );
        });
    }

    @Provide
    Arbitrary<Location> absoluteLocation() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 100),  // startLine
            Arbitraries.integers().between(1, 100),  // startColumn
            Arbitraries.integers().between(0, 5)     // additional lines
        ).as((startLine, startCol, additionalLines) -> 
            new Location(
                TEST_FILE, startLine, startCol, startLine + additionalLines, startCol + 20,
                -1, -1, null, null
            )
        );
    }

    private Arbitrary<String> jsCode() {
        return Arbitraries.of(
            "console.log('hello');",
            "var x = 1;",
            "function test() { return 42; }",
            "document.getElementById('id');",
            "alert('test');"
        );
    }

    private Arbitrary<String> cssCode() {
        return Arbitraries.of(
            ".class { color: red; }",
            "body { margin: 0; }",
            "#id { display: block; }",
            "p { font-size: 14px; }",
            "div { padding: 10px; }"
        );
    }
}
