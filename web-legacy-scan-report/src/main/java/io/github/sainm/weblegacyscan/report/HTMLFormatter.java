package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.Issue;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;

/**
 * HTML format report generator.
 */
public final class HTMLFormatter implements ReportFormatter {

    @Override
    public String format(ScanResult result, ReportConfig config) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Web Legacy Scan Report</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; }
                    .summary { background: #f5f5f5; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
                    .issue { border: 1px solid #ddd; padding: 15px; margin-bottom: 10px; border-radius: 8px; }
                    .issue.error { border-left: 4px solid #dc3545; }
                    .issue.warning { border-left: 4px solid #ffc107; }
                    .issue.info { border-left: 4px solid #17a2b8; }
                    .severity { font-weight: bold; text-transform: uppercase; }
                    .severity.error { color: #dc3545; }
                    .severity.warning { color: #ffc107; }
                    .severity.info { color: #17a2b8; }
                    .location { color: #666; font-family: monospace; }
                    .snippet { background: #f8f9fa; padding: 10px; border-radius: 4px; font-family: monospace; overflow-x: auto; }
                    .suggestion { color: #28a745; margin-top: 10px; }
                    .mdn-link { color: #007bff; }
                    h1 { color: #333; }
                    .stats { display: flex; gap: 20px; flex-wrap: wrap; }
                    .stat { background: white; padding: 10px 15px; border-radius: 4px; }
                </style>
            </head>
            <body>
                <h1>Web Legacy Scan Report</h1>
            """);

        // Summary
        sb.append("<div class=\"summary\">");
        sb.append("<h2>Summary</h2>");
        sb.append("<div class=\"stats\">");
        sb.append(String.format("<div class=\"stat\">Files: %d / %d</div>", 
            result.statistics().scannedFiles(), result.statistics().totalFiles()));
        sb.append(String.format("<div class=\"stat\">Issues: %d</div>", result.issues().size()));
        sb.append(String.format("<div class=\"stat\">Time: %.2fs</div>", 
            result.statistics().scanTime().toMillis() / 1000.0));
        sb.append("</div>");

        // By severity
        sb.append("<h3>By Severity</h3><ul>");
        result.getIssuesBySeverity().forEach((severity, count) -> 
            sb.append(String.format("<li><span class=\"severity %s\">%s</span>: %d</li>",
                severity.getDisplayName(), severity.getDisplayName(), count)));
        sb.append("</ul></div>");

        // Issues list
        if (result.issues().isEmpty()) {
            sb.append("<p style=\"color: #28a745; font-size: 1.2em;\">✓ No issues found!</p>");
        } else {
            sb.append("<h2>Issues</h2>");
            for (Issue issue : result.getSortedIssues()) {
                formatIssue(sb, issue, config);
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private void formatIssue(StringBuilder sb, Issue issue, ReportConfig config) {
        String severityClass = issue.severity().getDisplayName();
        
        sb.append(String.format("<div class=\"issue %s\">", severityClass));
        
        // Location and severity
        sb.append(String.format("<div><span class=\"location\">%s:%d:%d</span> ",
            escapeHtml(issue.location().filePath().toString()),
            issue.location().startLine(),
            issue.location().startColumn()));
        sb.append(String.format("<span class=\"severity %s\">[%s]</span> ",
            severityClass, issue.severity().getDisplayName()));
        sb.append(String.format("<code>%s</code></div>", escapeHtml(issue.ruleId())));

        // Description
        sb.append(String.format("<p>%s</p>", escapeHtml(issue.description())));

        // Code snippet
        if (config.includeSnippets() && issue.location().sourceSnippet() != null) {
            String snippet = issue.location().sourceSnippet();
            if (snippet.length() > 200) {
                snippet = snippet.substring(0, 200) + "...";
            }
            sb.append(String.format("<pre class=\"snippet\">%s</pre>", escapeHtml(snippet)));
        }

        // Replacement suggestion
        if (issue.suggestion() != null) {
            sb.append(String.format("<div class=\"suggestion\">✓ %s</div>", 
                escapeHtml(issue.suggestion().description())));
        }

        // MDN link
        if (issue.mdnReference() != null) {
            sb.append(String.format("<a class=\"mdn-link\" href=\"%s\" target=\"_blank\">MDN Documentation</a>",
                escapeHtml(issue.mdnReference())));
        }

        sb.append("</div>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    @Override
    public OutputFormat getFormat() {
        return OutputFormat.HTML;
    }
}
