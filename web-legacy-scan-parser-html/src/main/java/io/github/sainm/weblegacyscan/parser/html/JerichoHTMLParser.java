package io.github.sainm.weblegacyscan.parser.html;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;
import net.htmlparser.jericho.*;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * HTML/JSP Parser based on Jericho-HTML library.
 * Supports both HTML and JSP files.
 */
public class JerichoHTMLParser implements Parser {

    private static final Set<FileType> SUPPORTED_TYPES = Set.of(FileType.HTML, FileType.JSP);

    private static final Set<String> INLINE_EVENT_HANDLERS = Set.of(
        "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover",
        "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup",
        "onfocus", "onblur", "onchange", "onsubmit", "onreset", "onselect",
        "onload", "onunload", "onerror", "onresize", "onscroll"
    );

    @Override
    public ParseResult parse(SourceFile file) {
        try {
            String content = Files.readString(file.path(), file.encoding());
            return parseContent(content, file);
        } catch (IOException e) {
            return ParseResult.failure(List.of(
                ParseError.fatal(file.path(), 1, 1, "Failed to read file: " + e.getMessage())
            ));
        }
    }

    @Override
    public ParseResult parseContent(String content, SourceFile file) {
        Instant start = Instant.now();
        List<CodeElement> elements = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        
        try {
            Source source = new Source(content);
            source.fullSequentialParse();

            // Parse all elements
            List<Element> allElements = source.getAllElements();
            for (Element element : allElements) {
                try {
                    HTMLElement htmlElement = createHTMLElement(element, file, content);
                    if (htmlElement != null) {
                        elements.add(htmlElement);
                    }
                } catch (Exception e) {
                    errors.add(ParseError.recoverable(file.path(), 
                        getLineNumber(content, element.getBegin()), 1,
                        "Failed to parse element: " + e.getMessage()));
                }
            }

            // Extract inline scripts and styles
            extractInlineContent(source, file, content, elements);

        } catch (Exception e) {
            errors.add(ParseError.recoverable(file.path(), 1, 1, 
                "HTML parsing error: " + e.getMessage()));
        }

        Duration parseTime = Duration.between(start, Instant.now());
        int htmlCount = (int) elements.stream()
            .filter(e -> e instanceof HTMLElement)
            .count();
        
        ParseStatistics stats = ParseStatistics.builder()
            .totalElements(elements.size())
            .htmlElements(htmlCount)
            .parseTime(parseTime)
            .build();

        return ParseResult.partial(elements, errors, stats);
    }

    private HTMLElement createHTMLElement(Element element, SourceFile file, String content) {
        StartTag startTag = element.getStartTag();
        if (startTag == null) return null;

        String tagName = element.getName().toLowerCase();
        
        int startLine = getLineNumber(content, startTag.getBegin());
        int startColumn = getColumnNumber(content, startTag.getBegin());
        int endLine = getLineNumber(content, element.getEnd());
        int endColumn = getColumnNumber(content, element.getEnd());

        Location location = new Location(
            file.path(), startLine, startColumn, endLine, endColumn,
            startTag.getBegin(), element.getEnd(),
            element.toString(), null
        );

        // Tag name location (skip '<')
        int tagNameStart = startTag.getBegin() + 1;
        int tagNameEnd = tagNameStart + tagName.length();
        Location tagNameLocation = new Location(
            file.path(),
            getLineNumber(content, tagNameStart),
            getColumnNumber(content, tagNameStart),
            getLineNumber(content, tagNameEnd),
            getColumnNumber(content, tagNameEnd),
            tagNameStart, tagNameEnd, tagName, null
        );

        // Parse attributes
        Map<String, AttributeInfo> attributes = parseAttributes(startTag, file, content);

        HTMLElementType elementType = determineElementType(element);

        return HTMLElement.builder()
            .tagName(tagName)
            .tagNameLocation(tagNameLocation)
            .attributes(attributes)
            .location(location)
            .rawContent(element.toString())
            .elementType(elementType)
            .build();
    }

    private Map<String, AttributeInfo> parseAttributes(StartTag startTag, SourceFile file, String content) {
        Map<String, AttributeInfo> attributes = new LinkedHashMap<>();
        Attributes attrs = startTag.getAttributes();
        
        if (attrs == null) return attributes;

        for (Attribute attr : attrs) {
            String name = attr.getName().toLowerCase();
            String value = attr.getValue();

            int nameStart = attr.getNameSegment().getBegin();
            int nameEnd = attr.getNameSegment().getEnd();
            Location nameLocation = new Location(
                file.path(),
                getLineNumber(content, nameStart),
                getColumnNumber(content, nameStart),
                getLineNumber(content, nameEnd),
                getColumnNumber(content, nameEnd),
                nameStart, nameEnd, name, null
            );

            Location valueLocation = null;
            if (attr.getValueSegment() != null) {
                int valueStart = attr.getValueSegment().getBegin();
                int valueEnd = attr.getValueSegment().getEnd();
                valueLocation = new Location(
                    file.path(),
                    getLineNumber(content, valueStart),
                    getColumnNumber(content, valueStart),
                    getLineNumber(content, valueEnd),
                    getColumnNumber(content, valueEnd),
                    valueStart, valueEnd, value, null
                );
            }

            Location fullLocation = new Location(
                file.path(),
                getLineNumber(content, attr.getBegin()),
                getColumnNumber(content, attr.getBegin()),
                getLineNumber(content, attr.getEnd()),
                getColumnNumber(content, attr.getEnd()),
                attr.getBegin(), attr.getEnd(),
                attr.toString(), null
            );

            attributes.put(name, new AttributeInfo(name, nameLocation, value, valueLocation, fullLocation));
        }

        return attributes;
    }

    private void extractInlineContent(Source source, SourceFile file, String content, 
                                      List<CodeElement> elements) {
        // Extract inline scripts from <script> tags
        for (Element script : source.getAllElements(HTMLElementName.SCRIPT)) {
            if (script.getAttributeValue("src") == null) {
                extractInlineScript(script, file, content, elements);
            }
        }

        // Extract inline styles from <style> tags
        for (Element style : source.getAllElements(HTMLElementName.STYLE)) {
            extractInlineStyle(style, file, content, elements);
        }
        
        // Extract inline event handlers (onclick, onload, etc.)
        for (Element element : source.getAllElements()) {
            extractInlineEventHandlers(element, file, content, elements);
        }
        
        // Extract EL expressions ${...} for JSP files
        if (file.type() == FileType.JSP) {
            extractELExpressions(file, content, elements);
        }
    }

    /**
     * Extract inline JavaScript from event handler attributes like onclick, onload, etc.
     */
    private void extractInlineEventHandlers(Element element, SourceFile file, String content,
                                            List<CodeElement> elements) {
        Attributes attrs = element.getStartTag() != null ? element.getStartTag().getAttributes() : null;
        if (attrs == null) return;
        
        for (Attribute attr : attrs) {
            String attrName = attr.getName().toLowerCase();
            if (INLINE_EVENT_HANDLERS.contains(attrName)) {
                String jsCode = attr.getValue();
                if (jsCode == null || jsCode.trim().isEmpty()) continue;
                
                Segment valueSegment = attr.getValueSegment();
                if (valueSegment == null) continue;
                
                int startLine = getLineNumber(content, valueSegment.getBegin());
                int startColumn = getColumnNumber(content, valueSegment.getBegin());
                int endLine = getLineNumber(content, valueSegment.getEnd());
                int endColumn = getColumnNumber(content, valueSegment.getEnd());
                
                Location location = new Location(
                    file.path(), startLine, startColumn, endLine, endColumn,
                    valueSegment.getBegin(), valueSegment.getEnd(),
                    jsCode, null
                );
                
                JSElement jsElement = JSElement.builder()
                    .type(JSElementType.STATEMENT)
                    .identifier("inline-event:" + attrName)
                    .location(location)
                    .rawContent(jsCode)
                    .build();
                
                elements.add(jsElement);
            }
        }
    }

    private void extractELExpressions(SourceFile file, String content, List<CodeElement> elements) {
        int index = 0;
        while ((index = content.indexOf("${", index)) != -1) {
            int endIndex = content.indexOf("}", index);
            if (endIndex == -1) break;
            
            String elContent = content.substring(index, endIndex + 1);
            int startLine = getLineNumber(content, index);
            int startColumn = getColumnNumber(content, index);
            int endLine = getLineNumber(content, endIndex + 1);
            int endColumn = getColumnNumber(content, endIndex + 1);
            
            Location location = new Location(
                file.path(), startLine, startColumn, endLine, endColumn,
                index, endIndex + 1, elContent, null
            );
            
            HTMLElement elElement = HTMLElement.builder()
                .tagName("${}")
                .tagNameLocation(location)
                .location(location)
                .rawContent(elContent)
                .elementType(HTMLElementType.EL_EXPRESSION)
                .build();
            
            elements.add(elElement);
            index = endIndex + 1;
        }
    }

    private void extractInlineScript(Element script, SourceFile file, String content,
                                     List<CodeElement> elements) {
        Segment contentSegment = script.getContent();
        if (contentSegment == null || contentSegment.length() == 0) return;

        String scriptContent = contentSegment.toString();
        if (scriptContent.trim().isEmpty()) return;

        int startLine = getLineNumber(content, contentSegment.getBegin());
        int startColumn = getColumnNumber(content, contentSegment.getBegin());
        
        Location inlineParent = new Location(
            file.path(), startLine, startColumn, startLine, startColumn,
            contentSegment.getBegin(), contentSegment.getEnd(),
            null, null
        );

        // Create JSElement for inline script
        JSElement jsElement = JSElement.builder()
            .type(JSElementType.STATEMENT)
            .identifier("inline-script")
            .location(inlineParent)
            .rawContent(scriptContent)
            .build();

        elements.add(jsElement);
    }

    private void extractInlineStyle(Element style, SourceFile file, String content,
                                    List<CodeElement> elements) {
        Segment contentSegment = style.getContent();
        if (contentSegment == null || contentSegment.length() == 0) return;

        String styleContent = contentSegment.toString();
        if (styleContent.trim().isEmpty()) return;

        // Clean HTML comments from inline CSS (<!-- --> is common in legacy JSP)
        String cleanedCss = cleanHtmlCommentsFromCss(styleContent);
        if (cleanedCss.trim().isEmpty()) return;

        int startLine = getLineNumber(content, contentSegment.getBegin());
        int startColumn = getColumnNumber(content, contentSegment.getBegin());

        Location inlineParent = new Location(
            file.path(), startLine, startColumn, startLine, startColumn,
            contentSegment.getBegin(), contentSegment.getEnd(),
            null, null
        );

        // Parse CSS content using ph-css
        List<CSSElement> parsedElements = parseInlineCss(cleanedCss, file, contentSegment.getBegin(), content);
        
        if (!parsedElements.isEmpty()) {
            // Add all parsed CSS elements
            elements.addAll(parsedElements);
        } else {
            // Fallback: create a simple CSSElement with raw content
            CSSElement cssElement = CSSElement.builder()
                .type(CSSElementType.RULE_SET)
                .location(inlineParent)
                .rawContent(styleContent)
                .build();
            elements.add(cssElement);
        }
    }

    /**
     * Remove HTML comments from CSS content.
     * Legacy JSP files often wrap CSS in <!-- --> to hide from old browsers.
     */
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

    /**
     * Parse inline CSS content using ph-css library.
     */
    private List<CSSElement> parseInlineCss(String cssContent, SourceFile file, int baseOffset, String fullContent) {
        List<CSSElement> elements = new ArrayList<>();
        
        try {
            CascadingStyleSheet css = CSSReader.readFromString(
                cssContent,
                ECSSVersion.CSS30,
                new DoNothingCSSParseErrorHandler()
            );
            
            if (css != null) {
                for (ICSSTopLevelRule rule : css.getAllRules()) {
                    if (rule instanceof CSSStyleRule styleRule) {
                        CSSElement element = parseInlineStyleRule(styleRule, file, baseOffset, fullContent, cssContent);
                        if (element != null) {
                            elements.add(element);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore CSS parse errors for inline styles
        }
        
        return elements;
    }

    /**
     * Parse a CSS style rule from inline content.
     */
    private CSSElement parseInlineStyleRule(CSSStyleRule styleRule, 
                                            SourceFile file, int baseOffset, 
                                            String fullContent, String cssContent) {
        // Use default writer settings instead of null
        com.helger.css.writer.CSSWriterSettings writerSettings = new com.helger.css.writer.CSSWriterSettings(ECSSVersion.CSS30);
        
        String selector = styleRule.getSelectorsAsCSSString(writerSettings, 0);
        String ruleContent = styleRule.getAsCSSString(writerSettings, 0);
        
        // Find rule position in CSS content
        int ruleStart = cssContent.indexOf(selector);
        if (ruleStart < 0) ruleStart = 0;
        int absoluteStart = baseOffset + ruleStart;
        int absoluteEnd = absoluteStart + ruleContent.length();
        
        Location location = createInlineLocation(file, fullContent, absoluteStart, absoluteEnd, ruleContent);
        Location selectorLocation = createInlineLocation(file, fullContent, absoluteStart, 
            absoluteStart + selector.length(), selector);

        // Parse declarations
        List<CSSElement> declarations = new ArrayList<>();
        for (CSSDeclaration declaration : styleRule.getAllDeclarations()) {
            String property = declaration.getProperty();
            String value = declaration.getExpressionAsCSSString();
            String declContent = property + ": " + value;
            
            // Find declaration position
            int declStart = cssContent.indexOf(property, ruleStart);
            if (declStart < 0) declStart = ruleStart;
            int absoluteDeclStart = baseOffset + declStart;
            int absoluteDeclEnd = absoluteDeclStart + declContent.length();
            
            Location declLocation = createInlineLocation(file, fullContent, absoluteDeclStart, absoluteDeclEnd, declContent);
            Location propertyLocation = createInlineLocation(file, fullContent, absoluteDeclStart, 
                absoluteDeclStart + property.length(), property);
            
            int valueStart = absoluteDeclStart + property.length() + 2; // ": "
            Location valueLocation = createInlineLocation(file, fullContent, valueStart,
                valueStart + value.length(), value);
            
            CSSElement declElement = CSSElement.builder()
                .type(CSSElementType.DECLARATION)
                .selector(selector)
                .property(property)
                .propertyLocation(propertyLocation)
                .value(value)
                .valueLocation(valueLocation)
                .location(declLocation)
                .rawContent(declContent)
                .build();
            
            declarations.add(declElement);
        }

        return CSSElement.builder()
            .type(CSSElementType.RULE_SET)
            .selector(selector)
            .selectorLocation(selectorLocation)
            .location(location)
            .rawContent(ruleContent)
            .children(declarations)
            .build();
    }

    private Location createInlineLocation(SourceFile file, String content, int start, int end, String snippet) {
        int startLine = getLineNumber(content, start);
        int startColumn = getColumnNumber(content, start);
        int endLine = getLineNumber(content, end);
        int endColumn = getColumnNumber(content, end);
        return new Location(file.path(), startLine, startColumn, endLine, endColumn, start, end, snippet, null);
    }

    private HTMLElementType determineElementType(Element element) {
        String tagName = element.getName().toLowerCase();
        
        // JSP directives: <%@ page/include/taglib %>
        if (tagName.startsWith("%@")) {
            return HTMLElementType.JSP_DIRECTIVE;
        }
        
        // JSP expression: <%= %>
        if (tagName.equals("%=")) {
            return HTMLElementType.JSP_EXPRESSION;
        }
        
        // JSP declaration: <%! %>
        if (tagName.equals("%!")) {
            return HTMLElementType.JSP_DECLARATION;
        }
        
        // JSP scriptlet: <% %>
        if (tagName.equals("%")) {
            return HTMLElementType.JSP_SCRIPTLET;
        }
        
        // JSP comment: <%-- --%>
        if (tagName.equals("%--")) {
            return HTMLElementType.JSP_COMMENT;
        }
        
        // JSP action tags: <jsp:xxx>
        if (tagName.startsWith("jsp:")) {
            return HTMLElementType.JSP_ACTION;
        }
        
        // Check for namespaced tags (contains colon)
        if (tagName.contains(":") && !tagName.startsWith("xml:")) {
            return determineTagLibraryType(tagName);
        }
        
        // Standard HTML types
        if (element.isEmpty()) {
            return HTMLElementType.SELF_CLOSING;
        }
        return HTMLElementType.OPEN_TAG;
    }
    
    /**
     * Determine the specific tag library type based on prefix
     */
    private HTMLElementType determineTagLibraryType(String tagName) {
        String prefix = tagName.substring(0, tagName.indexOf(':')).toLowerCase();
        
        return switch (prefix) {
            // JSTL tags
            case "c" -> HTMLElementType.JSTL_CORE;
            case "fmt" -> HTMLElementType.JSTL_FMT;
            case "sql" -> HTMLElementType.JSTL_SQL;
            case "x" -> HTMLElementType.JSTL_XML;
            case "fn" -> HTMLElementType.JSTL_FN;
            
            // Struts1 tags
            case "html" -> HTMLElementType.STRUTS_HTML;
            case "bean" -> HTMLElementType.STRUTS_BEAN;
            case "logic" -> HTMLElementType.STRUTS_LOGIC;
            case "nested" -> HTMLElementType.STRUTS_NESTED;
            case "tiles" -> HTMLElementType.STRUTS_TILES;
            
            // Struts2 tags
            case "s" -> HTMLElementType.STRUTS2_TAG;
            
            // Spring tags
            case "form" -> HTMLElementType.SPRING_FORM;
            case "spring" -> HTMLElementType.SPRING_TAG;
            
            // Display tag
            case "display" -> HTMLElementType.DISPLAY_TAG;
            
            // Other custom tags
            default -> HTMLElementType.CUSTOM_TAG;
        };
    }

    private int getLineNumber(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private int getColumnNumber(String content, int offset) {
        int column = 1;
        for (int i = offset - 1; i >= 0 && content.charAt(i) != '\n'; i--) {
            column++;
        }
        return column;
    }

    @Override
    public FileType getSupportedType() {
        return FileType.HTML;
    }

    @Override
    public Set<FileType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }
}
