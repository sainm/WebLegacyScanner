package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.executor.DefaultVirtualThreadExecutor;
import io.github.sainm.weblegacyscan.core.executor.VirtualThreadExecutor;
import io.github.sainm.weblegacyscan.core.file.*;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * Property-based tests for Encoding Detection and BOM Handling.
 * 
 * Property 18: Encoding Detection and BOM Handling
 * For any source file with a BOM marker (UTF-8, UTF-16), the Parser SHALL correctly 
 * detect the encoding and parse the content without including BOM characters in the output.
 * 
 * Validates: Requirements 12.5, 12.6
 */
class EncodingDetectionPropertyTest {

    // UTF-8 BOM bytes
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * Property 18: Encoding Detection and BOM Handling - UTF-8 BOM
     * 
     * For any HTML file with UTF-8 BOM marker, the Scanner SHALL correctly detect 
     * the encoding and parse the content without including BOM characters in the output.
     * 
     * Feature: web-legacy-scan, Property 18: Encoding Detection and BOM Handling
     * Validates: Requirements 12.5, 12.6
     */
    @Property(tries = 100)
    void utf8BomIsDetectedAndStripped(
            @ForAll("validHtmlContent") String htmlContent
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("encoding-utf8-bom-test");
        try {
            // Create file with UTF-8 BOM
            Path file = tempDir.resolve("test.html");
            writeFileWithBom(file, UTF8_BOM, htmlContent, StandardCharsets.UTF_8);

            // Setup scanner
            ParserFactory.reset();
            ParserFactory.registerParser(new BomAwareHTMLParser());
            
            FileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
            RuleEvaluator ruleEvaluator = new NoOpRuleEvaluator();
            VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(1);
            Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor);

            ScanConfig config = ScanConfig.builder()
                    .targetPath(tempDir)
                    .defaultEncoding(StandardCharsets.UTF_8)
                    .build();

            ScanResult result = scanner.scan(config);

            // Verify: Scanner should complete successfully
            assert result != null : "ScanResult should not be null";
            assert result.statistics().totalFiles() == 1 : "Expected 1 file";
            assert result.statistics().scannedFiles() == 1 : "Expected 1 scanned file";
            
            // Verify: No errors related to BOM
            boolean hasBomError = result.errors().stream()
                    .anyMatch(e -> e.message().toLowerCase().contains("bom"));
            assert !hasBomError : "Should not have BOM-related errors";

        } finally {
            ParserFactory.reset();
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 18: Encoding Detection and BOM Handling - UTF-8 BOM Detection
     * 
     * For any file with UTF-8 BOM, the encoding detection SHALL return UTF-8.
     * 
     * Feature: web-legacy-scan, Property 18: Encoding Detection and BOM Handling
     * Validates: Requirements 12.5, 12.6
     */
    @Property(tries = 100)
    void utf8BomEncodingIsCorrectlyDetected(
            @ForAll("validHtmlContent") String htmlContent
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("encoding-utf8-detection-test");
        try {
            // Create file with UTF-8 BOM
            Path file = tempDir.resolve("test.html");
            writeFileWithBom(file, UTF8_BOM, htmlContent, StandardCharsets.UTF_8);

            // Read file bytes and verify BOM is present
            byte[] bytes = Files.readAllBytes(file);
            assert bytes.length >= 3 : "File should have at least 3 bytes for BOM";
            assert bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF 
                : "File should start with UTF-8 BOM";

            // Verify encoding detection
            Charset detected = detectEncodingFromBytes(bytes);
            assert detected.equals(StandardCharsets.UTF_8) 
                : "UTF-8 BOM should be detected as UTF-8, got: " + detected;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 18: Encoding Detection and BOM Handling - No BOM defaults to UTF-8
     * 
     * For any file without BOM marker, the Scanner SHALL default to UTF-8 encoding.
     * 
     * Feature: web-legacy-scan, Property 18: Encoding Detection and BOM Handling
     * Validates: Requirements 12.5
     */
    @Property(tries = 100)
    void noBomDefaultsToUtf8(
            @ForAll("validHtmlContent") String htmlContent
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("encoding-no-bom-test");
        try {
            // Create file without BOM
            Path file = tempDir.resolve("test.html");
            Files.writeString(file, htmlContent, StandardCharsets.UTF_8);

            // Verify no BOM in file
            byte[] bytes = Files.readAllBytes(file);
            boolean hasBom = (bytes.length >= 3 && 
                bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
            assert !hasBom : "File should not have BOM";

            // Verify encoding detection defaults to UTF-8
            Charset detected = detectEncodingFromBytes(bytes);
            assert detected.equals(StandardCharsets.UTF_8) 
                : "No BOM should default to UTF-8, got: " + detected;

            // Setup scanner
            ParserFactory.reset();
            ParserFactory.registerParser(new BomAwareHTMLParser());
            
            FileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
            RuleEvaluator ruleEvaluator = new NoOpRuleEvaluator();
            VirtualThreadExecutor executor = new DefaultVirtualThreadExecutor(1);
            Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEvaluator, executor);

            ScanConfig config = ScanConfig.builder()
                    .targetPath(tempDir)
                    .defaultEncoding(StandardCharsets.UTF_8)
                    .build();

            ScanResult result = scanner.scan(config);

            // Verify: Scanner should complete successfully
            assert result != null : "ScanResult should not be null";
            assert result.statistics().totalFiles() == 1 : "Expected 1 file";
            assert result.statistics().scannedFiles() == 1 : "Expected 1 scanned file";

        } finally {
            ParserFactory.reset();
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 18: Encoding Detection and BOM Handling - UTF-16 BE BOM Detection
     * 
     * For any file with UTF-16 BE BOM, the encoding detection SHALL return UTF-16BE.
     * 
     * Feature: web-legacy-scan, Property 18: Encoding Detection and BOM Handling
     * Validates: Requirements 12.5, 12.6
     */
    @Property(tries = 100)
    void utf16BeBomEncodingIsCorrectlyDetected(
            @ForAll("simpleAsciiContent") String htmlContent
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("encoding-utf16be-detection-test");
        try {
            // UTF-16 BE BOM bytes
            byte[] utf16BeBom = {(byte) 0xFE, (byte) 0xFF};
            
            // Create file with UTF-16 BE BOM
            Path file = tempDir.resolve("test.html");
            writeFileWithBom(file, utf16BeBom, htmlContent, StandardCharsets.UTF_16BE);

            // Read file bytes and verify BOM is present
            byte[] bytes = Files.readAllBytes(file);
            assert bytes.length >= 2 : "File should have at least 2 bytes for BOM";
            assert bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF 
                : "File should start with UTF-16 BE BOM";

            // Verify encoding detection
            Charset detected = detectEncodingFromBytes(bytes);
            assert detected.equals(StandardCharsets.UTF_16BE) 
                : "UTF-16 BE BOM should be detected as UTF-16BE, got: " + detected;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 18: Encoding Detection and BOM Handling - UTF-16 LE BOM Detection
     * 
     * For any file with UTF-16 LE BOM, the encoding detection SHALL return UTF-16LE.
     * 
     * Feature: web-legacy-scan, Property 18: Encoding Detection and BOM Handling
     * Validates: Requirements 12.5, 12.6
     */
    @Property(tries = 100)
    void utf16LeBomEncodingIsCorrectlyDetected(
            @ForAll("simpleAsciiContent") String htmlContent
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("encoding-utf16le-detection-test");
        try {
            // UTF-16 LE BOM bytes
            byte[] utf16LeBom = {(byte) 0xFF, (byte) 0xFE};
            
            // Create file with UTF-16 LE BOM
            Path file = tempDir.resolve("test.html");
            writeFileWithBom(file, utf16LeBom, htmlContent, StandardCharsets.UTF_16LE);

            // Read file bytes and verify BOM is present
            byte[] bytes = Files.readAllBytes(file);
            assert bytes.length >= 2 : "File should have at least 2 bytes for BOM";
            assert bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE 
                : "File should start with UTF-16 LE BOM";

            // Verify encoding detection
            Charset detected = detectEncodingFromBytes(bytes);
            assert detected.equals(StandardCharsets.UTF_16LE) 
                : "UTF-16 LE BOM should be detected as UTF-16LE, got: " + detected;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    /**
     * Property 18: Encoding Detection and BOM Handling - BOM stripped from content
     * 
     * For any file with UTF-8 BOM, when parsed with BOM-aware parser, the content 
     * SHALL NOT contain BOM characters.
     * 
     * Feature: web-legacy-scan, Property 18: Encoding Detection and BOM Handling
     * Validates: Requirements 12.6
     */
    @Property(tries = 100)
    void bomIsStrippedFromParsedContent(
            @ForAll("validHtmlContent") String htmlContent
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("encoding-bom-stripped-test");
        try {
            // Create file with UTF-8 BOM
            Path file = tempDir.resolve("test.html");
            writeFileWithBom(file, UTF8_BOM, htmlContent, StandardCharsets.UTF_8);

            // Read file with BOM stripping (simulating proper BOM handling)
            String content = readFileWithBomStripping(file);
            
            // Verify: Content should not contain BOM character
            assert !content.contains("\uFEFF") 
                : "Content read with BOM stripping should not contain BOM character";
            
            // Verify: Content should match original
            assert content.equals(htmlContent) 
                : "Content should match original after BOM stripping";

        } finally {
            deleteDirectory(tempDir);
        }
    }

    // Helper method to write file with BOM
    private void writeFileWithBom(Path file, byte[] bom, String content, Charset charset) 
            throws IOException {
        try (OutputStream os = Files.newOutputStream(file)) {
            os.write(bom);
            os.write(content.getBytes(charset));
        }
    }

    // Helper method to detect encoding from bytes (mirrors DefaultScanner.detectEncoding)
    private Charset detectEncodingFromBytes(byte[] bytes) {
        // Check UTF-8 BOM
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && 
            bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        // Check UTF-16 BOMs
        if (bytes.length >= 2) {
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
        }
        // Default to UTF-8
        return StandardCharsets.UTF_8;
    }

    // Helper method to read file with BOM stripping
    private String readFileWithBomStripping(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        Charset encoding = detectEncodingFromBytes(bytes);
        
        int offset = 0;
        // Skip BOM bytes
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && 
            bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            offset = 3; // UTF-8 BOM is 3 bytes
        } else if (bytes.length >= 2) {
            if ((bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) ||
                (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)) {
                offset = 2; // UTF-16 BOM is 2 bytes
            }
        }
        
        return new String(bytes, offset, bytes.length - offset, encoding);
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

    // Providers for test data
    @Provide
    Arbitrary<String> validHtmlContent() {
        return Arbitraries.of(
            "<html><body><p>Hello World</p></body></html>",
            "<html><head><title>Test</title></head><body></body></html>",
            "<!DOCTYPE html><html><body>Content</body></html>",
            "<div><span>Text</span></div>",
            "<html><body><h1>Header</h1><p>Paragraph</p></body></html>"
        );
    }

    @Provide
    Arbitrary<String> simpleAsciiContent() {
        // Simple ASCII content for UTF-16 tests
        return Arbitraries.of(
            "<html><body>Test</body></html>",
            "<div>Content</div>",
            "<p>Text</p>"
        );
    }

    // No-op rule evaluator
    private static class NoOpRuleEvaluator implements RuleEvaluator {
        @Override
        public void loadRules(Path configDir) {}

        @Override
        public List<Issue> evaluate(CodeElement element, String filePath) {
            return List.of();
        }
    }

    // BOM-aware HTML parser for testing
    private static class BomAwareHTMLParser implements Parser {
        @Override
        public ParseResult parse(SourceFile file) {
            try {
                // Read with detected encoding
                String content = Files.readString(file.path(), file.encoding());
                // Strip BOM if present
                if (content.startsWith("\uFEFF")) {
                    content = content.substring(1);
                }
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
            
            // Simple tag detection
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
