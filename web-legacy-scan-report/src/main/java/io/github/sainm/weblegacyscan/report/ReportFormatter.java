package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.scanner.ScanResult;

/**
 * Report formatter interface.
 */
public sealed interface ReportFormatter 
    permits JSONFormatter, TextFormatter, HTMLFormatter, SARIFFormatter, CSVFormatter {
    
    /**
     * Format scan result.
     * @param result scan result
     * @param config report configuration
     * @return formatted string
     */
    String format(ScanResult result, ReportConfig config);

    /**
     * Get output format.
     */
    OutputFormat getFormat();
}
