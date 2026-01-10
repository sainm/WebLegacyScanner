package io.github.sainm.weblegacyscan.core.parser;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;

/**
 * Parser interface
 */
public interface Parser {
    
    /**
     * Parse source file
     * @param file source file
     * @return parse result
     */
    ParseResult parse(SourceFile file);

    /**
     * Parse string content
     * @param content content
     * @param file source file (for location information)
     * @return parse result
     */
    ParseResult parseContent(String content, SourceFile file);

    /**
     * Get supported file type
     */
    FileType getSupportedType();
}
