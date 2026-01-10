package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.file.FilterConfig;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 扫描配置
 */
public record ScanConfig(
    Path targetPath,
    Path configDir,
    FilterConfig filterConfig,
    Charset defaultEncoding,
    int maxConcurrency,
    boolean strictMode,
    boolean outputAllElements,
    SeverityLevel minSeverity,
    boolean showProgress
) {
    public ScanConfig {
        Objects.requireNonNull(targetPath, "targetPath cannot be null");
        if (filterConfig == null) filterConfig = FilterConfig.defaults();
        if (defaultEncoding == null) defaultEncoding = StandardCharsets.UTF_8;
        if (maxConcurrency <= 0) maxConcurrency = Runtime.getRuntime().availableProcessors();
        if (minSeverity == null) minSeverity = SeverityLevel.INFO;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path targetPath;
        private Path configDir;
        private FilterConfig filterConfig;
        private Charset defaultEncoding = StandardCharsets.UTF_8;
        private int maxConcurrency = Runtime.getRuntime().availableProcessors();
        private boolean strictMode = false;
        private boolean outputAllElements = false;
        private SeverityLevel minSeverity = SeverityLevel.INFO;
        private boolean showProgress = true;

        public Builder targetPath(Path path) {
            this.targetPath = path;
            return this;
        }

        public Builder configDir(Path path) {
            this.configDir = path;
            return this;
        }

        public Builder filterConfig(FilterConfig config) {
            this.filterConfig = config;
            return this;
        }

        public Builder defaultEncoding(Charset encoding) {
            this.defaultEncoding = encoding;
            return this;
        }

        public Builder maxConcurrency(int max) {
            this.maxConcurrency = max;
            return this;
        }

        public Builder strictMode(boolean strict) {
            this.strictMode = strict;
            return this;
        }

        public Builder outputAllElements(boolean output) {
            this.outputAllElements = output;
            return this;
        }

        public Builder minSeverity(SeverityLevel severity) {
            this.minSeverity = severity;
            return this;
        }

        public Builder showProgress(boolean show) {
            this.showProgress = show;
            return this;
        }

        public ScanConfig build() {
            return new ScanConfig(
                targetPath, configDir, filterConfig, defaultEncoding,
                maxConcurrency, strictMode, outputAllElements, minSeverity, showProgress
            );
        }
    }
}
