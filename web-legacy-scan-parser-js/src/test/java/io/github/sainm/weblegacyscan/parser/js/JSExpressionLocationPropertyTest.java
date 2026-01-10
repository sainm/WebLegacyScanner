package io.github.sainm.weblegacyscan.parser.js;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.ParseResult;
import net.jqwik.api.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Property-based tests for JS Parser Expression Location Tracking.
 * 
 * Property 24: JS Expression Location Tracking
 * For any JSElement representing a function or method call, the element SHALL have valid 
 * identifierLocation and each JSArgument SHALL have a valid location.
 * 
 * Validates: Requirements 13.4
 */
class JSExpressionLocationPropertyTest {

    private static final Path TEST_FILE = Path.of("test.js");
    private final ClosureJSParser parser = new ClosureJSParser();

    /**
     * Property 24: JS Expression Location Tracking - Function calls have valid identifier locations
     * 
     * For any JSElement representing a function call parsed by ClosureJSParser,
     * the element SHALL have a valid identifierLocation with startLine >= 1 and startColumn >= 1.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void functionCallsMustHaveValidIdentifierLocation(
            @ForAll("jsWithFunctionCalls") String jsContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.JAVASCRIPT, StandardCharsets.UTF_8, jsContent.length());
        ParseResult result = parser.parseContent(jsContent, sourceFile);
        
        List<JSElement> calls = collectFunctionCalls(result.elements());
        
        for (JSElement call : calls) {
            assert call.identifierLocation() != null : 
                "Function call identifierLocation must not be null for: " + call.identifier();
            assert call.identifierLocation().startLine() >= 1 : 
                "Function call identifierLocation startLine must be >= 1, got: " + call.identifierLocation().startLine();
            assert call.identifierLocation().startColumn() >= 1 : 
                "Function call identifierLocation startColumn must be >= 1, got: " + call.identifierLocation().startColumn();
        }
    }

    /**
     * Property 24: JS Expression Location Tracking - Method calls have valid identifier locations
     * 
     * For any JSElement representing a method call parsed by ClosureJSParser,
     * the element SHALL have a valid identifierLocation with startLine >= 1 and startColumn >= 1.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void methodCallsMustHaveValidIdentifierLocation(
            @ForAll("jsWithMethodCalls") String jsContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.JAVASCRIPT, StandardCharsets.UTF_8, jsContent.length());
        ParseResult result = parser.parseContent(jsContent, sourceFile);
        
        List<JSElement> calls = collectMethodCalls(result.elements());
        
        for (JSElement call : calls) {
            assert call.identifierLocation() != null : 
                "Method call identifierLocation must not be null for: " + call.identifier();
            assert call.identifierLocation().startLine() >= 1 : 
                "Method call identifierLocation startLine must be >= 1, got: " + call.identifierLocation().startLine();
            assert call.identifierLocation().startColumn() >= 1 : 
                "Method call identifierLocation startColumn must be >= 1, got: " + call.identifierLocation().startColumn();
        }
    }


    /**
     * Property 24: JS Expression Location Tracking - Arguments have valid locations
     * 
     * For any JSElement representing a function or method call with arguments,
     * each JSArgument SHALL have a valid location with startLine >= 1 and startColumn >= 1.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void argumentsMustHaveValidLocations(
            @ForAll("jsWithFunctionCallsWithArgs") String jsContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.JAVASCRIPT, StandardCharsets.UTF_8, jsContent.length());
        ParseResult result = parser.parseContent(jsContent, sourceFile);
        
        List<JSElement> calls = collectAllCalls(result.elements());
        
        for (JSElement call : calls) {
            for (JSArgument arg : call.arguments()) {
                assert arg.location() != null : 
                    "Argument location must not be null for argument: " + arg.value();
                assert arg.location().startLine() >= 1 : 
                    "Argument location startLine must be >= 1, got: " + arg.location().startLine();
                assert arg.location().startColumn() >= 1 : 
                    "Argument location startColumn must be >= 1, got: " + arg.location().startColumn();
            }
        }
    }

    /**
     * Property 24: JS Expression Location Tracking - Full expression locations are valid
     * 
     * For any JSElement representing a function or method call,
     * the element SHALL have a valid full location with proper line/column bounds.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void callExpressionsMustHaveValidFullLocation(
            @ForAll("jsWithFunctionCalls") String jsContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.JAVASCRIPT, StandardCharsets.UTF_8, jsContent.length());
        ParseResult result = parser.parseContent(jsContent, sourceFile);
        
        List<JSElement> calls = collectAllCalls(result.elements());
        
        for (JSElement call : calls) {
            assert call.location() != null : 
                "Call expression location must not be null for: " + call.identifier();
            assert call.location().startLine() >= 1 : 
                "Call expression location startLine must be >= 1, got: " + call.location().startLine();
            assert call.location().startColumn() >= 1 : 
                "Call expression location startColumn must be >= 1, got: " + call.location().startColumn();
            assert call.location().endLine() >= call.location().startLine() :
                "Call expression location endLine must be >= startLine";
        }
    }

    /**
     * Property 24: JS Expression Location Tracking - Identifier location offsets are valid
     * 
     * For any JSElement representing a function or method call,
     * the identifierLocation offsets SHALL be within the bounds of the JS content.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void identifierLocationOffsetsAreValid(
            @ForAll("jsWithFunctionCalls") String jsContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.JAVASCRIPT, StandardCharsets.UTF_8, jsContent.length());
        ParseResult result = parser.parseContent(jsContent, sourceFile);
        
        List<JSElement> calls = collectAllCalls(result.elements());
        
        for (JSElement call : calls) {
            Location idLoc = call.identifierLocation();
            if (idLoc != null && idLoc.startOffset() >= 0 && idLoc.endOffset() >= 0) {
                assert idLoc.endOffset() >= idLoc.startOffset() :
                    "identifierLocation endOffset must be >= startOffset";
                assert idLoc.startOffset() < jsContent.length() :
                    "identifierLocation startOffset must be within content bounds";
            }
        }
    }

    /**
     * Property 24: JS Expression Location Tracking - Argument location offsets are valid
     * 
     * For any JSArgument in a function or method call,
     * the location offsets SHALL be within the bounds of the JS content.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void argumentLocationOffsetsAreValid(
            @ForAll("jsWithFunctionCallsWithArgs") String jsContent
    ) {
        SourceFile sourceFile = new SourceFile(TEST_FILE, FileType.JAVASCRIPT, StandardCharsets.UTF_8, jsContent.length());
        ParseResult result = parser.parseContent(jsContent, sourceFile);
        
        List<JSElement> calls = collectAllCalls(result.elements());
        
        for (JSElement call : calls) {
            for (JSArgument arg : call.arguments()) {
                Location argLoc = arg.location();
                if (argLoc.startOffset() >= 0 && argLoc.endOffset() >= 0) {
                    assert argLoc.endOffset() >= argLoc.startOffset() :
                        "argument location endOffset must be >= startOffset";
                    assert argLoc.startOffset() < jsContent.length() :
                        "argument location startOffset must be within content bounds";
                }
            }
        }
    }

    // ========== Helper Methods ==========

    private List<JSElement> collectFunctionCalls(List<CodeElement> elements) {
        return elements.stream()
            .filter(e -> e instanceof JSElement)
            .map(e -> (JSElement) e)
            .filter(js -> js.type() == JSElementType.FUNCTION_CALL)
            .toList();
    }

    private List<JSElement> collectMethodCalls(List<CodeElement> elements) {
        return elements.stream()
            .filter(e -> e instanceof JSElement)
            .map(e -> (JSElement) e)
            .filter(js -> js.type() == JSElementType.METHOD_CALL)
            .toList();
    }

    private List<JSElement> collectAllCalls(List<CodeElement> elements) {
        return elements.stream()
            .filter(e -> e instanceof JSElement)
            .map(e -> (JSElement) e)
            .filter(js -> js.type() == JSElementType.FUNCTION_CALL || js.type() == JSElementType.METHOD_CALL)
            .toList();
    }


    // ========== Providers ==========

    @Provide
    Arbitrary<String> jsWithFunctionCalls() {
        return Combinators.combine(
            functionName(),
            argumentList()
        ).as((name, args) -> name + "(" + args + ");");
    }

    @Provide
    Arbitrary<String> jsWithMethodCalls() {
        return Combinators.combine(
            objectName(),
            methodName(),
            argumentList()
        ).as((obj, method, args) -> obj + "." + method + "(" + args + ");");
    }

    @Provide
    Arbitrary<String> jsWithFunctionCallsWithArgs() {
        return Combinators.combine(
            functionName(),
            nonEmptyArgumentList()
        ).as((name, args) -> name + "(" + args + ");");
    }

    private Arbitrary<String> functionName() {
        return Arbitraries.of(
            "alert", "console", "parseInt", "parseFloat", "setTimeout",
            "setInterval", "clearTimeout", "clearInterval", "fetch",
            "myFunction", "doSomething", "processData", "handleEvent"
        );
    }

    private Arbitrary<String> objectName() {
        return Arbitraries.of(
            "console", "document", "window", "Math", "JSON",
            "Array", "Object", "String", "myObj", "data"
        );
    }

    private Arbitrary<String> methodName() {
        return Arbitraries.of(
            "log", "warn", "error", "getElementById", "querySelector",
            "addEventListener", "stringify", "parse", "floor", "ceil",
            "random", "toString", "valueOf", "push", "pop"
        );
    }

    private Arbitrary<String> argumentValue() {
        return Arbitraries.oneOf(
            stringLiteral(),
            numberLiteral(),
            identifier()
        );
    }

    private Arbitrary<String> stringLiteral() {
        return Arbitraries.of(
            "\"hello\"", "\"world\"", "\"test\"", "\"value\"",
            "'single'", "'quoted'", "'string'"
        );
    }

    private Arbitrary<String> numberLiteral() {
        return Arbitraries.integers().between(0, 1000).map(String::valueOf);
    }

    private Arbitrary<String> identifier() {
        return Arbitraries.of(
            "x", "y", "value", "data", "result", "item", "element"
        );
    }

    private Arbitrary<String> argumentList() {
        return argumentValue()
            .list()
            .ofMinSize(0)
            .ofMaxSize(3)
            .map(args -> String.join(", ", args));
    }

    private Arbitrary<String> nonEmptyArgumentList() {
        return argumentValue()
            .list()
            .ofMinSize(1)
            .ofMaxSize(3)
            .map(args -> String.join(", ", args));
    }
}
