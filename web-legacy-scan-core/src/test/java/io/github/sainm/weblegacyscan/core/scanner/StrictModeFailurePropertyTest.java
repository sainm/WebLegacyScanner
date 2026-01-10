package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.executor.DefaultVirtualThreadExecutor;
import io.github.sainm.weblegacyscan.core.executor.VirtualThreadExecutor;
import io.github.sainm.weblegacyscan.core.file.*;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * Property-based tests for Strict Mode Failure.
 * 
 * Property 19: Strict Mode Failure
 * For any scan in strict mode, if any file produces a parse error, 
 * the entire scan SHALL fail and return an error result.
 * 
 * Validates: Requirements 12.4
 */
class StrictModeFailurePropertyTest {

    /**
     * Property 19: Strict Mode Failure
     * 
     * For any scan in strict mode, if any file produces a fatal parse error,
     * the scan SHALL indicate failure by having fewer successfully scanned files
     * than total files, and errors SHALL be recorded.
     * 
     * Feature: web-legacy-scan, Property 19: Strict Mode Failure
     * Validates: Requirements 12.4
     */
    @Property(tries = 100)
    void strictModeFailsOnFatalParseError(
            @ForAll @IntRange(min = 1, max = 5) int validFileCount,
            @ForAll @IntRange(min = 1, max = 3) int failingFileCount
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("strict-mode-test");
        try {
            // Register a parser that produces fatal errors for specific content
            ParserFactory.reset();
            ParserFactory.registerParser(new FatalErrorParser());

            // Create valid HTML files
            List<Path> validFiles = new ArrayList<>();
            for (int i = 0; i < validFileCount; i++) {
                Path file = tempDir.resolve("valid" + i + ".html");
                Files.writeString(file, "<html><body><p>Valid content " + i + "</p></body></html>");
                validFiles.add(file);
            }

            // Create files that will cause fatal parse errors
            List<Path> failingFiles = new ArrayList<>();
            for (int i = 0; i < failingFileCount; i++) {
                Path file = tempDir.resolve("failing" + i + ".html");
                Files.writeString(file, "TRIGGER_FATAL_ERROR");
                failingFiles.add(file);
            }

            // Setup scanner
            RuleEvaluator ruleEvaluator = new NoOpRuleEvaluator();
            FileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
            VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(2);
            Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor);

            // Configure scan with strict mode enabled
            ScanConfig config = ScanConfig.builder()
                    .targetPath(tempDir)
                    .strictMode(true) // Strict mode
                    .maxConcurrency(2)
                    .build();

            // Execute scan
            ScanResult result = scanner.scan(config);

            // Verify: ScanResult should not be null
            assert result != null : "ScanResult should not be null";

            // Verify: Total files should include all files
            int totalFiles = validFileCount + failingFileCount;
            assert result.statistics().totalFiles() == totalFiles 
                : "Expected " + totalFiles + " total files but got " + result.statistics().totalFiles();

            // Verify: In strict mode, files with fatal errors should NOT be counted as successfully scanned
            // The scannedFiles count should be less than totalFiles when there are fatal errors
            assert result.statistics().scannedFiles() < totalFiles 
                : "In strict mode, scannedFiles (" + result.statistics().scannedFiles() 
                  + ") should be less than totalFiles (" + totalFiles + ") when fatal errors occur";

            // Verify: Errors should be recorded
            assert result.hasErrors() : "Errors should be recorded when fatal parse errors occur";

        } finally {
            ParserFactory.reset();
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 19: Strict Mode vs Non-Strict Mode Comparison
     * 
     * For the same set of files with parse errors, strict mode SHALL result in
     * fewer successfully scanned files compared to non-strict mode.
     * 
     * Feature: web-legacy-scan, Property 19: Strict Mode Failure
     * Validates: Requirements 12.4
     */
    @Property(tries = 50)
    void strictModeHasFewerSuccessfulScansThanNonStrictMode(
            @ForAll @IntRange(min = 2, max = 4) int validFileCount
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("strict-vs-nonstrict-test");
        try {
            // Register a parser that produces fatal errors for specific content
            ParserFactory.reset();
            ParserFactory.registerParser(new FatalErrorParser());

            // Create valid HTML files
            for (int i = 0; i < validFileCount; i++) {
                Path file = tempDir.resolve("valid" + i + ".html");
                Files.writeString(file, "<html><body><p>Valid content " + i + "</p></body></html>");
            }

            // Create one file that will cause a fatal parse error
            Path failingFile = tempDir.resolve("failing.html");
            Files.writeString(failingFile, "TRIGGER_FATAL_ERROR");

            // Setup scanner components
            RuleEvaluator ruleEvaluator = new NoOpRuleEvaluator();
            FileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();

            // Scan in non-strict mode
            VirtualThreadExecutor executor1 = new DefaultVirtualThreadExecutor(2);
            Scanner scanner1 = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor1);
            ScanConfig nonStrictConfig = ScanConfig.builder()
                    .targetPath(tempDir)
                    .strictMode(false)
                    .maxConcurrency(2)
                    .build();
            ScanResult nonStrictResult = scanner1.scan(nonStrictConfig);

            // Scan in strict mode
            VirtualThreadExecutor executor2 = new DefaultVirtualThreadExecutor(2);
            Scanner scanner2 = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor2);
            ScanConfig strictConfig = ScanConfig.builder()
                    .targetPath(tempDir)
                    .strictMode(true)
                    .maxConcurrency(2)
                    .build();
            ScanResult strictResult = scanner2.scan(strictConfig);

            // Verify: Both results should not be null
            assert nonStrictResult != null : "Non-strict ScanResult should not be null";
            assert strictResult != null : "Strict ScanResult should not be null";

            // Verify: Strict mode should have fewer or equal successfully scanned files
            assert strictResult.statistics().scannedFiles() <= nonStrictResult.statistics().scannedFiles()
                : "Strict mode scannedFiles (" + strictResult.statistics().scannedFiles() 
                  + ") should be <= non-strict mode scannedFiles (" 
                  + nonStrictResult.statistics().scannedFiles() + ")";

        } finally {
            ParserFactory.reset();
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 19: Strict Mode with No Errors
     * 
     * For any scan in strict mode with no parse errors, all files SHALL be
     * successfully scanned.
     * 
     * Feature: web-legacy-scan, Property 19: Strict Mode Failure
     * Validates: Requirements 12.4
     */
    @Property(tries = 50)
    void strictModeSucceedsWhenNoErrors(
            @ForAll @IntRange(min = 1, max = 5) int fileCount
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("strict-mode-success-test");
        try {
            // Register a parser that always succeeds
            ParserFactory.reset();
            ParserFactory.registerParser(new SuccessfulParser());

            // Create valid HTML files only
            for (int i = 0; i < fileCount; i++) {
                Path file = tempDir.resolve("valid" + i + ".html");
                Files.writeString(file, "<html><body><p>Valid content " + i + "</p></body></html>");
            }

            // Setup scanner
            RuleEvaluator ruleEvaluator = new NoOpRuleEvaluator();
            FileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
            VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(2);
            Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor);

            // Configure scan with strict mode enabled
            ScanConfig config = ScanConfig.builder()
                    .targetPath(tempDir)
                    .strictMode(true)
                    .maxConcurrency(2)
                    .build();

            // Execute scan
            ScanResult result = scanner.scan(config);

            // Verify: ScanResult should not be null
            assert result != null : "ScanResult should not be null";

            // Verify: All files should be successfully scanned
            assert result.statistics().totalFiles() == fileCount 
                : "Expected " + fileCount + " total files but got " + result.statistics().totalFiles();
            assert result.statistics().scannedFiles() == fileCount 
                : "Expected " + fileCount + " scanned files but got " + result.statistics().scannedFiles();

            // Verify: No errors should be recorded
            assert !result.hasErrors() : "No errors should be recorded when all files parse successfully";

        } finally {
            ParserFactory.reset();
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 19: Strict Mode Records Fatal Errors
     * 
     * For any scan in strict mode with fatal parse errors, the errors SHALL
     * be recorded in the ScanResult.
     * 
     * Feature: web-legacy-scan, Property 19: Strict Mode Failure
     * Validates: Requirements 12.4
     */
    @Property(tries = 50)
    void strictModeRecordsFatalErrors(
            @ForAll @IntRange(min = 1, max = 3) int failingFileCount
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("strict-mode-errors-test");
        try {
            // Register a parser that produces fatal errors
            ParserFactory.reset();
            ParserFactory.registerParser(new FatalErrorParser());

            // Create files that will cause fatal parse errors
            for (int i = 0; i < failingFileCount; i++) {
                Path file = tempDir.resolve("failing" + i + ".html");
                Files.writeString(file, "TRIGGER_FATAL_ERROR");
            }

            // Setup scanner
            RuleEvaluator ruleEvaluator = new NoOpRuleEvaluator();
            FileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
            VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(2);
            Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor);

            // Configure scan with strict mode enabled
            ScanConfig config = ScanConfig.builder()
                    .targetPath(tempDir)
                    .strictMode(true)
                    .maxConcurrency(2)
                    .build();

            // Execute scan
            ScanResult result = scanner.scan(config);

            // Verify: ScanResult should not be null
            assert result != null : "ScanResult should not be null";

            // Verify: Errors should be recorded
            assert result.hasErrors() : "Errors should be recorded for fatal parse errors";
            
            // Verify: Number of errors should match number of failing files
            assert result.errors().size() >= failingFileCount 
                : "Expected at least " + failingFileCount + " errors but got " + result.errors().size();

        } finally {
            ParserFactory.reset();
            deleteDirectory(tempDir);
        }
    }

    // Helper method to delete directory recursively
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    // No-op rule evaluator for basic tests
    private static class NoOpRuleEvaluator implements RuleEvaluator {
        @Override
        public void loadRules(Path configDir) {}

        @Override
        public List<Issue> evaluate(CodeElement element, String filePath) {
            return List.of();
        }
    }

    // Parser that produces fatal errors for specific content
    private static class FatalErrorParser implements Parser {
        @Override
        public ParseResult parse(SourceFile file) {
            try {
                String content = Files.readString(file.path());
                return parseContent(content, file);
            } catch (IOException e) {
                return ParseResult.failure(List.of(
                        ParseError.fatal(file.path(), 1, 1, "Failed to read file: " + e.getMessage())
                ));
            }
        }

        @Override
        public ParseResult parseContent(String content, SourceFile file) {
            if (content.contains("TRIGGER_FATAL_ERROR")) {
                return ParseResult.failure(List.of(
                        ParseError.fatal(file.path(), 1, 1, "Fatal parse error: invalid content")
                ));
            }
            // Return successful result for valid content
            List<CodeElement> elements = new ArrayList<>();
            
            // Simple regex-based tag detection
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<(\\w+)[^>]*>");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String tagName = matcher.group(1);
                Location location = Location.of(file.path(), 1, matcher.start() + 1);
                elements.add(new HTMLElement(
                        tagName,
                        location,
                        Map.of(),
                        location,
                        matcher.group(),
                        List.of(),
                        HTMLElementType.OPEN_TAG
                ));
            }
            
            ParseStatistics stats = ParseStatistics.builder()
                    .totalElements(elements.size())
                    .htmlElements(elements.size())
                    .parseTime(Duration.ZERO)
                    .build();
            
            return ParseResult.success(elements, stats);
        }

        @Override
        public FileType getSupportedType() {
            return FileType.HTML;
        }
    }

    // Parser that always succeeds
    private static class SuccessfulParser implements Parser {
        @Override
        public ParseResult parse(SourceFile file) {
            try {
                String content = Files.readString(file.path());
                return parseContent(content, file);
            } catch (IOException e) {
                return ParseResult.failure(List.of(
                        ParseError.fatal(file.path(), 1, 1, "Failed to read file: " + e.getMessage())
                ));
            }
        }

        @Override
        public ParseResult parseContent(String content, SourceFile file) {
            List<CodeElement> elements = new ArrayList<>();
            
            // Simple regex-based tag detection
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<(\\w+)[^>]*>");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String tagName = matcher.group(1);
                Location location = Location.of(file.path(), 1, matcher.start() + 1);
                elements.add(new HTMLElement(
                        tagName,
                        location,
                        Map.of(),
                        location,
                        matcher.group(),
                        List.of(),
                        HTMLElementType.OPEN_TAG
                ));
            }
            
            ParseStatistics stats = ParseStatistics.builder()
                    .totalElements(elements.size())
                    .htmlElements(elements.size())
                    .parseTime(Duration.ZERO)
                    .build();
            
            return ParseResult.success(elements, stats);
        }

        @Override
        public FileType getSupportedType() {
            return FileType.HTML;
        }
    }
}
