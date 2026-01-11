package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV format reporter.
 * Supports two report types:
 * 1. FULL - all parsed elements (HTML/JSP tags, CSS, JS) - outputs separate CSV for each type
 * 2. DEPRECATED - only deprecated/obsolete elements (issues)
 */
public final class CSVFormatter implements ReportFormatter {

    private static final String SEP = ",";
    private static final String NL = "\n";
    private static final int MAX_CONTENT_LENGTH = 500; // Increased from 80

    // Store separated reports for multi-file output
    private String htmlReport;
    private String cssReport;
    private String jsReport;

    @Override
    public String format(ScanResult result, ReportConfig config) {
        return switch (config.reportType()) {
            case FULL -> formatFullReport(result);
            case DEPRECATED -> formatDeprecatedReport(result);
        };
    }

    @Override
    public OutputFormat getFormat() {
        return OutputFormat.CSV;
    }

    /**
     * Get HTML elements report (call after format())
     */
    public String getHtmlReport() {
        return htmlReport;
    }

    /**
     * Get CSS elements report (call after format())
     */
    public String getCssReport() {
        return cssReport;
    }

    /**
     * Get JS elements report (call after format())
     */
    public String getJsReport() {
        return jsReport;
    }

    private String formatFullReport(ScanResult result) {
        // Separate elements by type
        List<HTMLElement> htmlElements = new ArrayList<>();
        List<CSSElement> cssElements = new ArrayList<>();
        List<JSElement> jsElements = new ArrayList<>();
        
        for (CodeElement el : result.allElements()) {
            if (el instanceof HTMLElement html) {
                htmlElements.add(html);
            } else if (el instanceof CSSElement css) {
                cssElements.add(css);
            } else if (el instanceof JSElement js) {
                jsElements.add(js);
            }
        }
        
        // Generate separate reports
        htmlReport = formatHtmlReport(htmlElements);
        cssReport = formatCssReport(cssElements);
        jsReport = formatJsReport(jsElements);
        
        // Return combined report for backward compatibility
        StringBuilder sb = new StringBuilder();
        sb.append(htmlReport);
        sb.append(NL).append(cssReport);
        sb.append(NL).append(jsReport);
        return sb.toString();
    }

    private String formatHtmlReport(List<HTMLElement> elements) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinCSV("File", "Line", "Column", "ElementType", "TagName", "AttrName", "AttrValue", "Content")).append(NL);
        for (HTMLElement html : elements) {
            // If element has attributes, output each attribute as a separate row
            if (html.attributes() != null && !html.attributes().isEmpty()) {
                for (Map.Entry<String, AttributeInfo> entry : html.attributes().entrySet()) {
                    sb.append(formatHTMLAttrRow(html, entry.getKey(), entry.getValue())).append(NL);
                }
            } else {
                // No attributes, output single row with empty attr columns
                sb.append(formatHTMLRowNoAttr(html)).append(NL);
            }
        }
        return sb.toString();
    }

    private String formatHTMLAttrRow(HTMLElement html, String attrName, AttributeInfo attrInfo) {
        // Use attribute location if available, otherwise use element location
        Location loc = attrInfo.fullLocation() != null ? attrInfo.fullLocation() : html.location();
        
        return joinCSV(
            loc.filePath().toString(),
            String.valueOf(loc.startLine()),
            String.valueOf(loc.startColumn()),
            html.elementType().name(),
            html.tagName(),
            attrName,
            attrInfo.value() != null ? attrInfo.value() : "",
            truncate(html.rawContent())
        );
    }

    private String formatHTMLRowNoAttr(HTMLElement html) {
        Location loc = html.location();
        return joinCSV(
            loc.filePath().toString(),
            String.valueOf(loc.startLine()),
            String.valueOf(loc.startColumn()),
            html.elementType().name(),
            html.tagName(),
            "",
            "",
            truncate(html.rawContent())
        );
    }

    private String formatCssReport(List<CSSElement> elements) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinCSV("File", "Line", "Column", "ElementType", "Selector", "Property", "Value", "Content")).append(NL);
        for (CSSElement css : elements) {
            // Flatten: output each declaration as a separate row
            flattenCssElement(css, sb, null);
        }
        return sb.toString();
    }

    private void flattenCssElement(CSSElement css, StringBuilder sb, String parentSelector) {
        String selector = css.selector() != null ? css.selector() : parentSelector;
        
        if (css.type() == CSSElementType.DECLARATION) {
            // Output declaration with parent selector
            sb.append(formatCSSDeclarationRow(css, selector)).append(NL);
        } else {
            // For rule sets, media queries, etc., output children
            if (css.children() != null && !css.children().isEmpty()) {
                for (CSSElement child : css.children()) {
                    flattenCssElement(child, sb, selector);
                }
            } else {
                // No children, output the rule itself
                sb.append(formatCSSRow(css)).append(NL);
            }
        }
    }

    private String formatCSSDeclarationRow(CSSElement css, String selector) {
        Location loc = css.location();
        return joinCSV(
            loc.filePath().toString(),
            String.valueOf(loc.startLine()),
            String.valueOf(loc.startColumn()),
            css.type().name(),
            selector != null ? selector : "",
            css.property() != null ? css.property() : "",
            css.value() != null ? css.value() : "",
            truncate(css.rawContent())
        );
    }

    private String formatJsReport(List<JSElement> elements) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinCSV("File", "Line", "Column", "ElementType", "Identifier", "Arguments", "Content")).append(NL);
        for (JSElement js : elements) {
            sb.append(formatJSRow(js)).append(NL);
        }
        return sb.toString();
    }

    private String formatDeprecatedReport(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinCSV("File", "Line", "Column", "RuleID", "Severity", "Category", "Message", "Suggestion", "Content")).append(NL);
        
        List<Issue> sorted = result.issues().stream()
            .sorted(Comparator.comparing((Issue i) -> i.location().filePath().toString())
                .thenComparingInt(i -> i.location().startLine()))
            .toList();
        
        for (Issue issue : sorted) {
            Location loc = issue.location();
            String suggestion = issue.suggestion() != null ? issue.suggestion().modernAlternative() : "";
            sb.append(joinCSV(
                loc.filePath().toString(),
                String.valueOf(loc.startLine()),
                String.valueOf(loc.startColumn()),
                issue.ruleId(),
                issue.severity().name(),
                issue.category().name(),
                issue.description(),
                suggestion,
                truncate(loc.sourceSnippet(), 80)
            )).append(NL);
        }
        
        return sb.toString();
    }

    private String formatCSSRow(CSSElement css) {
        Location loc = css.location();
        return joinCSV(
            loc.filePath().toString(),
            String.valueOf(loc.startLine()),
            String.valueOf(loc.startColumn()),
            css.type().name(),
            css.selector() != null ? css.selector() : "",
            css.property() != null ? css.property() : "",
            css.value() != null ? css.value() : "",
            truncate(css.rawContent())
        );
    }

    private String formatJSRow(JSElement js) {
        Location loc = js.location();
        String args = js.arguments().stream()
            .map(JSArgument::value)
            .collect(Collectors.joining("; "));
        
        return joinCSV(
            loc.filePath().toString(),
            String.valueOf(loc.startLine()),
            String.valueOf(loc.startColumn()),
            js.type().name(),
            js.identifier() != null ? js.identifier() : "",
            args,
            truncate(js.rawContent())
        );
    }

    private String joinCSV(String... values) {
        return Arrays.stream(values)
            .map(this::escapeCSV)
            .collect(Collectors.joining(SEP));
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\r", "");
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String truncate(String s) {
        return truncate(s, MAX_CONTENT_LENGTH);
    }
}
