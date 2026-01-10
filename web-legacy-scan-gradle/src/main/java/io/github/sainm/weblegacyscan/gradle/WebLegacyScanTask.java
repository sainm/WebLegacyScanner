package io.github.sainm.weblegacyscan.gradle;

import io.github.sainm.weblegacyscan.core.file.DefaultFileDiscoverer;
import io.github.sainm.weblegacyscan.core.file.FilterConfig;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import io.github.sainm.weblegacyscan.core.scanner.*;
import io.github.sainm.weblegacyscan.parser.css.PhCSSParser;
import io.github.sainm.weblegacyscan.parser.html.JerichoHTMLParser;
import io.github.sainm.weblegacyscan.parser.js.ClosureJSParser;
import io.github.sainm.weblegacyscan.report.*;
import io.github.sainm.weblegacyscan.rules.DefaultRuleEngine;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Gradle scan task.
 */
public abstract class WebLegacyScanTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ListProperty<String> getSourceDirs();

    @InputDirectory
    @Optional
    public abstract DirectoryProperty getConfigDir();

    @OutputDirectory
    public abstract DirectoryProperty getReportDir();

    @Input
    @Optional
    public abstract ListProperty<String> getIncludePatterns();

    @Input
    @Optional
    public abstract ListProperty<String> getExcludePatterns();

    @Input
    @Optional
    public abstract Property<String> getMinSeverity();

    @Input
    @Optional
    public abstract Property<String> getFailOnSeverity();

    @Input
    public abstract Property<Boolean> getFailOnError();

    @Input
    @Optional
    public abstract Property<String> getOutputFormat();

    @Input
    public abstract Property<Boolean> getOutputAllElements();

    @Input
    public abstract Property<Integer> getMaxThreads();

    @TaskAction
    public void scan() {
        // Register parsers
        ParserFactory.registerParser(new JerichoHTMLParser());
        ParserFactory.registerParser(new PhCSSParser());
        ParserFactory.registerParser(new ClosureJSParser());

        // Create components
        DefaultFileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
        DefaultRuleEngine ruleEngine = new DefaultRuleEngine();
        Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEngine);

        // Build filter configuration
        FilterConfig.Builder filterBuilder = FilterConfig.builder();
        if (getIncludePatterns().isPresent() && !getIncludePatterns().get().isEmpty()) {
            filterBuilder.includePatterns(getIncludePatterns().get());
        }
        if (getExcludePatterns().isPresent() && !getExcludePatterns().get().isEmpty()) {
            filterBuilder.excludePatterns(getExcludePatterns().get());
        }

        // Parse severity level
        SeverityLevel minSeverity = SeverityLevel.INFO;
        if (getMinSeverity().isPresent()) {
            try {
                minSeverity = SeverityLevel.valueOf(getMinSeverity().get().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        SeverityLevel failOnSeverity = null;
        if (getFailOnSeverity().isPresent()) {
            try {
                failOnSeverity = SeverityLevel.valueOf(getFailOnSeverity().get().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Scan all source directories
        ScanResult.Builder resultBuilder = ScanResult.builder();
        int totalIssues = 0;
        int totalFiles = 0;

        for (String sourceDir : getSourceDirs().get()) {
            Path sourcePath = getProject().file(sourceDir).toPath();
            if (!Files.exists(sourcePath)) {
                getLogger().warn("Source directory does not exist: {}", sourcePath);
                continue;
            }

            ScanConfig config = ScanConfig.builder()
                .targetPath(sourcePath)
                .configDir(getConfigDir().isPresent() ? getConfigDir().get().getAsFile().toPath() : null)
                .filterConfig(filterBuilder.build())
                .maxConcurrency(getMaxThreads().getOrElse(Runtime.getRuntime().availableProcessors()))
                .outputAllElements(getOutputAllElements().getOrElse(false))
                .minSeverity(minSeverity)
                .build();

            getLogger().lifecycle("Scanning: {}", sourcePath);
            ScanResult result = scanner.scan(config);

            result.issues().forEach(resultBuilder::addIssue);
            totalIssues += result.issues().size();
            totalFiles += result.statistics().scannedFiles();
        }

        ScanResult finalResult = resultBuilder.build();

        // Generate report
        try {
            Path reportDir = getReportDir().get().getAsFile().toPath();
            Files.createDirectories(reportDir);

            OutputFormat format = OutputFormat.HTML;
            if (getOutputFormat().isPresent()) {
                try {
                    format = OutputFormat.valueOf(getOutputFormat().get().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            ReportConfig reportConfig = ReportConfig.builder()
                .format(format)
                .includeAllElements(getOutputAllElements().getOrElse(false))
                .build();

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generate(finalResult, reportConfig);

            String extension = switch (format) {
                case JSON -> ".json";
                case HTML -> ".html";
                case SARIF -> ".sarif.json";
                default -> ".txt";
            };

            Path reportPath = reportDir.resolve("web-legacy-scan" + extension);
            Files.writeString(reportPath, report);
            getLogger().lifecycle("Report written to: {}", reportPath);

        } catch (IOException e) {
            throw new GradleException("Failed to write report: " + e.getMessage(), e);
        }

        // Output summary
        getLogger().lifecycle("Scanned {} files, found {} issues", totalFiles, totalIssues);

        // Check if build should fail
        if (getFailOnError().getOrElse(false) && totalIssues > 0) {
            SeverityLevel effectiveFailOn = failOnSeverity != null ? failOnSeverity : SeverityLevel.WARNING;
            long failCount = finalResult.getIssueCount(effectiveFailOn);
            if (failCount > 0) {
                throw new GradleException(
                    String.format("Found %d issues with severity %s or higher", failCount, effectiveFailOn));
            }
        }
    }
}
