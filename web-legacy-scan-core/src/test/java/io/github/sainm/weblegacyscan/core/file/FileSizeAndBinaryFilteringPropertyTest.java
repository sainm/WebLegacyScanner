package io.github.sainm.weblegacyscan.core.file;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Property-based tests for File Size and Binary Filtering.
 * 
 * Property 17: File Size and Binary Filtering
 * For any file exceeding the configured size limit or detected as binary,
 * the File_Discoverer SHALL exclude it from the scan results.
 * 
 * Validates: Requirements 10.8
 */
class FileSizeAndBinaryFilteringPropertyTest {

    private Path createTempDir() throws IOException {
        return Files.createTempDirectory("file-filter-test");
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

    // ========== Property 17.1: File Size Limit Filtering ==========

    /**
     * Property 17: File Size and Binary Filtering - Size Limit
     * 
     * For any file exceeding the configured size limit, the File_Discoverer
     * SHALL exclude it from the scan results.
     * 
     * Feature: web-legacy-scan, Property 17: File Size and Binary Filtering
     * Validates: Requirements 10.8
     */
    @Property(tries = 100)
    void discovererExcludesFilesExceedingSizeLimit(
            @ForAll @IntRange(min = 100, max = 1000) int sizeLimit,
            @ForAll @IntRange(min = 1, max = 5) int smallFileCount,
            @ForAll @IntRange(min = 1, max = 3) int largeFileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create small files (under limit)
            for (int i = 0; i < smallFileCount; i++) {
                Path file = tempDir.resolve("small" + i + ".html");
                byte[] content = new byte[sizeLimit - 10]; // Under limit
                Arrays.fill(content, (byte) 'a');
                Files.write(file, content);
            }

            // Create large files (over limit)
            for (int i = 0; i < largeFileCount; i++) {
                Path file = tempDir.resolve("large" + i + ".html");
                byte[] content = new byte[sizeLimit + 100]; // Over limit
                Arrays.fill(content, (byte) 'a');
                Files.write(file, content);
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .maxFileSizeBytes(sizeLimit)
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // All discovered files must be under the size limit
            for (SourceFile file : discovered) {
                long fileSize = Files.size(file.path());
                assert fileSize <= sizeLimit 
                    : "File " + file.path() + " with size " + fileSize 
                      + " exceeds limit " + sizeLimit;
            }

            // Only small files should be discovered
            assert discovered.size() == smallFileCount 
                : "Expected " + smallFileCount + " small files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 17.2: Binary File Filtering ==========

    /**
     * Property 17: File Size and Binary Filtering - Binary Detection
     * 
     * For any file detected as binary (containing null bytes), the File_Discoverer
     * SHALL exclude it from the scan results.
     * 
     * Feature: web-legacy-scan, Property 17: File Size and Binary Filtering
     * Validates: Requirements 10.8
     */
    @Property(tries = 100)
    void discovererExcludesBinaryFiles(
            @ForAll @IntRange(min = 1, max = 5) int textFileCount,
            @ForAll @IntRange(min = 1, max = 3) int binaryFileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create text files (no null bytes)
            for (int i = 0; i < textFileCount; i++) {
                Path file = tempDir.resolve("text" + i + ".html");
                String content = "<html><body>Hello World " + i + "</body></html>";
                Files.writeString(file, content);
            }

            // Create binary files (with null bytes)
            for (int i = 0; i < binaryFileCount; i++) {
                Path file = tempDir.resolve("binary" + i + ".html");
                byte[] content = new byte[100];
                Arrays.fill(content, (byte) 'a');
                content[50] = 0; // Insert null byte to make it binary
                Files.write(file, content);
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // No binary files should be discovered
            for (SourceFile file : discovered) {
                byte[] bytes = Files.readAllBytes(file.path());
                boolean hasBinaryContent = containsNullByte(bytes);
                assert !hasBinaryContent 
                    : "Binary file " + file.path() + " was not excluded";
            }

            // Only text files should be discovered
            assert discovered.size() == textFileCount 
                : "Expected " + textFileCount + " text files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 17.3: Combined Size and Binary Filtering ==========

    /**
     * Property 17: File Size and Binary Filtering - Combined Filtering
     * 
     * For any directory containing a mix of valid, oversized, and binary files,
     * the File_Discoverer SHALL only return files that are both under the size
     * limit AND not binary.
     * 
     * Feature: web-legacy-scan, Property 17: File Size and Binary Filtering
     * Validates: Requirements 10.8
     */
    @Property(tries = 100)
    void discovererAppliesBothSizeAndBinaryFiltering(
            @ForAll @IntRange(min = 100, max = 500) int sizeLimit,
            @ForAll @IntRange(min = 1, max = 3) int validFileCount,
            @ForAll @IntRange(min = 0, max = 2) int oversizedFileCount,
            @ForAll @IntRange(min = 0, max = 2) int binaryFileCount
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create valid files (under limit, not binary)
            for (int i = 0; i < validFileCount; i++) {
                Path file = tempDir.resolve("valid" + i + ".html");
                String content = "<html>" + "x".repeat(Math.max(0, sizeLimit - 50)) + "</html>";
                if (content.length() > sizeLimit) {
                    content = content.substring(0, sizeLimit - 10);
                }
                Files.writeString(file, content);
            }

            // Create oversized files
            for (int i = 0; i < oversizedFileCount; i++) {
                Path file = tempDir.resolve("oversized" + i + ".html");
                byte[] content = new byte[sizeLimit + 100];
                Arrays.fill(content, (byte) 'a');
                Files.write(file, content);
            }

            // Create binary files (under size limit but binary)
            for (int i = 0; i < binaryFileCount; i++) {
                Path file = tempDir.resolve("binarysmall" + i + ".html");
                byte[] content = new byte[sizeLimit - 50];
                Arrays.fill(content, (byte) 'a');
                content[10] = 0; // Insert null byte
                Files.write(file, content);
            }

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .maxFileSizeBytes(sizeLimit)
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // All discovered files must pass both filters
            for (SourceFile file : discovered) {
                long fileSize = Files.size(file.path());
                byte[] bytes = Files.readAllBytes(file.path());
                
                assert fileSize <= sizeLimit 
                    : "File " + file.path() + " exceeds size limit";
                assert !containsNullByte(bytes) 
                    : "Binary file " + file.path() + " was not excluded";
            }

            // Only valid files should be discovered
            assert discovered.size() == validFileCount 
                : "Expected " + validFileCount + " valid files but found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 17.4: Edge Case - Exactly At Size Limit ==========

    /**
     * Property 17: File Size and Binary Filtering - Boundary Condition
     * 
     * Files exactly at the size limit SHALL be included (not excluded).
     * 
     * Feature: web-legacy-scan, Property 17: File Size and Binary Filtering
     * Validates: Requirements 10.8
     */
    @Property(tries = 50)
    void discovererIncludesFilesExactlyAtSizeLimit(
            @ForAll @IntRange(min = 100, max = 1000) int sizeLimit
    ) throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create file exactly at size limit
            Path exactFile = tempDir.resolve("exact.html");
            byte[] content = new byte[sizeLimit];
            Arrays.fill(content, (byte) 'a');
            Files.write(exactFile, content);

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .maxFileSizeBytes(sizeLimit)
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // File exactly at limit should be included
            assert discovered.size() == 1 
                : "File exactly at size limit should be included, found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Property 17.5: Empty Files Are Not Binary ==========

    /**
     * Property 17: File Size and Binary Filtering - Empty Files
     * 
     * Empty files (0 bytes) SHALL NOT be considered binary and SHALL be included
     * if they match other criteria.
     * 
     * Feature: web-legacy-scan, Property 17: File Size and Binary Filtering
     * Validates: Requirements 10.8
     */
    @Example
    void discovererIncludesEmptyFiles() throws IOException {
        Path tempDir = createTempDir();
        try {
            FileDiscoverer discoverer = new DefaultFileDiscoverer();
            
            // Create empty file
            Path emptyFile = tempDir.resolve("empty.html");
            Files.createFile(emptyFile);

            FilterConfig config = FilterConfig.builder()
                    .includeExtensions(Set.of(".html"))
                    .respectGitignore(false)
                    .build();

            List<SourceFile> discovered = discoverer.discover(tempDir, config).toList();

            // Empty file should be included
            assert discovered.size() == 1 
                : "Empty file should be included, found " + discovered.size();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ========== Helper Methods ==========

    private boolean containsNullByte(byte[] bytes) {
        int checkLength = Math.min(bytes.length, 8000);
        for (int i = 0; i < checkLength; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }
}
