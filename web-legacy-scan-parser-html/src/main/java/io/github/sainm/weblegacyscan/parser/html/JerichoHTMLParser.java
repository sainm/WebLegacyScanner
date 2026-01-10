package io.github.sainm.weblegacyscan.parser.html;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;
import net.htmlparser.jericho.*;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * HTML Parser based on Jericho-HTML library.
 */
public class JerichoHTMLParser implements Parser {

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
        // Extract inline scripts
        for (Element script : source.getAllElements(HTMLElementName.SCRIPT)) {
            if (script.getAttributeValue("src") == null) {
                extractInlineScript(script, file, content, elements);
            }
        }

        // Extract inline styles
        for (Element style : source.getAllElements(HTMLElementName.STYLE)) {
            extractInlineStyle(style, file, content, elements);
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

        int startLine = getLineNumber(content, contentSegment.getBegin());
        int startColumn = getColumnNumber(content, contentSegment.getBegin());

        Location inlineParent = new Location(
            file.path(), startLine, startColumn, startLine, startColumn,
            contentSegment.getBegin(), contentSegment.getEnd(),
            null, null
        );

        CSSElement cssElement = CSSElement.builder()
            .type(CSSElementType.RULE_SET)
            .location(inlineParent)
            .rawContent(styleContent)
            .build();

        elements.add(cssElement);
    }

    private HTMLElementType determineElementType(Element element) {
        if (element.isEmpty()) {
            return HTMLElementType.SELF_CLOSING;
        }
        return HTMLElementType.OPEN_TAG;
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
}
