package io.github.sainm.weblegacyscan.core.file;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Property-based tests for File Discovery Correctness.
 * 
 * Property 4: File Discovery Correctness
 * For any directory structure with nested subdirectories, the File_Discoverer SHALL:
 * - Return all files matching the configured extensions
 * - Exclude files matching exclude patterns
 * - Include only files matching include patterns
 * - Not return the same file twice
 * - Not enter infinite loops on circular symbolic links
 * 
 * Validates: Requirements 5.6, 10.1, 10.2, 10.3, 10.4, 10.7
 */
class FileDiscoveryPropertyTest {

    private Path createTempDir() throws IOException {
        return Files.createTempDirectory("file-discovery-test");
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

    // ========== Property 4.1: Extension Filtering ==========

    /**
     * Property 4: File Discovery Correctness - Extension Filtering
     * 
     * For any set of files with various extensions, the File_Discoverer SHALL return
     * only files matching the configured extensions.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.1, 10.2
     */
    @Property(tries = 100)
    void discovererReturnsOnlyFilesWithMatchingExtensions(
            @ForAll("fileNamesWithExtensions") List<String> fileNames
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create files in temp directory
            for (String fileName : fileNames) {
                Files.createFile(tempDir.resolve(fileName));
            }

            Set<String> targetExtensions = Set.of(".html", ".css", ".js");
            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(targetExtensions)
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // All discovered files must have matching extensions
            for (SourceFile file : discovered) {
                String ext = file.getExtension().toLowerCase();
                assert targetExtensions.contains(ext) 
                    : "Discovered file " + file.path() + " has non-matching extension: " + ext;
            }

            // Count expected files
            long expectedCount = fileNames.stream()
                    .filter(name -> {
                        String lower = name.toLowerCase();
                        return targetExtensions.stream().anyMatch(lower::endsWith);
                    })
                    .count();

            assert discovered.size() == expectedCount 
                : "Expected " + expectedCount + " files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.2: Exclude Pattern Filtering ==========

    /**
     * Property 4: File Discovery Correctness - Exclude Pattern Filtering
     * 
     * For any set of files, files matching exclude patterns SHALL NOT be returned.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.4
     */
    @Property(tries = 100)
    void discovererExcludesFilesMatchingExcludePatterns(
            @ForAll @IntRange(min = 1, max = 5) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create a subdirectory to exclude
            Path excludedDir = Files.createDirectory(tempDir.resolve("excluded"));
            Path includedDir = Files.createDirectory(tempDir.resolve("included"));

            // Create files in both directories
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(excludedDir.resolve("file" + i + ".html"));
                Files.createFile(includedDir.resolve("file" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .excludePatterns(List.of("excluded/**"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // No files from excluded directory should be returned
            for (SourceFile file : discovered) {
                String relativePath = tempDir.relativize(file.path()).toString().replace('\\', '/');
                assert !relativePath.startsWith("excluded/") 
                    : "File from excluded directory was returned: " + relativePath;
            }

            // All files from included directory should be returned
            assert discovered.size() == fileCount 
                : "Expected " + fileCount + " files from included dir but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.3: Include Pattern Filtering ==========

    /**
     * Property 4: File Discovery Correctness - Include Pattern Filtering
     * 
     * When include patterns are specified, only files matching those patterns SHALL be returned.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.3
     */
    @Property(tries = 100)
    void discovererReturnsOnlyFilesMatchingIncludePatterns(
            @ForAll @IntRange(min = 1, max = 5) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create subdirectories
            Path srcDir = Files.createDirectory(tempDir.resolve("src"));
            Path libDir = Files.createDirectory(tempDir.resolve("lib"));

            // Create files in both directories
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(srcDir.resolve("file" + i + ".html"));
                Files.createFile(libDir.resolve("file" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .includePatterns(List.of("src/**"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // All discovered files must match include pattern
            for (SourceFile file : discovered) {
                String relativePath = tempDir.relativize(file.path()).toString().replace('\\', '/');
                assert relativePath.startsWith("src/") 
                    : "File not matching include pattern was returned: " + relativePath;
            }

            // Only files from src directory should be returned
            assert discovered.size() == fileCount 
                : "Expected " + fileCount + " files from src dir but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.4: No Duplicate Files ==========

    /**
     * Property 4: File Discovery Correctness - No Duplicates
     * 
     * The File_Discoverer SHALL NOT return the same file twice.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.1
     */
    @Property(tries = 100)
    void discovererNeverReturnsDuplicateFiles(
            @ForAll @IntRange(min = 1, max = 10) int fileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create nested directory structure
            Path level1 = Files.createDirectory(tempDir.resolve("level1"));
            Path level2 = Files.createDirectory(level1.resolve("level2"));

            // Create files at different levels
            for (int i = 0; i < fileCount; i++) {
                Files.createFile(tempDir.resolve("root" + i + ".html"));
                Files.createFile(level1.resolve("l1_" + i + ".html"));
                Files.createFile(level2.resolve("l2_" + i + ".html"));
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();
            Set<Path> uniquePaths = discovered.stream()
                    .map(SourceFile::path)
                    .collect(Collectors.toSet());

            assert discovered.size() == uniquePaths.size() 
                : "Duplicate files detected: found " + discovered.size() + " files but only " 
                  + uniquePaths.size() + " unique paths";
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.5: Recursive Directory Scanning ==========

    /**
     * Property 4: File Discovery Correctness - Recursive Scanning
     * 
     * The File_Discoverer SHALL recursively scan all nested subdirectories.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 5.6, 10.1
     */
    @Property(tries = 100)
    void discovererScansNestedDirectoriesRecursively(
            @ForAll @IntRange(min = 1, max = 4) int depth,
            @ForAll @IntRange(min = 1, max = 3) int filesPerLevel
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create nested directory structure
            Path currentDir = tempDir;
            int totalExpectedFiles = 0;

            for (int level = 0; level < depth; level++) {
                for (int i = 0; i < filesPerLevel; i++) {
                    Files.createFile(currentDir.resolve("file_l" + level + "_" + i + ".html"));
                    totalExpectedFiles++;
                }
                if (level < depth - 1) {
                    currentDir = Files.createDirectory(currentDir.resolve("level" + (level + 1)));
                }
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            assert discovered.size() == totalExpectedFiles 
                : "Expected " + totalExpectedFiles + " files across " + depth 
                  + " levels but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.6: Symbolic Link Loop Prevention ==========

    /**
     * Property 4: File Discovery Correctness - Symbolic Link Loop Prevention
     * 
     * The File_Discoverer SHALL NOT enter infinite loops on circular symbolic links.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.7
     */
    @Example
    void discovererHandlesCircularSymbolicLinks() throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create a directory with a file
            Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
            Files.createFile(subDir.resolve("test.html"));

            // Try to create a circular symbolic link (subdir -> tempDir)
            Path symlink = subDir.resolve("circular");
            try {
                Files.createSymbolicLink(symlink, tempDir);
            } catch (UnsupportedOperationException | IOException e) {
                // Symbolic links not supported on this system, skip test
                return;
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(false)
                    .build();

            // This should complete without infinite loop
            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // Should find the test.html file exactly once
            long htmlCount = discovered.stream()
                    .filter(f -> f.path().getFileName().toString().equals("test.html"))
                    .count();

            assert htmlCount == 1 
                : "Expected exactly 1 test.html file but found " + htmlCount;
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.7: Empty Directory Handling ==========

    /**
     * Property 4: File Discovery Correctness - Empty Directory
     * 
     * For an empty directory, the File_Discoverer SHALL return an empty stream.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.1
     */
    @Example
    void discovererReturnsEmptyForEmptyDirectory() throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            FilterConfig config = FilterConfig.defaults();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            assert discovered.isEmpty() 
                : "Expected empty result for empty directory but found " + discovered.size() + " files";
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.8: Non-Existent Path Handling ==========

    /**
     * Property 4: File Discovery Correctness - Non-Existent Path
     * 
     * For a non-existent path, the File_Discoverer SHALL return an empty stream.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.1
     */
    @Example
    void discovererReturnsEmptyForNonExistentPath() throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            Path nonExistent = tempDir.resolve("does-not-exist");
            FilterConfig config = FilterConfig.defaults();

            List<SourceFile> discovered = discoverer.discover(nonExistent, config).toList();

            assert discovered.isEmpty() 
                : "Expected empty result for non-existent path but found " + discovered.size() + " files";
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 4.9: Single File Discovery ==========

    /**
     * Property 4: File Discovery Correctness - Single File
     * 
     * When given a single file path, the File_Discoverer SHALL return that file
     * if it matches the configured extensions.
     * 
     * Feature: web-legacy-scan, Property 4: File Discovery Correctness
     * Validates: Requirements 10.1, 10.2
     */
    @Property(tries = 100)
    void discovererHandlesSingleFileInput(
            @ForAll("validExtension") String extension
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            Path singleFile = Files.createFile(tempDir.resolve("test" + extension));

            Set<String> targetExtensions = Set.of(".html", ".css", ".js");
            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(targetExtensions)
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(singleFile, config).toList();

            boolean shouldMatch = targetExtensions.contains(extension.toLowerCase());
            
            if (shouldMatch) {
                assert discovered.size() == 1 
                    : "Expected 1 file for matching extension " + extension + " but found " + discovered.size();
                assert discovered.get(0).path().equals(singleFile) 
                    : "Discovered file path doesn't match input";
            } else {
                assert discovered.isEmpty() 
                    : "Expected 0 files for non-matching extension " + extension + " but found " + discovered.size();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<List<String>> fileNamesWithExtensions() {
        Arbitrary<String> baseName = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(String::toLowerCase); // Use lowercase to avoid case-sensitivity issues on Windows
        
        Arbitrary<String> extension = Arbitraries.of(
                ".html", ".htm", ".css", ".js", ".txt", ".md", ".java", ".xml"
        );

        return Combinators.combine(baseName, extension)
                .as((name, ext) -> name + ext)
                .list()
                .ofMinSize(1)
                .ofMaxSize(20)
                .filter(list -> {
                    // Ensure unique names (case-insensitive for Windows compatibility)
                    Set<String> lowerCaseNames = new HashSet<>();
                    for (String name : list) {
                        if (!lowerCaseNames.add(name.toLowerCase())) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    @Provide
    Arbitrary<String> validExtension() {
        return Arbitraries.of(".html", ".css", ".js", ".txt", ".md", ".java");
    }
}
