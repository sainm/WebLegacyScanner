package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.Issue;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;

/**
 * Text format report generator.
 */
public final class TextFormatter implements ReportFormatter {

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String BOLD = "\u001B[1m";

    @Override
    public String format(ScanResult result, ReportConfig config) {
        StringBuilder sb = new StringBuilder();
        boolean color = config.colorOutput();

        if (config.quietMode()) {
            return formatQuiet(result, color);
        }

        // Title
        sb.append(color ? BOLD : "").append("Web Legacy Scan Report")
          .append(color ? RESET : "").append("\n");
        sb.append("=".repeat(50)).append("\n\n");

        if (result.issues().isEmpty()) {
            sb.append(color ? GREEN : "").append("✓ No issues found!")
              .append(color ? RESET : "").append("\n");
        } else {
            // Issues list
            for (Issue issue : result.getSortedIssues()) {
                formatIssue(sb, issue, config);
            }
        }

        // Summary
        sb.append("\n").append("-".repeat(50)).append("\n");
        sb.append(color ? BOLD : "").append("Summary")
          .append(color ? RESET : "").append("\n");
        
        sb.append(String.format("  Files scanned: %d / %d%n", 
            result.statistics().scannedFiles(), 
            result.statistics().totalFiles()));
        sb.append(String.format("  Scan time: %.2f seconds%n", 
            result.statistics().scanTime().toMillis() / 1000.0));
        sb.append(String.format("  Throughput: %.1f files/sec%n", 
            result.statistics().filesPerSecond()));
        sb.append(String.format("  Total issues: %d%n", result.issues().size()));

        // By severity
        result.getIssuesBySeverity().forEach((severity, count) -> {
            String severityColor = getSeverityColor(severity, color);
            sb.append(String.format("    %s%s%s: %d%n", 
                severityColor, severity.getDisplayName(), 
                color ? RESET : "", count));
        });

        // Errors
        if (!result.errors().isEmpty()) {
            sb.append("\n").append(color ? RED : "").append("Errors:")
              .append(color ? RESET : "").append("\n");
            for (ScanResult.ScanError error : result.errors()) {
                sb.append(String.format("  %s: %s%n", error.filePath(), error.message()));
            }
        }

        return sb.toString();
    }

    private void formatIssue(StringBuilder sb, Issue issue, ReportConfig config) {
        boolean color = config.colorOutput();
        String severityColor = getSeverityColor(issue.severity(), color);

        // Location
        sb.append(String.format("%s:%d:%d: ", 
            issue.location().filePath(),
            issue.location().startLine(),
            issue.location().startColumn()));

        // Severity
        sb.append(severityColor)
          .append(issue.severity().getDisplayName())
          .append(color ? RESET : "")
          .append(" ");

        // Rule ID
        sb.append(color ? CYAN : "")
          .append("[").append(issue.ruleId()).append("]")
          .append(color ? RESET : "")
          .append(" ");

        // Description
        sb.append(issue.description()).append("\n");

        // Code snippet
        if (config.includeSnippets() && issue.location().sourceSnippet() != null) {
            String snippet = issue.location().sourceSnippet();
            if (snippet.length() > 100) {
                snippet = snippet.substring(0, 100) + "...";
            }
            sb.append("    ").append(snippet.replace("\n", " ")).append("\n");
        }

        // Replacement suggestion
        if (issue.suggestion() != null) {
            sb.append(color ? GREEN : "")
              .append("    ✓ ").append(issue.suggestion().description())
              .append(color ? RESET : "").append("\n");
        }

        // MDN link
        if (issue.mdnReference() != null) {
            sb.append("    See: ").append(issue.mdnReference()).append("\n");
        }

        sb.append("\n");
    }

    private String formatQuiet(ScanResult result, boolean color) {
        StringBuilder sb = new StringBuilder();
        
        long errors = result.getIssueCount(SeverityLevel.ERROR);
        long warnings = result.getIssueCount(SeverityLevel.WARNING);
        long infos = result.issues().size() - errors - warnings;

        if (result.issues().isEmpty()) {
            sb.append(color ? GREEN : "").append("✓ No issues")
              .append(color ? RESET : "");
        } else {
            sb.append(String.format("%d issues: ", result.issues().size()));
            if (errors > 0) {
                sb.append(color ? RED : "").append(errors).append(" errors")
                  .append(color ? RESET : "");
            }
            if (warnings > 0) {
                if (errors > 0) sb.append(", ");
                sb.append(color ? YELLOW : "").append(warnings).append(" warnings")
                  .append(color ? RESET : "");
            }
            if (infos > 0) {
                if (errors > 0 || warnings > 0) sb.append(", ");
                sb.append(color ? CYAN : "").append(infos).append(" info")
                  .append(color ? RESET : "");
            }
        }

        sb.append(String.format(" (%d files, %.2fs)", 
            result.statistics().scannedFiles(),
            result.statistics().scanTime().toMillis() / 1000.0));

        return sb.toString();
    }

    private String getSeverityColor(SeverityLevel severity, boolean color) {
        if (!color) return "";
        return switch (severity) {
            case ERROR -> RED;
            case WARNING -> YELLOW;
            case INFO -> CYAN;
        };
    }

    @Override
    public OutputFormat getFormat() {
        return OutputFormat.TEXT;
    }
}
