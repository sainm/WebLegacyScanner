package io.github.sainm.weblegacyscan.parser.js;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;
import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.parsing.Config;
import com.google.javascript.jscomp.parsing.ParserRunner;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * JavaScript parser based on Closure Compiler
 */
public class ClosureJSParser implements Parser {

    @Override
    public ParseResult parse(io.github.sainm.weblegacyscan.core.file.SourceFile file) {
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
    public ParseResult parseContent(String content, io.github.sainm.weblegacyscan.core.file.SourceFile file) {
        Instant start = Instant.now();
        List<CodeElement> elements = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        try {
            Compiler compiler = new Compiler();
            CompilerOptions options = new CompilerOptions();
            options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_NEXT);
            options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT_NEXT);
            // Skip all checks to just parse
            options.setSkipNonTranspilationPasses(true);

            com.google.javascript.jscomp.SourceFile jsSourceFile = 
                com.google.javascript.jscomp.SourceFile.fromCode(
                    file.path().toString(), 
                    content
                );

            // Use compile instead of parse - parse() returns void in newer versions
            Result result = compiler.compile(
                List.of(),
                List.of(jsSourceFile),
                options
            );
            
            // Collect parse errors
            for (JSError error : compiler.getErrors()) {
                errors.add(ParseError.recoverable(
                    file.path(),
                    error.getLineNumber(),
                    error.getCharno(),
                    error.getDescription()
                ));
            }

            // Get the AST root
            Node root = compiler.getRoot();
            if (root != null) {
                // Traverse AST
                traverseNode(root, file, content, elements);
            }

        } catch (Exception e) {
            errors.add(ParseError.recoverable(file.path(), 1, 1,
                "JavaScript parsing error: " + e.getMessage()));
        }

        Duration parseTime = Duration.between(start, Instant.now());
        ParseStatistics stats = ParseStatistics.builder()
            .totalElements(elements.size())
            .jsElements(elements.size())
            .parseTime(parseTime)
            .build();

        return ParseResult.partial(elements, errors, stats);
    }

    private void traverseNode(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                              String content, List<CodeElement> elements) {
        if (node == null) return;

        JSElement element = createJSElement(node, file, content);
        if (element != null) {
            elements.add(element);
        }

        // Recursively traverse child nodes
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            traverseNode(child, file, content, elements);
        }
    }

    private JSElement createJSElement(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                      String content) {
        Token token = node.getToken();
        
        return switch (token) {
            case CALL -> createCallElement(node, file, content);
            case VAR, LET, CONST -> createVariableDeclaration(node, file, content);
            case FUNCTION -> createFunctionElement(node, file, content);
            case ASSIGN -> createAssignmentElement(node, file, content);
            case GETPROP -> createPropertyAccessElement(node, file, content);
            default -> null;
        };
    }

    private JSElement createCallElement(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                        String content) {
        Node callee = node.getFirstChild();
        if (callee == null) return null;

        String identifier = extractIdentifier(callee);
        Location location = createLocation(node, file, content);
        Location identifierLocation = createLocation(callee, file, content);

        List<JSArgument> arguments = new ArrayList<>();
        for (Node arg = callee.getNext(); arg != null; arg = arg.getNext()) {
            arguments.add(createArgument(arg, file, content));
        }

        JSElementType type = callee.getToken() == Token.GETPROP 
            ? JSElementType.METHOD_CALL 
            : JSElementType.FUNCTION_CALL;

        return JSElement.builder()
            .type(type)
            .identifier(identifier)
            .identifierLocation(identifierLocation)
            .arguments(arguments)
            .location(location)
            .rawContent(extractSource(node, content))
            .build();
    }

    private JSElement createVariableDeclaration(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                                String content) {
        String keyword = switch (node.getToken()) {
            case VAR -> "var";
            case LET -> "let";
            case CONST -> "const";
            default -> "var";
        };

        Node nameNode = node.getFirstChild();
        String identifier = nameNode != null ? nameNode.getString() : "";
        
        Location location = createLocation(node, file, content);
        Location identifierLocation = nameNode != null 
            ? createLocation(nameNode, file, content) 
            : location;

        return JSElement.builder()
            .type(JSElementType.VARIABLE_DECLARATION)
            .identifier(keyword + " " + identifier)
            .identifierLocation(identifierLocation)
            .location(location)
            .rawContent(extractSource(node, content))
            .build();
    }

    private JSElement createFunctionElement(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                            String content) {
        Node nameNode = node.getFirstChild();
        String identifier = nameNode != null && nameNode.isName() ? nameNode.getString() : "anonymous";

        Location location = createLocation(node, file, content);
        Location identifierLocation = nameNode != null 
            ? createLocation(nameNode, file, content) 
            : location;

        return JSElement.builder()
            .type(JSElementType.FUNCTION_CALL)
            .identifier("function " + identifier)
            .identifierLocation(identifierLocation)
            .location(location)
            .rawContent(extractSource(node, content))
            .build();
    }

    private JSElement createAssignmentElement(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                              String content) {
        Node left = node.getFirstChild();
        String identifier = extractIdentifier(left);

        Location location = createLocation(node, file, content);
        Location identifierLocation = left != null 
            ? createLocation(left, file, content) 
            : location;

        return JSElement.builder()
            .type(JSElementType.ASSIGNMENT)
            .identifier(identifier)
            .identifierLocation(identifierLocation)
            .location(location)
            .rawContent(extractSource(node, content))
            .build();
    }

    private JSElement createPropertyAccessElement(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                                  String content) {
        String identifier = extractIdentifier(node);

        Location location = createLocation(node, file, content);

        return JSElement.builder()
            .type(JSElementType.PROPERTY_ACCESS)
            .identifier(identifier)
            .identifierLocation(location)
            .location(location)
            .rawContent(extractSource(node, content))
            .build();
    }

    private JSArgument createArgument(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                      String content) {
        String value = extractSource(node, content);
        Location location = createLocation(node, file, content);
        JSArgumentType type = determineArgumentType(node);

        return new JSArgument(value, location, type);
    }

    private JSArgumentType determineArgumentType(Node node) {
        return switch (node.getToken()) {
            case STRINGLIT -> JSArgumentType.STRING;
            case NUMBER -> JSArgumentType.NUMBER;
            case NAME -> JSArgumentType.IDENTIFIER;
            case OBJECTLIT -> JSArgumentType.OBJECT;
            case ARRAYLIT -> JSArgumentType.ARRAY;
            default -> JSArgumentType.EXPRESSION;
        };
    }

    private String extractIdentifier(Node node) {
        if (node == null) return "";
        
        return switch (node.getToken()) {
            case NAME -> node.getString();
            case GETPROP -> {
                Node obj = node.getFirstChild();
                Node prop = node.getLastChild();
                yield extractIdentifier(obj) + "." + (prop != null ? prop.getString() : "");
            }
            case THIS -> "this";
            default -> node.toString();
        };
    }

    private Location createLocation(Node node, io.github.sainm.weblegacyscan.core.file.SourceFile file, 
                                    String content) {
        int line = node.getLineno();
        int column = node.getCharno() + 1;
        int length = node.getLength();
        
        int startOffset = getOffset(content, line, column);
        int endOffset = startOffset + length;
        
        int endLine = getLineNumber(content, endOffset);
        int endColumn = getColumnNumber(content, endOffset);

        String snippet = extractSource(node, content);

        return new Location(file.path(), line, column, endLine, endColumn,
            startOffset, endOffset, snippet, null);
    }

    private String extractSource(Node node, String content) {
        int line = node.getLineno();
        int column = node.getCharno();
        int length = node.getLength();
        
        int startOffset = getOffset(content, line, column + 1);
        int endOffset = Math.min(startOffset + length, content.length());
        
        if (startOffset >= 0 && endOffset > startOffset && endOffset <= content.length()) {
            return content.substring(startOffset, endOffset);
        }
        return "";
    }

    private int getOffset(String content, int line, int column) {
        int offset = 0;
        int currentLine = 1;
        
        while (currentLine < line && offset < content.length()) {
            if (content.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }
        
        return offset + column - 1;
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
        return FileType.JAVASCRIPT;
    }
}
