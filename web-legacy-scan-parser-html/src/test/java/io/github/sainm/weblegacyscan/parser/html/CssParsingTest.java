package io.github.sainm.weblegacyscan.parser.html;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;
import com.helger.css.writer.CSSWriterSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test CSS parsing with ph-css library.
 */
public class CssParsingTest {

    @Test
    void testSimpleCssParsing() {
        String css = "div { color: red; }";
        CascadingStyleSheet parsed = CSSReader.readFromString(
            css, ECSSVersion.CSS30, new DoNothingCSSParseErrorHandler());
        
        assertNotNull(parsed, "CSS should be parsed");
        assertEquals(1, parsed.getAllRules().size(), "Should have 1 rule");
        
        ICSSTopLevelRule rule = parsed.getAllRules().get(0);
        assertTrue(rule instanceof CSSStyleRule, "Should be a style rule");
        
        CSSStyleRule styleRule = (CSSStyleRule) rule;
        CSSWriterSettings settings = new CSSWriterSettings(ECSSVersion.CSS30);
        String selector = styleRule.getSelectorsAsCSSString(settings, 0);
        System.out.println("Selector: " + selector);
        
        assertEquals(1, styleRule.getDeclarationCount(), "Should have 1 declaration");
        CSSDeclaration decl = styleRule.getDeclarationAtIndex(0);
        assertEquals("color", decl.getProperty());
        assertEquals("red", decl.getExpressionAsCSSString());
    }

    @Test
    void testCssWithHtmlComments() {
        // This is what legacy JSP files often have
        String rawCss = "<!-- th.width0 { width: 150px; } -->";
        
        // Clean HTML comments
        String cleaned = cleanHtmlCommentsFromCss(rawCss);
        System.out.println("Cleaned CSS: [" + cleaned + "]");
        
        CascadingStyleSheet parsed = CSSReader.readFromString(
            cleaned, ECSSVersion.CSS30, new DoNothingCSSParseErrorHandler());
        
        assertNotNull(parsed, "CSS should be parsed after cleaning");
        assertEquals(1, parsed.getAllRules().size(), "Should have 1 rule");
    }

    @Test
    void testMultipleRules() {
        String css = "div#navbar { display: none; } div { text-align: left; }";
        CascadingStyleSheet parsed = CSSReader.readFromString(
            css, ECSSVersion.CSS30, new DoNothingCSSParseErrorHandler());
        
        assertNotNull(parsed);
        assertEquals(2, parsed.getAllRules().size(), "Should have 2 rules");
        
        for (ICSSTopLevelRule rule : parsed.getAllRules()) {
            if (rule instanceof CSSStyleRule styleRule) {
                CSSWriterSettings settings = new CSSWriterSettings(ECSSVersion.CSS30);
                System.out.println("Rule selector: " + styleRule.getSelectorsAsCSSString(settings, 0));
                for (CSSDeclaration decl : styleRule.getAllDeclarations()) {
                    System.out.println("  " + decl.getProperty() + ": " + decl.getExpressionAsCSSString());
                }
            }
        }
    }

    private String cleanHtmlCommentsFromCss(String cssContent) {
        String cleaned = cssContent;
        // Remove opening HTML comment
        cleaned = cleaned.replaceAll("<!--\\s*", "");
        // Remove closing HTML comment
        cleaned = cleaned.replaceAll("\\s*-->", "");
        // Also handle <!- (malformed comment)
        cleaned = cleaned.replaceAll("<!-\\s*", "");
        return cleaned.trim();
    }
}
