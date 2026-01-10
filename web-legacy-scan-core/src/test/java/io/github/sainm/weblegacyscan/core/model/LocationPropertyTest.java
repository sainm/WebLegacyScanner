package io.github.sainm.weblegacyscan.core.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;

/**
 * Property-based tests for Location completeness.
 * 
 * Property 21: Code Element Location Completeness
 * For any CodeElement extracted by the Parser, the element SHALL have a non-null Location 
 * with valid startLine >= 1, startColumn >= 1, endLine >= startLine, 
 * and (endLine > startLine OR endColumn >= startColumn).
 * 
 * Validates: Requirements 13.1
 */
class LocationPropertyTest {

    /**
     * Property 21: Code Element Location Completeness
     * 
     * For any valid Location, the following invariants must hold:
     * - startLine >= 1
     * - startColumn >= 1
     * - endLine >= startLine
     * - (endLine > startLine) OR (endColumn >= startColumn)
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void locationInvariantsHold(
            @ForAll @IntRange(min = 1, max = 10000) int startLine,
            @ForAll @IntRange(min = 1, max = 1000) int startColumn,
            @ForAll @IntRange(min = 0, max = 100) int lineOffset,
            @ForAll @IntRange(min = 0, max = 1000) int columnOffset
    ) {
        Path filePath = Path.of("test.html");
        int endLine = startLine + lineOffset;
        int endColumn = (lineOffset == 0) 
            ? startColumn + columnOffset  // Same line: endColumn must be >= startColumn
            : 1 + columnOffset;           // Different line: endColumn can be any valid value
        
        Location location = Location.of(filePath, startLine, startColumn, endLine, endColumn);
        
        // Verify all invariants
        assert location.filePath() != null : "filePath must not be null";
        assert location.startLine() >= 1 : "startLine must be >= 1";
        assert location.startColumn() >= 1 : "startColumn must be >= 1";
        assert location.endLine() >= location.startLine() : "endLine must be >= startLine";
        assert location.endLine() > location.startLine() || location.endColumn() >= location.startColumn() 
            : "endColumn must be >= startColumn when on same line";
    }

    /**
     * Property: Location rejects invalid startLine
     * 
     * For any startLine < 1, Location construction SHALL throw IllegalArgumentException.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void locationRejectsInvalidStartLine(
            @ForAll @IntRange(min = -1000, max = 0) int invalidStartLine
    ) {
        Path filePath = Path.of("test.html");
        
        try {
            Location.of(filePath, invalidStartLine, 1, invalidStartLine, 1);
            throw new AssertionError("Should have thrown IllegalArgumentException for startLine: " + invalidStartLine);
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    /**
     * Property: Location rejects invalid startColumn
     * 
     * For any startColumn < 1, Location construction SHALL throw IllegalArgumentException.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void locationRejectsInvalidStartColumn(
            @ForAll @IntRange(min = -1000, max = 0) int invalidStartColumn
    ) {
        Path filePath = Path.of("test.html");
        
        try {
            Location.of(filePath, 1, invalidStartColumn, 1, invalidStartColumn);
            throw new AssertionError("Should have thrown IllegalArgumentException for startColumn: " + invalidStartColumn);
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    /**
     * Property: Location rejects endLine < startLine
     * 
     * For any endLine < startLine, Location construction SHALL throw IllegalArgumentException.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void locationRejectsEndLineBeforeStartLine(
            @ForAll @IntRange(min = 2, max = 1000) int startLine,
            @ForAll @IntRange(min = 1, max = 100) int lineDiff
    ) {
        Path filePath = Path.of("test.html");
        int endLine = startLine - lineDiff; // endLine < startLine
        
        try {
            Location.of(filePath, startLine, 1, endLine, 1);
            throw new AssertionError("Should have thrown IllegalArgumentException for endLine < startLine");
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    /**
     * Property: Location rejects endColumn < startColumn on same line
     * 
     * When endLine == startLine and endColumn < startColumn, 
     * Location construction SHALL throw IllegalArgumentException.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void locationRejectsEndColumnBeforeStartColumnOnSameLine(
            @ForAll @IntRange(min = 1, max = 1000) int line,
            @ForAll @IntRange(min = 2, max = 1000) int startColumn,
            @ForAll @IntRange(min = 1, max = 100) int columnDiff
    ) {
        Path filePath = Path.of("test.html");
        int endColumn = startColumn - columnDiff; // endColumn < startColumn
        
        try {
            Location.of(filePath, line, startColumn, line, endColumn);
            throw new AssertionError("Should have thrown IllegalArgumentException for endColumn < startColumn on same line");
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    /**
     * Property: Location allows endColumn < startColumn on different lines
     * 
     * When endLine > startLine, endColumn can be any valid value (>= 1),
     * even if it's less than startColumn.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void locationAllowsAnyEndColumnOnDifferentLines(
            @ForAll @IntRange(min = 1, max = 1000) int startLine,
            @ForAll @IntRange(min = 1, max = 1000) int startColumn,
            @ForAll @IntRange(min = 1, max = 100) int lineDiff,
            @ForAll @IntRange(min = 1, max = 1000) int endColumn
    ) {
        Path filePath = Path.of("test.html");
        int endLine = startLine + lineDiff; // endLine > startLine
        
        // This should NOT throw, even if endColumn < startColumn
        Location location = Location.of(filePath, startLine, startColumn, endLine, endColumn);
        
        assert location.endLine() > location.startLine() : "endLine should be > startLine";
        assert location.endColumn() >= 1 : "endColumn should be >= 1";
    }

    /**
     * Property: Location.of convenience method produces valid locations
     * 
     * The single-point Location.of(path, line, column) method SHALL produce
     * a valid Location where startLine == endLine and startColumn == endColumn.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 100)
    void singlePointLocationIsValid(
            @ForAll @IntRange(min = 1, max = 10000) int line,
            @ForAll @IntRange(min = 1, max = 1000) int column
    ) {
        Path filePath = Path.of("test.html");
        
        Location location = Location.of(filePath, line, column);
        
        assert location.filePath() != null : "filePath must not be null";
        assert location.startLine() == line : "startLine should equal input line";
        assert location.startColumn() == column : "startColumn should equal input column";
        assert location.endLine() == line : "endLine should equal startLine for single-point location";
        assert location.endColumn() == column : "endColumn should equal startColumn for single-point location";
    }

    /**
     * Property: Location rejects null filePath
     * 
     * Location construction with null filePath SHALL throw NullPointerException.
     * 
     * Feature: web-legacy-scan, Property 21: Code Element Location Completeness
     * Validates: Requirements 13.1
     */
    @Property(tries = 10)
    void locationRejectsNullFilePath(
            @ForAll @IntRange(min = 1, max = 100) int line,
            @ForAll @IntRange(min = 1, max = 100) int column
    ) {
        try {
            new Location(null, line, column, line, column, -1, -1, null, null);
            throw new AssertionError("Should have thrown NullPointerException for null filePath");
        } catch (NullPointerException e) {
            // Expected behavior
        }
    }
}
