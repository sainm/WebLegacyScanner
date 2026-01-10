package io.github.sainm.weblegacyscan.cli;

import io.github.sainm.weblegacyscan.core.executor.ProgressListener;
import io.github.sainm.weblegacyscan.core.executor.ScanProgress;
import io.github.sainm.weblegacyscan.core.file.DefaultFileDiscoverer;
import io.github.sainm.weblegacyscan.core.file.FilterConfig;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import io.github.sainm.weblegacyscan.core.scanner.*;
import io.github.sainm.weblegacyscan.parser.css.PhCSSParser;
import io.github.sainm.weblegacyscan.parser.html.JerichoHTMLParser;
import io.github.sainm.weblegacyscan.parser.js.ClosureJSParser;
import io.github.sainm.weblegacyscan.report.*;
import io.github.sainm.weblegacyscan.rules.DefaultRuleEngine;
import io.github.sainm.weblegacyscan.rules.RuleEngine;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Web Legacy Scan CLI 主入�?
 */
@Command(
    name = "web-legacy-scan",
    mixinStandardHelpOptions = true,
    version = "web-legacy-scan 1.0.0",
    description = "Scan web files for deprecated HTML, CSS, and JavaScript patterns"
)
public class WebLegacyScanCLI implements Callable<Integer> {

    @Parameters(index = "0", description = "Target path to scan (file or directory)")
    private Path targetPath;

    @Option(names = {"-f", "--format"}, description = "Output format: ${COMPLETION-CANDIDATES}", 
            defaultValue = "TEXT")
    private OutputFormat format;

    @Option(names = {"-c", "--config"}, description = "Configuration directory")
    private Path configDir;

    @Option(names = {"-o", "--output"}, description = "Output file path")
    private Path outputPath;

    @Option(names = {"-s", "--severity"}, description = "Minimum severity: ${COMPLETION-CANDIDATES}",
            defaultValue = "INFO")
    private SeverityLevel minSeverity;

    @Option(names = {"--fail-on"}, description = "Fail on severity: ${COMPLETION-CANDIDATES}")
    private SeverityLevel failOnSeverity;

    @Option(names = {"--strict"}, description = "Strict mode - fail on any parse error")
    private boolean strictMode;

    @Option(names = {"--output-all-elements"}, description = "Output all scanned elements")
    private boolean outputAllElements;

    @Option(names = {"--no-progress"}, description = "Disable progress output")
    private boolean noProgress;

    @Option(names = {"-j", "--threads"}, description = "Max concurrent threads", 
            defaultValue = "0")
    private int maxThreads;

    @Option(names = {"--include"}, description = "Include glob patterns", split = ",")
    private List<String> includePatterns;

    @Option(names = {"--exclude"}, description = "Exclude glob patterns", split = ",")
    private List<String> excludePatterns;

    @Option(names = {"--init"}, description = "Initialize default configuration files")
    private boolean initConfig;

    @Option(names = {"-q", "--quiet"}, description = "Quiet mode - minimal output")
    private boolean quiet;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WebLegacyScanCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            if (initConfig) {
                return initializeConfig();
            }

            return runScan();
        } catch (Exception e) {
            if (!quiet) {
                System.err.println("Error: " + e.getMessage());
            }
            return 2;
        }
    }

    private Integer runScan() throws IOException {
        // 验证目标路径
        if (!Files.exists(targetPath)) {
            System.err.println("Error: Target path does not exist: " + targetPath);
            return 2;
        }

        // 注册解析�?
        ParserFactory.registerParser(new JerichoHTMLParser());
        ParserFactory.registerParser(new PhCSSParser());
        ParserFactory.registerParser(new ClosureJSParser());

        // 创建组件
        DefaultFileDiscoverer fileDiscoverer = new DefaultFileDiscoverer();
        RuleEngine ruleEngine = new DefaultRuleEngine();
        Scanner scanner = new DefaultScanner(fileDiscoverer, ruleEngine);

        // 添加进度监听
        if (!noProgress && !quiet) {
            scanner.addProgressListener(new ConsoleProgressListener());
        }

        // 构建过滤配置
        FilterConfig.Builder filterBuilder = FilterConfig.builder();
        if (includePatterns != null) {
            filterBuilder.includePatterns(includePatterns);
        }
        if (excludePatterns != null) {
            filterBuilder.excludePatterns(excludePatterns);
        }

        // 构建扫描配置
        ScanConfig config = ScanConfig.builder()
            .targetPath(targetPath)
            .configDir(configDir)
            .filterConfig(filterBuilder.build())
            .maxConcurrency(maxThreads > 0 ? maxThreads : Runtime.getRuntime().availableProcessors())
            .strictMode(strictMode)
            .outputAllElements(outputAllElements)
            .minSeverity(minSeverity)
            .showProgress(!noProgress)
            .build();

        // 执行扫描
        if (!quiet) {
            System.out.println("Scanning: " + targetPath);
        }

        ScanResult result = scanner.scan(config);

        // 生成报告
        ReportConfig reportConfig = ReportConfig.builder()
            .format(format)
            .outputPath(outputPath)
            .includeAllElements(outputAllElements)
            .colorOutput(!quiet && outputPath == null)
            .quietMode(quiet)
            .build();

        ReportGenerator generator = new ReportGenerator();
        String report = generator.generate(result, reportConfig);

        // 输出报告
        if (outputPath != null) {
            Files.writeString(outputPath, report);
            if (!quiet) {
                System.out.println("Report written to: " + outputPath);
            }
        } else {
            System.out.println(report);
        }

        // 确定退出码
        return determineExitCode(result);
    }

    private Integer determineExitCode(ScanResult result) {
        if (result.hasErrors() && strictMode) {
            return 2;
        }

        if (failOnSeverity != null && result.getIssueCount(failOnSeverity) > 0) {
            return 1;
        }

        if (result.hasIssues()) {
            return 1;
        }

        return 0;
    }

    private Integer initializeConfig() throws IOException {
        Path configPath = configDir != null ? configDir : Path.of(".");
        Files.createDirectories(configPath);

        // 创建默认 rules.json
        Path rulesPath = configPath.resolve("rules.json");
        if (!Files.exists(rulesPath)) {
            Files.writeString(rulesPath, getDefaultRulesJson());
            System.out.println("Created: " + rulesPath);
        }

        // 创建默认 replacements.json
        Path replacementsPath = configPath.resolve("replacements.json");
        if (!Files.exists(replacementsPath)) {
            Files.writeString(replacementsPath, getDefaultReplacementsJson());
            System.out.println("Created: " + replacementsPath);
        }

        System.out.println("Configuration initialized.");
        return 0;
    }

    private String getDefaultRulesJson() {
        return """
            {
              "rules": [
                {
                  "id": "html-deprecated-tag",
                  "name": "Deprecated HTML Tag",
                  "category": "HTML_DEPRECATED",
                  "severity": "WARNING",
                  "enabled": true,
                  "patterns": ["font", "center", "marquee", "blink", "frame", "frameset"]
                },
                {
                  "id": "css-deprecated-property",
                  "name": "Deprecated CSS Property",
                  "category": "CSS_DEPRECATED",
                  "severity": "WARNING",
                  "enabled": true,
                  "patterns": ["clip", "zoom"]
                },
                {
                  "id": "js-deprecated-api",
                  "name": "Deprecated JavaScript API",
                  "category": "JS_DEPRECATED",
                  "severity": "WARNING",
                  "enabled": true,
                  "patterns": ["escape", "unescape", "document.write"]
                }
              ]
            }
            """;
    }

    private String getDefaultReplacementsJson() {
        return """
            {
              "replacements": {
                "font": {
                  "suggestion": "Use CSS font properties",
                  "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/CSS/font"
                },
                "center": {
                  "suggestion": "Use CSS text-align or flexbox",
                  "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/CSS/text-align"
                },
                "escape": {
                  "suggestion": "Use encodeURIComponent()",
                  "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent"
                },
                "document.write": {
                  "suggestion": "Use DOM manipulation methods",
                  "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/API/Document/createElement"
                }
              }
            }
            """;
    }

    /**
     * 控制台进度监听器
     */
    private static class ConsoleProgressListener implements ProgressListener {
        @Override
        public void onProgress(ScanProgress progress) {
            System.out.printf("\rProgress: %d/%d files (%.1f%%)  ",
                progress.completedTasks(), progress.totalTasks(),
                progress.getPercentage());
        }

        @Override
        public void onFileComplete(java.nio.file.Path file, boolean success) {
            // 不输出单个文件完成信�?
        }

        @Override
        public void onScanComplete(ScanProgress progress) {
            System.out.printf("\rCompleted: %d files in %.2fs%n",
                progress.completedTasks(),
                progress.elapsed().toMillis() / 1000.0);
        }
    }
}
