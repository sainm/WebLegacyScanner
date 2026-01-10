package io.github.sainm.weblegacyscan.core.parser;

import java.nio.file.Path;

/**
 * 解析错误
 */
public record ParseError(
    Path filePath,
    int line,
    int column,
    String message,
    ErrorSeverity severity
) {
    public enum ErrorSeverity {
        FATAL,
        RECOVERABLE
    }

    public static ParseError fatal(Path filePath, int line, int column, String message) {
        return new ParseError(filePath, line, column, message, ErrorSeverity.FATAL);
    }

    public static ParseError recoverable(Path filePath, int line, int column, String message) {
        return new ParseError(filePath, line, column, message, ErrorSeverity.RECOVERABLE);
    }

    public boolean isFatal() {
        return severity == ErrorSeverity.FATAL;
    }
}
