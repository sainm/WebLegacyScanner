package io.github.sainm.weblegacyscan.core.file;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Property-based tests for Gitignore Pattern Exclusion.
 * 
 * Property 5: Gitignore Pattern Exclusion
 * For any directory containing a `.gitignore` file, when `respectGitignore` is enabled,
 * the File_Discoverer SHALL exclude all files matching the gitignore patterns.
 * 
 * Validates: Requirements 5.7, 10.5
 */
class GitignorePatternPropertyTest {

    private Path createTempDir() throws IOException {
        return Files.createTempDirectory("gitignore-test");
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try {
            if (Files.isDirectory(path)) {
                try (var entries = Files.list(path)) {
                    for (Path entry : entries.toList()) {
                        deleteRecursively(entry);
                    }
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    // ========== Property 5.1: Gitignore File Pattern Exclusion ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - File Pattern
     * 
     * For any directory with a .gitignore containing file patterns,
     * files matching those patterns SHALL be excluded when respectGitignore is enabled.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void gitignoreExcludesMatchingFiles(
            @ForAll @IntRange(min = 1, max = 5) int matchingFileCount,
            @ForAll @IntRange(min = 1, max = 5) int nonMatchingFileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with pattern to exclude *.log files
            Files.writeString(tempDir.resolve(".gitignore"), "*.log\n");

            // Create matching files (should be excluded)
            for (int i = 0; i < matchingFileCount; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".log"));
            }

            // Create non-matching files (should be included)
            for (int i = 0; i < nonMatchingFileCount; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".html"));
            }

            FilterConfig configWithGitignore = FilterConfig.builder()
                    .includeExtensions(Set.of(".html", ".log"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, configWithGitignore).toList();

            // No .log files should be returned
            long logCount = discovered.stream()
                    .filter(f -> f.path().toString().endsWith(".log"))
                    .count();
            assert logCount == 0 
                : "Expected 0 .log files but found " + logCount + " (gitignore should exclude them)";

            // All .html files should be returned
            assert discovered.size() == nonMatchingFileCount 
                : "Expected " + nonMatchingFileCount + " .html files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.2: Gitignore Directory Pattern Exclusion ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Directory Pattern
     * 
     * For any directory with a .gitignore containing directory patterns,
     * files in those directories SHALL be excluded when respectGitignore is enabled.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void gitignoreExcludesMatchingDirectories(
            @ForAll @IntRange(min = 1, max = 5) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with directory pattern
            Files.writeString(tempDir.resolve(".gitignore"), "ignored_dir/\n");

            // Create ignored directory with files
            Path ignoredDir = Files.createDirectory(tempDir.resolve("ignored_dir"));
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(ignoredDir.resolve("file" + i + ".html"));
            }

            // Create included directory with files
            Path includedDir = Files.createDirectory(tempDir.resolve("included_dir"));
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(includedDir.resolve("file" + i + ".html"));
            }

            FilterConfig configWithGitignore = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, configWithGitignore).toList();

            // No files from ignored_dir should be returned
            long ignoredCount = discovered.stream()
                    .filter(f -> f.path().toString().contains("ignored_dir"))
                    .count();
            assert ignoredCount == 0 
                : "Expected 0 files from ignored_dir but found " + ignoredCount;

            // All files from included_dir should be returned
            assert discovered.size() == fileCount 
                : "Expected " + fileCount + " files from included_dir but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.3: Gitignore Disabled ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Disabled
     * 
     * When respectGitignore is disabled, files matching gitignore patterns
     * SHALL still be included in the results.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void gitignoreDisabledIncludesAllFiles(
            @ForAll @IntRange(min = 1, max = 5) int matchingFileCount,
            @ForAll @IntRange(min = 1, max = 5) int nonMatchingFileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with pattern
            Files.writeString(tempDir.resolve(".gitignore"), "*.log\n");

            // Create matching files
            for (int i = 0; i < matchingFileCount; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".log"));
            }

            // Create non-matching files
            for (int i = 0; i < nonMatchingFileCount; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".html"));
            }

            FilterConfig configWithoutGitignore = FilterConfig.builder()
                    .includeExtensions(Set.of(".html", ".log"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, configWithoutGitignore).toList();

            // All files should be returned when gitignore is disabled
            int expectedTotal = matchingFileCount + nonMatchingFileCount;
            assert discovered.size() == expectedTotal 
                : "Expected " + expectedTotal + " files but found " + discovered.size() 
                  + " (gitignore should be ignored)";
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.4: Gitignore Wildcard Patterns ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Wildcard Patterns
     * 
     * For any directory with a .gitignore containing wildcard patterns (e.g., *.tmp, test_*),
     * files matching those patterns SHALL be excluded.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void gitignoreWildcardPatternsExcludeFiles(
            @ForAll @IntRange(min = 1, max = 5) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with wildcard patterns
            Files.writeString(tempDir.resolve(".gitignore"), "test_*.html\n*.tmp\n");

            // Create files matching test_* pattern
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(tempDir.resolve("test_file" + i + ".html"));
            }

            // Create files matching *.tmp pattern
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(tempDir.resolve("data" + i + ".tmp"));
            }

            // Create non-matching files
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(tempDir.resolve("main" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html", ".tmp"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // No test_*.html or *.tmp files should be returned
            for (SourceFile file : discovered) {
                String fileName = file.path().getFileName().toString();
                assert !fileName.startsWith("test_") 
                    : "File matching test_* pattern was not excluded: " + fileName;
                assert !fileName.endsWith(".tmp") 
                    : "File matching *.tmp pattern was not excluded: " + fileName;
            }

            // Only main*.html files should be returned
            assert discovered.size() == fileCount 
                : "Expected " + fileCount + " main*.html files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.5: Gitignore Nested Directory Patterns ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Nested Directory Patterns
     * 
     * For any directory with a .gitignore containing path patterns (e.g., logs/**),
     * files in nested directories matching those patterns SHALL be excluded.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void gitignoreNestedDirectoryPatternsExcludeFiles(
            @ForAll @IntRange(min = 1, max = 3) int depth,
            @ForAll @IntRange(min = 1, max = 3) int filesPerLevel
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with nested path pattern
            Files.writeString(tempDir.resolve(".gitignore"), "logs/**\n");

            // Create logs directory with nested structure
            Path logsDir = Files.createDirectory(tempDir.resolve("logs"));
            Path currentLogsDir = logsDir;
            for (int level = 0; level < depth; level++) {
                for (int i = 0; i < filesPerLevel; i++) {
                    Files.createFile(currentLogsDir.resolve("log" + level + "_" + i + ".html"));
                }
                if (level < depth - 1) {
                    currentLogsDir = Files.createDirectory(currentLogsDir.resolve("sublogs" + level));
                }
            }

            // Create src directory with nested structure (should be included)
            Path srcDir = Files.createDirectory(tempDir.resolve("src"));
            Path currentSrcDir = srcDir;
            int expectedFiles = 0;
            for (int level = 0; level < depth; level++) {
                for (int i = 0; i < filesPerLevel; i++) {
                    Files.createFile(currentSrcDir.resolve("src" + level + "_" + i + ".html"));
                    expectedFiles++;
                }
                if (level < depth - 1) {
                    currentSrcDir = Files.createDirectory(currentSrcDir.resolve("subsrc" + level));
                }
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // No files from logs directory should be returned
            long logsCount = discovered.stream()
                    .filter(f -> f.path().toString().contains("logs"))
                    .count();
            assert logsCount == 0 
                : "Expected 0 files from logs directory but found " + logsCount;

            // All files from src directory should be returned
            assert discovered.size() == expectedFiles 
                : "Expected " + expectedFiles + " files from src but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.6: Gitignore Comment Lines ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Comment Lines
     * 
     * Lines starting with # in .gitignore SHALL be treated as comments and ignored.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Example
    void gitignoreIgnoresCommentLines() throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with comments
            Files.writeString(tempDir.resolve(".gitignore"), 
                "# This is a comment\n" +
                "*.log\n" +
                "# Another comment\n" +
                "  # Indented comment\n");

            // Create files
            Files.createFile(tempDir.resolve("test.log"));
            Files.createFile(tempDir.resolve("test.html"));
            Files.createFile(tempDir.resolve("#comment.html")); // File starting with #

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html", ".log"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // .log file should be excluded
            long logCount = discovered.stream()
                    .filter(f -> f.path().toString().endsWith(".log"))
                    .count();
            assert logCount == 0 : "Expected 0 .log files but found " + logCount;

            // Both .html files should be included (including #comment.html)
            assert discovered.size() == 2 
                : "Expected 2 .html files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.7: Empty Gitignore ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Empty Gitignore
     * 
     * An empty .gitignore file SHALL not exclude any files.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void emptyGitignoreExcludesNothing(
            @ForAll @IntRange(min = 1, max = 10) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create empty .gitignore
            Files.writeString(tempDir.resolve(".gitignore"), "");

            // Create files
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // All files should be returned
            assert discovered.size() == fileCount 
                : "Expected " + fileCount + " files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.8: No Gitignore File ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - No Gitignore File
     * 
     * When no .gitignore file exists, all matching files SHALL be included
     * regardless of respectGitignore setting.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void noGitignoreIncludesAllFiles(
            @ForAll @IntRange(min = 1, max = 10) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // No .gitignore file created

            // Create files
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // All files should be returned
            assert discovered.size() == fileCount 
                : "Expected " + fileCount + " files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 5.9: Multiple Patterns in Gitignore ==========

    /**
     * Property 5: Gitignore Pattern Exclusion - Multiple Patterns
     * 
     * For any .gitignore with multiple patterns, files matching ANY pattern SHALL be excluded.
     * 
     * Feature: web-legacy-scan, Property 5: Gitignore Pattern Exclusion
     * Validates: Requirements 5.7, 10.5
     */
    @Property(tries = 100)
    void gitignoreMultiplePatternsExcludeFiles(
            @ForAll @IntRange(min = 1, max = 3) int filesPerPattern
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create .gitignore with multiple patterns
            Files.writeString(tempDir.resolve(".gitignore"), 
                "*.log\n" +
                "*.tmp\n" +
                "*.bak\n");

            // Create files matching each pattern
            for (int i = 0; i < filesPerPattern; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".log"));
                Files.createFile(tempDir.resolve("file" + i + ".tmp"));
                Files.createFile(tempDir.resolve("file" + i + ".bak"));
            }

            // Create non-matching files
            for (int i = 0; i < filesPerPattern; i++) {
                Files.createFile(tempDir.resolve("file" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html", ".log", ".tmp", ".bak"))
                    .respectGitignore(true)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // Only .html files should be returned
            for (SourceFile file : discovered) {
                String fileName = file.path().getFileName().toString();
                assert fileName.endsWith(".html") 
                    : "Non-.html file was not excluded: " + fileName;
            }

            assert discovered.size() == filesPerPattern 
                : "Expected " + filesPerPattern + " .html files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }
}
