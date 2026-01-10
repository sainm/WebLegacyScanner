package io.github.sainm.weblegacyscan.parser.css;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.errorhandler.LoggingCSSParseErrorHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * CSS Parser based on ph-css library.
 */
public class PhCSSParser implements Parser {

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
            CascadingStyleSheet css = CSSReader.readFromString(
                content, 
                ECSSVersion.CSS30,
                new LoggingCSSParseErrorHandler()
            );

            if (css != null) {
                // Parse all rules
                for (ICSSTopLevelRule rule : css.getAllRules()) {
                    try {
                        CodeElement element = parseRule(rule, file, content);
                        if (element != null) {
                            elements.add(element);
                        }
                    } catch (Exception e) {
                        errors.add(ParseError.recoverable(file.path(), 1, 1,
                            "Failed to parse rule: " + e.getMessage()));
                    }
                }
            } else {
                errors.add(ParseError.recoverable(file.path(), 1, 1, "Failed to parse CSS"));
            }

        } catch (Exception e) {
            errors.add(ParseError.recoverable(file.path(), 1, 1,
                "CSS parsing error: " + e.getMessage()));
        }

        Duration parseTime = Duration.between(start, Instant.now());
        ParseStatistics stats = ParseStatistics.builder()
            .totalElements(elements.size())
            .cssElements(elements.size())
            .parseTime(parseTime)
            .build();

        return ParseResult.partial(elements, errors, stats);
    }


    private CodeElement parseRule(ICSSTopLevelRule rule, SourceFile file, String content) {
        if (rule instanceof CSSStyleRule styleRule) {
            return parseStyleRule(styleRule, file, content);
        } else if (rule instanceof CSSMediaRule mediaRule) {
            return parseMediaRule(mediaRule, file, content);
        } else if (rule instanceof CSSKeyframesRule keyframesRule) {
            return parseKeyframesRule(keyframesRule, file, content);
        } else if (rule instanceof CSSFontFaceRule fontFaceRule) {
            return parseFontFaceRule(fontFaceRule, file, content);
        }
        return null;
    }

    private CSSElement parseStyleRule(CSSStyleRule styleRule, SourceFile file, String content) {
        String selector = styleRule.getSelectorsAsCSSString(null, 0);
        String ruleContent = styleRule.getAsCSSString(null, 0);
        
        // Find rule position in content
        int ruleStart = findRulePosition(content, selector);
        int ruleEnd = ruleStart + ruleContent.length();

        Location location = createLocation(file, content, ruleStart, ruleEnd, ruleContent);
        Location selectorLocation = createLocation(file, content, ruleStart, 
            ruleStart + selector.length(), selector);

        // Parse declarations
        List<CSSElement> declarations = new ArrayList<>();
        for (CSSDeclaration declaration : styleRule.getAllDeclarations()) {
            CSSElement declElement = parseDeclaration(declaration, file, content);
            if (declElement != null) {
                declarations.add(declElement);
            }
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

    private CSSElement parseDeclaration(CSSDeclaration declaration, SourceFile file, String content) {
        String property = declaration.getProperty();
        String value = declaration.getExpressionAsCSSString();
        String declContent = property + ": " + value;

        // Simplified position calculation
        int declStart = content.indexOf(property);
        if (declStart < 0) declStart = 0;
        int declEnd = declStart + declContent.length();

        Location location = createLocation(file, content, declStart, declEnd, declContent);
        Location propertyLocation = createLocation(file, content, declStart, 
            declStart + property.length(), property);
        
        int valueStart = declStart + property.length() + 2; // ": "
        Location valueLocation = createLocation(file, content, valueStart,
            valueStart + value.length(), value);

        return CSSElement.builder()
            .type(CSSElementType.DECLARATION)
            .property(property)
            .propertyLocation(propertyLocation)
            .value(value)
            .valueLocation(valueLocation)
            .location(location)
            .rawContent(declContent)
            .build();
    }

    private CSSElement parseMediaRule(CSSMediaRule mediaRule, SourceFile file, String content) {
        String ruleContent = mediaRule.getAsCSSString(null, 0);
        String mediaQuery = mediaRule.getAllMediaQueries().toString();

        int ruleStart = content.indexOf("@media");
        if (ruleStart < 0) ruleStart = 0;
        int ruleEnd = ruleStart + ruleContent.length();

        Location location = createLocation(file, content, ruleStart, ruleEnd, ruleContent);

        List<CSSElement> children = new ArrayList<>();
        for (CSSStyleRule styleRule : mediaRule.getAllStyleRules()) {
            CSSElement child = parseStyleRule(styleRule, file, content);
            if (child != null) {
                children.add(child);
            }
        }

        return CSSElement.builder()
            .type(CSSElementType.MEDIA_QUERY)
            .selector(mediaQuery)
            .location(location)
            .rawContent(ruleContent)
            .children(children)
            .build();
    }

    private CSSElement parseKeyframesRule(CSSKeyframesRule keyframesRule, SourceFile file, String content) {
        String ruleContent = keyframesRule.getAsCSSString(null, 0);
        String animationName = keyframesRule.getAnimationName();

        int ruleStart = content.indexOf("@keyframes");
        if (ruleStart < 0) ruleStart = 0;
        int ruleEnd = ruleStart + ruleContent.length();

        Location location = createLocation(file, content, ruleStart, ruleEnd, ruleContent);

        return CSSElement.builder()
            .type(CSSElementType.KEYFRAME)
            .selector(animationName)
            .location(location)
            .rawContent(ruleContent)
            .build();
    }


    private CSSElement parseFontFaceRule(CSSFontFaceRule fontFaceRule, SourceFile file, String content) {
        String ruleContent = fontFaceRule.getAsCSSString(null, 0);

        int ruleStart = content.indexOf("@font-face");
        if (ruleStart < 0) ruleStart = 0;
        int ruleEnd = ruleStart + ruleContent.length();

        Location location = createLocation(file, content, ruleStart, ruleEnd, ruleContent);

        List<CSSElement> declarations = new ArrayList<>();
        for (CSSDeclaration declaration : fontFaceRule.getAllDeclarations()) {
            CSSElement declElement = parseDeclaration(declaration, file, content);
            if (declElement != null) {
                declarations.add(declElement);
            }
        }

        return CSSElement.builder()
            .type(CSSElementType.AT_RULE)
            .selector("@font-face")
            .location(location)
            .rawContent(ruleContent)
            .children(declarations)
            .build();
    }

    private int findRulePosition(String content, String selector) {
        int pos = content.indexOf(selector);
        return pos >= 0 ? pos : 0;
    }

    private Location createLocation(SourceFile file, String content, int start, int end, String snippet) {
        int startLine = getLineNumber(content, start);
        int startColumn = getColumnNumber(content, start);
        int endLine = getLineNumber(content, end);
        int endColumn = getColumnNumber(content, end);

        return new Location(file.path(), startLine, startColumn, endLine, endColumn,
            start, end, snippet, null);
    }

    private int getLineNumber(String content, int offset) {
        if (offset < 0 || offset > content.length()) return 1;
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private int getColumnNumber(String content, int offset) {
        if (offset < 0 || offset > content.length()) return 1;
        int column = 1;
        for (int i = offset - 1; i >= 0 && content.charAt(i) != '\n'; i--) {
            column++;
        }
        return column;
    }

    @Override
    public FileType getSupportedType() {
        return FileType.CSS;
    }
}
