package io.github.sainm.weblegacyscan.core.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Precise location information for code elements
 */
public record Location(
    Path filePath,
    int startLine,
    int startColumn,
    int endLine,
    int endColumn,
    int startOffset,
    int endOffset,
    String sourceSnippet,
    Location inlineParent
) {
    public Location {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        if (startLine < 1) throw new IllegalArgumentException("startLine must be >= 1");
        if (startColumn < 1) throw new IllegalArgumentException("startColumn must be >= 1");
        if (endLine < startLine) throw new IllegalArgumentException("endLine must be >= startLine");
        if (endLine == startLine && endColumn < startColumn) {
            throw new IllegalArgumentException("endColumn must be >= startColumn when on same line");
        }
    }

    public static Location of(Path filePath, int line, int column) {
        return new Location(filePath, line, column, line, column, -1, -1, null, null);
    }

    public static Location of(Path filePath, int startLine, int startColumn, int endLine, int endColumn) {
        return new Location(filePath, startLine, startColumn, endLine, endColumn, -1, -1, null, null);
    }

    public Location withSnippet(String snippet) {
        return new Location(filePath, startLine, startColumn, endLine, endColumn, 
                           startOffset, endOffset, snippet, inlineParent);
    }

    public Location withInlineParent(Location parent) {
        return new Location(filePath, startLine, startColumn, endLine, endColumn,
                           startOffset, endOffset, sourceSnippet, parent);
    }

    public Location toAbsolute() {
        if (inlineParent == null) return this;
        
        int absStartLine = inlineParent.startLine() + startLine - 1;
        int absEndLine = inlineParent.startLine() + endLine - 1;
        int absStartColumn = (startLine == 1) 
            ? inlineParent.startColumn() + startColumn - 1 
            : startColumn;
        int absEndColumn = (endLine == 1) 
            ? inlineParent.startColumn() + endColumn - 1 
            : endColumn;
        int absStartOffset = (inlineParent.startOffset() >= 0 && startOffset >= 0)
            ? inlineParent.startOffset() + startOffset
            : -1;
        int absEndOffset = (inlineParent.startOffset() >= 0 && endOffset >= 0)
            ? inlineParent.startOffset() + endOffset
            : -1;

        return new Location(
            inlineParent.filePath(),
            absStartLine,
            absStartColumn,
            absEndLine,
            absEndColumn,
            absStartOffset,
            absEndOffset,
            sourceSnippet,
            null
        );
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d", filePath.getFileName(), startLine, startColumn);
    }
}
