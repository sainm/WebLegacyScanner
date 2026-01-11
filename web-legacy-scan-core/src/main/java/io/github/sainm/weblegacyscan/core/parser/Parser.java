package io.github.sainm.weblegacyscan.core.parser;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;

import java.util.Set;

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
     * Get primary supported file type
     */
    FileType getSupportedType();

    /**
     * Get all supported file types (default returns only primary type)
     */
    default Set<FileType> getSupportedTypes() {
        return Set.of(getSupportedType());
    }
}
