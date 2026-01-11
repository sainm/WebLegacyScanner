package io.github.sainm.weblegacyscan.cli;

import io.github.sainm.weblegacyscan.core.file.*;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;
import io.github.sainm.weblegacyscan.core.scanner.*;
import io.github.sainm.weblegacyscan.parser.html.JerichoHTMLParser;
import io.github.sainm.weblegacyscan.parser.css.PhCSSParser;
import io.github.sainm.weblegacyscan.parser.js.ClosureJSParser;
import io.github.sainm.weblegacyscan.report.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command line scanner tool
 * Usage: java ScannerMain <directory> [--format csv|json|html] [--type full|deprecated] [--output file]
 */
public class ScannerMain {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        // Handle --help and --version first
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                return;
            }
            if ("--version".equals(arg) || "-v".equals(arg)) {
                printVersion();
                return;
            }
        }

        String directory = args[0];
        OutputFormat format = OutputFormat.CSV;
        ReportType reportType = ReportType.FULL;
        String outputFile = null;
        boolean outputAllElements = false;

        // Parse arguments
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--format", "-f" -> {
                    if (i + 1 < args.length) format = OutputFormat.fromString(args[++i]);
                }
                case "--type", "-t" -> {
                    if (i + 1 < args.length) reportType = ReportType.fromString(args[++i]);
                }
                case "--output", "-o" -> {
                    if (i + 1 < args.length) outputFile = args[++i];
                }
                case "--output-all-elements" -> {
                    outputAllElements = true;
                }
            }
        }

        try {
            new ScannerMain().run(directory, format, reportType, outputFile, outputAllElements);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printVersion() {
        System.out.println("Web Legacy Scanner v1.0.0");
    }

    private static void printUsage() {
        System.out.println("Web Legacy Scanner - Analyze JSP/HTML/CSS/JS files");
        System.out.println();
        System.out.println("Usage: java -jar scanner.jar <directory> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --format, -f <format>     Output format: csv, json, html, text (default: csv)");
        System.out.println("  --type, -t <type>         Report type: full, deprecated (default: full)");
        System.out.println("  --output, -o <file>       Output file (default: stdout)");
        System.out.println("  --output-all-elements     Export all parsed code elements with locations");
        System.out.println("  --help, -h                Show this help message");
        System.out.println("  --version, -v             Show version information");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar scanner.jar D:\\project\\WEB-INF --format csv --type full -o report.csv");
        System.out.println("  java -jar scanner.jar ./webapp --format json --type deprecated");
        System.out.println("  java -jar scanner.jar ./webapp --format json --output-all-elements -o elements.json");
    }

    public void run(String directory, OutputFormat format, ReportType reportType, String outputFile, boolean outputAllElements) throws Exception {
        Path scanPath = Path.of(directory);
        if (!Files.exists(scanPath)) {
            throw new IllegalArgumentException("Directory not found: " + directory);
        }

        System.out.println("Scanning: " + scanPath.toAbsolutePath());
        System.out.println("Format: " + format.getName() + ", Type: " + reportType.getName());
        System.out.println();

        // Register parsers
        registerParsers();

        // Discover files
        System.out.println("Discovering files...");
        List<SourceFile> files = discoverFiles(scanPath);
        System.out.println("Found " + files.size() + " files");

        // Parse files
        System.out.println("Parsing files...");
        Instant start = Instant.now();
        List<CodeElement> allElements = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        int parsedCount = 0;

        for (SourceFile file : files) {
            try {
                Optional<Parser> parser = ParserFactory.getParser(file.type());
                if (parser.isPresent()) {
                    ParseResult result = parser.get().parse(file);
                    allElements.addAll(result.elements());
                    parsedCount++;
                    
                    if (parsedCount % 100 == 0) {
                        System.out.println("  Parsed " + parsedCount + " files...");
                    }
                }
            } catch (Exception e) {
                System.err.println("  Error parsing " + file.path() + ": " + e.getMessage());
            }
        }

        Duration scanTime = Duration.between(start, Instant.now());
        System.out.println("Parsed " + parsedCount + " files in " + scanTime.toMillis() + "ms");
        System.out.println("Found " + allElements.size() + " elements");

        // Build scan result
        ScanResult.ScanStatistics stats = ScanResult.ScanStatistics.builder()
            .totalFiles(files.size())
            .scannedFiles(parsedCount)
            .totalElements(allElements.size())
            .scanTime(scanTime)
            .build();

        ScanResult result = ScanResult.builder()
            .allElements(allElements)
            .issues(issues)
            .statistics(stats)
            .build();

        // Generate report
        System.out.println("Generating report...");
        ReportConfig config = ReportConfig.builder()
            .format(format)
            .reportType(reportType)
            .includeSnippets(true)
            .outputAllElements(outputAllElements)
            .build();

        ReportFormatter formatter = getFormatter(format);
        String report = formatter.format(result, config);

        // Output
        if (outputFile != null) {
            // For CSV format with FULL type, output separate files
            if (format == OutputFormat.CSV && reportType == ReportType.FULL && formatter instanceof CSVFormatter csvFormatter) {
                String baseName = outputFile.replaceAll("\\.[^.]+$", ""); // Remove extension
                String ext = outputFile.contains(".") ? outputFile.substring(outputFile.lastIndexOf(".")) : ".csv";
                
                // Write HTML report
                String htmlFile = baseName + "-html" + ext;
                Files.writeString(Path.of(htmlFile), csvFormatter.getHtmlReport(), StandardCharsets.UTF_8);
                System.out.println("HTML report saved to: " + htmlFile);
                
                // Write CSS report
                String cssFile = baseName + "-css" + ext;
                Files.writeString(Path.of(cssFile), csvFormatter.getCssReport(), StandardCharsets.UTF_8);
                System.out.println("CSS report saved to: " + cssFile);
                
                // Write JS report
                String jsFile = baseName + "-js" + ext;
                Files.writeString(Path.of(jsFile), csvFormatter.getJsReport(), StandardCharsets.UTF_8);
                System.out.println("JS report saved to: " + jsFile);
            } else {
                Files.writeString(Path.of(outputFile), report, StandardCharsets.UTF_8);
                System.out.println("Report saved to: " + outputFile);
            }
        } else {
            System.out.println();
            System.out.println("=== Report ===");
            System.out.println(report);
        }

        // Summary
        printSummary(allElements);
    }

    private void registerParsers() {
        ParserFactory.reset();
        ParserFactory.registerParser(new JerichoHTMLParser());
        try {
            ParserFactory.registerParser(new PhCSSParser());
        } catch (Exception e) {
            System.err.println("Warning: CSS parser not available");
        }
        try {
            ParserFactory.registerParser(new ClosureJSParser());
        } catch (Exception e) {
            System.err.println("Warning: JS parser not available");
        }
    }

    private List<SourceFile> discoverFiles(Path root) throws IOException {
        List<SourceFile> files = new ArrayList<>();
        Set<String> extensions = Set.of(".jsp", ".jspx", ".html", ".htm", ".css", ".js");

        Files.walk(root)
            .filter(Files::isRegularFile)
            .filter(p -> {
                String name = p.getFileName().toString().toLowerCase();
                return extensions.stream().anyMatch(name::endsWith);
            })
            .forEach(p -> files.add(SourceFile.of(p)));

        return files;
    }

    private ReportFormatter getFormatter(OutputFormat format) {
        return switch (format) {
            case CSV -> new CSVFormatter();
            case JSON -> new JSONFormatter();
            case HTML -> new HTMLFormatter();
            case TEXT -> new TextFormatter();
            case SARIF -> new SARIFFormatter();
        };
    }

    private void printSummary(List<CodeElement> elements) {
        System.out.println();
        System.out.println("=== Summary ===");
        
        Map<String, Long> byType = elements.stream()
            .collect(Collectors.groupingBy(e -> e.getClass().getSimpleName(), Collectors.counting()));
        
        byType.forEach((type, count) -> System.out.println("  " + type + ": " + count));

        // HTML element types breakdown
        Map<HTMLElementType, Long> htmlTypes = elements.stream()
            .filter(e -> e instanceof HTMLElement)
            .map(e -> ((HTMLElement) e).elementType())
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        if (!htmlTypes.isEmpty()) {
            System.out.println();
            System.out.println("HTML/JSP Element Types:");
            htmlTypes.entrySet().stream()
                .sorted(Map.Entry.<HTMLElementType, Long>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
        }
    }
}
