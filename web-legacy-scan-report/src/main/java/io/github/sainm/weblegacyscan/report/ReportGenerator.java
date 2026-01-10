package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.scanner.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Report generator.
 */
public class ReportGenerator {

    private final Map<OutputFormat, ReportFormatter> formatters = Map.of(
        OutputFormat.JSON, new JSONFormatter(),
        OutputFormat.TEXT, new TextFormatter(),
        OutputFormat.HTML, new HTMLFormatter(),
        OutputFormat.SARIF, new SARIFFormatter()
    );

    public String generate(ScanResult result, ReportConfig config) {
        ReportFormatter formatter = formatters.get(config.format());
        if (formatter == null) {
            formatter = formatters.get(OutputFormat.TEXT);
        }
        return formatter.format(result, config);
    }

    public void generateToFile(ScanResult result, ReportConfig config) throws IOException {
        String content = generate(result, config);
        
        if (config.outputPath() != null) {
            Path parent = config.outputPath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(config.outputPath(), content);
        }
    }

    public ReportFormatter getFormatter(OutputFormat format) {
        return formatters.get(format);
    }
}
