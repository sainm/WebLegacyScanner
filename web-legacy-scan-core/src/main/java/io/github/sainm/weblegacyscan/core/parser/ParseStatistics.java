package io.github.sainm.weblegacyscan.core.parser;

import java.time.Duration;

/**
 * 解析统计信息
 */
public record ParseStatistics(
    int totalElements,
    int htmlElements,
    int cssElements,
    int jsElements,
    Duration parseTime
) {
    public static ParseStatistics empty() {
        return new ParseStatistics(0, 0, 0, 0, Duration.ZERO);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalElements;
        private int htmlElements;
        private int cssElements;
        private int jsElements;
        private Duration parseTime = Duration.ZERO;

        public Builder totalElements(int count) {
            this.totalElements = count;
            return this;
        }

        public Builder htmlElements(int count) {
            this.htmlElements = count;
            return this;
        }

        public Builder cssElements(int count) {
            this.cssElements = count;
            return this;
        }

        public Builder jsElements(int count) {
            this.jsElements = count;
            return this;
        }

        public Builder parseTime(Duration time) {
            this.parseTime = time;
            return this;
        }

        public ParseStatistics build() {
            return new ParseStatistics(totalElements, htmlElements, cssElements, jsElements, parseTime);
        }
    }
}
