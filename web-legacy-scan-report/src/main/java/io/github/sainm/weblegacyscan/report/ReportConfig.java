package io.github.sainm.weblegacyscan.report;

import java.nio.file.Path;

/**
 * Report configuration
 */
public record ReportConfig(
    OutputFormat format,
    ReportType reportType,
    Path outputPath,
    boolean includeSnippets,
    boolean colorOutput,
    boolean quietMode,
    boolean outputAllElements
) {
    public ReportConfig {
        format = format != null ? format : OutputFormat.TEXT;
        reportType = reportType != null ? reportType : ReportType.DEPRECATED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OutputFormat format = OutputFormat.TEXT;
        private ReportType reportType = ReportType.DEPRECATED;
        private Path outputPath;
        private boolean includeSnippets = true;
        private boolean colorOutput = true;
        private boolean quietMode = false;
        private boolean outputAllElements = false;

        public Builder format(OutputFormat format) {
            this.format = format;
            return this;
        }

        public Builder reportType(ReportType type) {
            this.reportType = type;
            return this;
        }

        public Builder outputPath(Path path) {
            this.outputPath = path;
            return this;
        }

        public Builder includeSnippets(boolean include) {
            this.includeSnippets = include;
            return this;
        }

        public Builder colorOutput(boolean color) {
            this.colorOutput = color;
            return this;
        }

        public Builder quietMode(boolean quiet) {
            this.quietMode = quiet;
            return this;
        }

        public Builder outputAllElements(boolean outputAll) {
            this.outputAllElements = outputAll;
            return this;
        }

        public ReportConfig build() {
            return new ReportConfig(format, reportType, outputPath, 
                                   includeSnippets, colorOutput, quietMode, outputAllElements);
        }
    }
}
