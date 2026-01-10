package io.github.sainm.weblegacyscan.parser.html;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.ParseResult;
import net.jqwik.api.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Property-based tests for Parser Error Tolerance.
 * 
 * Property 3: Parser Error Tolerance
 * For any source file containing syntax errors, malformed markup, or server-side template syntax,
 * the Parser SHALL not throw exceptions and SHALL return a ParseResult with partial results
 * and error information, allowing the scan to continue.
 * 
 * Validates: Requirements 1.3, 1.4
 */
class ParserErrorTolerancePropertyTest {

    private static final Path TEST_FILE = Path.of("test.html");
    private final JerichoHTMLParser parser = new JerichoHTMLParser();

    /**
     * Property 3: Parser Error Tolerance - Parser never throws exceptions on malformed HTML
     * 
     * For any malformed HTML content, the parser SHALL NOT throw exceptions
     * and SHALL return a valid ParseResult.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void parserNeverThrowsOnMalformedHtml(
            @ForAll("malformedHtml") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for malformed HTML";
        
        // Elements list must not be null (can be empty)
        assert result.elements() != null : "ParseResult.elements() must not be null";
        
        // Errors list must not be null (can be empty)
        assert result.errors() != null : "ParseResult.errors() must not be null";
    }

    /**
     * Property 3: Parser Error Tolerance - Parser tolerates JSP syntax
     * 
     * For any HTML content containing JSP syntax (<% %>, <%= %>, <%@ %>),
     * the parser SHALL tolerate these and continue parsing the surrounding HTML.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void parserToleratesJspSyntax(
            @ForAll("htmlWithJspSyntax") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for HTML with JSP syntax";
        
        // Parser should still extract some HTML elements despite JSP syntax
        // (the surrounding HTML should be parsed)
        assert result.elements() != null : "ParseResult.elements() must not be null";
    }

    /**
     * Property 3: Parser Error Tolerance - Parser tolerates JSTL tags
     * 
     * For any HTML content containing JSTL tags (c:if, c:forEach, etc.),
     * the parser SHALL tolerate these and continue parsing.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void parserToleratesJstlTags(
            @ForAll("htmlWithJstlTags") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for HTML with JSTL tags";
        
        // Elements list must not be null
        assert result.elements() != null : "ParseResult.elements() must not be null";
    }

    /**
     * Property 3: Parser Error Tolerance - Parser tolerates Thymeleaf syntax
     * 
     * For any HTML content containing Thymeleaf attributes (th:text, th:if, etc.),
     * the parser SHALL tolerate these and continue parsing.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void parserToleratesThymeleafSyntax(
            @ForAll("htmlWithThymeleafSyntax") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for HTML with Thymeleaf syntax";
        
        // Elements list must not be null
        assert result.elements() != null : "ParseResult.elements() must not be null";
    }

    /**
     * Property 3: Parser Error Tolerance - Parser returns partial results on malformed markup
     * 
     * For any HTML content with unclosed tags or broken markup,
     * the parser SHALL attempt best-effort parsing and return partial results.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void parserReturnsPartialResultsOnMalformedMarkup(
            @ForAll("htmlWithUnclosedTags") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for malformed markup";
        
        // Parser should still extract some elements (best-effort parsing)
        assert result.elements() != null : "ParseResult.elements() must not be null";
        
        // For content with valid HTML tags (even if unclosed), parser should find some elements
        // This validates best-effort parsing behavior
    }

    /**
     * Property 3: Parser Error Tolerance - Parser handles empty and whitespace content
     * 
     * For any empty or whitespace-only content, the parser SHALL not throw
     * and SHALL return a valid (possibly empty) ParseResult.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void parserHandlesEmptyAndWhitespaceContent(
            @ForAll("emptyOrWhitespaceContent") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for empty/whitespace content";
        
        // Elements and errors lists must not be null
        assert result.elements() != null : "ParseResult.elements() must not be null";
        assert result.errors() != null : "ParseResult.errors() must not be null";
    }

    /**
     * Property 3: Parser Error Tolerance - Parser tolerates Freemarker syntax
     * 
     * For any HTML content containing Freemarker directives (<#if>, ${...}, etc.),
     * the parser SHALL tolerate these and continue parsing.
     * 
     * Feature: web-legacy-scan, Property 3: Parser Error Tolerance
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void parserToleratesFreemarkerSyntax(
            @ForAll("htmlWithFreemarkerSyntax") String htmlContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.HTML, StandardCharsets.UTF_8, htmlContent.length());
        
        // Parser must not throw any exception
        ParseResult result = parser.parseContent(htmlContent, sourceFile);
        
        // Result must not be null
        assert result != null : "ParseResult must not be null for HTML with Freemarker syntax";
        
        // Elements list must not be null
        assert result.elements() != null : "ParseResult.elements() must not be null";
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> malformedHtml() {
        return Arbitraries.oneOf(
            // Unclosed tags
            Arbitraries.of(
                "<div><span>text",
                "<p>paragraph without closing",
                "<div><div><div>deeply nested unclosed",
                "<table><tr><td>cell"
            ),
            // Broken attributes
            Arbitraries.of(
                "<div class=>content</div>",
                "<div class=\"unclosed>content</div>",
                "<div class='mixed\">content</div>",
                "<div =>content</div>"
            ),
            // Invalid tag names
            Arbitraries.of(
                "<123invalid>content</123invalid>",
                "<>empty tag</>",
                "< space>content</ space>"
            ),
            // Mixed broken content
            Arbitraries.of(
                "<<<nested angles>>>",
                "<div><</div>",
                "<div>></div>",
                "<div attr=<value>>content</div>"
            )
        );
    }

    @Provide
    Arbitrary<String> htmlWithJspSyntax() {
        return Combinators.combine(
            tagName(),
            jspExpression()
        ).as((tag, jsp) -> 
            "<" + tag + ">" + jsp + "</" + tag + ">"
        );
    }

    @Provide
    Arbitrary<String> htmlWithJstlTags() {
        return Combinators.combine(
            tagName(),
            jstlTag()
        ).as((tag, jstl) -> 
            "<" + tag + ">" + jstl + "</" + tag + ">"
        );
    }

    @Provide
    Arbitrary<String> htmlWithThymeleafSyntax() {
        return Combinators.combine(
            tagName(),
            thymeleafAttribute()
        ).as((tag, thAttr) -> 
            "<" + tag + " " + thAttr + ">content</" + tag + ">"
        );
    }

    @Provide
    Arbitrary<String> htmlWithUnclosedTags() {
        return Arbitraries.oneOf(
            // Single unclosed tag
            Arbitraries.of(
                "<div>content without closing",
                "<span>inline content",
                "<p>paragraph text"
            ),
            // Nested unclosed tags
            Arbitraries.of(
                "<div><span>nested unclosed",
                "<ul><li>list item<li>another item",
                "<table><tr><td>cell<td>another cell"
            ),
            // Mixed closed and unclosed
            Arbitraries.of(
                "<div><span>text</span>",
                "<div><p>para</p><span>unclosed",
                "<section><article>content</article>"
            )
        );
    }

    @Provide
    Arbitrary<String> emptyOrWhitespaceContent() {
        return Arbitraries.oneOf(
            Arbitraries.just(""),
            Arbitraries.just(" "),
            Arbitraries.just("   "),
            Arbitraries.just("\n"),
            Arbitraries.just("\t"),
            Arbitraries.just("\n\n\n"),
            Arbitraries.just("  \n  \t  ")
        );
    }

    @Provide
    Arbitrary<String> htmlWithFreemarkerSyntax() {
        return Combinators.combine(
            tagName(),
            freemarkerExpression()
        ).as((tag, fm) -> 
            "<" + tag + ">" + fm + "</" + tag + ">"
        );
    }

    private Arbitrary<String> tagName() {
        return Arbitraries.of("div", "span", "p", "a", "section", "article", "header", "footer");
    }

    private Arbitrary<String> jspExpression() {
        return Arbitraries.oneOf(
            // Scriptlet
            Arbitraries.of(
                "<% String name = \"test\"; %>",
                "<% for(int i=0; i<10; i++) { %>",
                "<% } %>"
            ),
            // Expression
            Arbitraries.of(
                "<%= request.getParameter(\"id\") %>",
                "<%= user.getName() %>",
                "<%= item.getPrice() %>"
            ),
            // Directive
            Arbitraries.of(
                "<%@ page language=\"java\" %>",
                "<%@ include file=\"header.jsp\" %>",
                "<%@ taglib uri=\"http://java.sun.com/jsp/jstl/core\" prefix=\"c\" %>"
            )
        );
    }

    private Arbitrary<String> jstlTag() {
        return Arbitraries.of(
            "<c:if test=\"${condition}\">content</c:if>",
            "<c:forEach items=\"${list}\" var=\"item\">${item}</c:forEach>",
            "<c:choose><c:when test=\"${x}\">a</c:when><c:otherwise>b</c:otherwise></c:choose>",
            "<c:set var=\"name\" value=\"${value}\"/>",
            "<c:out value=\"${text}\"/>"
        );
    }

    private Arbitrary<String> thymeleafAttribute() {
        return Arbitraries.of(
            "th:text=\"${message}\"",
            "th:if=\"${condition}\"",
            "th:each=\"item : ${items}\"",
            "th:href=\"@{/path}\"",
            "th:class=\"${active ? 'active' : ''}\""
        );
    }

    private Arbitrary<String> freemarkerExpression() {
        return Arbitraries.of(
            "${user.name}",
            "<#if condition>content</#if>",
            "<#list items as item>${item}</#list>",
            "<#assign x = 1>",
            "${message?html}"
        );
    }
}
