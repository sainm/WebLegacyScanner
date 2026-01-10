package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.model.CodeElement;
import io.github.sainm.weblegacyscan.core.model.Issue;
import io.github.sainm.weblegacyscan.core.model.RuleCategory;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import io.github.sainm.weblegacyscan.core.parser.ParseError;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 扫描结果
 */
public record ScanResult(
    List<Issue> issues,
    List<CodeElement> allElements,
    ScanStatistics statistics,
    List<ScanError> errors
) {
    public ScanResult {
        issues = issues != null ? List.copyOf(issues) : List.of();
        allElements = allElements != null ? List.copyOf(allElements) : List.of();
        errors = errors != null ? List.copyOf(errors) : List.of();
        statistics = statistics != null ? statistics : ScanStatistics.empty();
    }

    public List<Issue> getSortedIssues() {
        return issues.stream()
            .sorted()
            .collect(Collectors.toList());
    }

    public Map<SeverityLevel, Long> getIssuesBySeverity() {
        return issues.stream()
            .collect(Collectors.groupingBy(Issue::severity, Collectors.counting()));
    }

    public Map<RuleCategory, Long> getIssuesByCategory() {
        return issues.stream()
            .collect(Collectors.groupingBy(Issue::category, Collectors.counting()));
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public long getIssueCount(SeverityLevel minSeverity) {
        return issues.stream()
            .filter(i -> i.severity().isAtLeast(minSeverity))
            .count();
    }

    public record ScanStatistics(
        int totalFiles,
        int scannedFiles,
        int totalElements,
        Duration scanTime,
        double filesPerSecond
    ) {
        public static ScanStatistics empty() {
            return new ScanStatistics(0, 0, 0, Duration.ZERO, 0);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int totalFiles;
            private int scannedFiles;
            private int totalElements;
            private Duration scanTime = Duration.ZERO;

            public Builder totalFiles(int count) {
                this.totalFiles = count;
                return this;
            }

            public Builder scannedFiles(int count) {
                this.scannedFiles = count;
                return this;
            }

            public Builder totalElements(int count) {
                this.totalElements = count;
                return this;
            }

            public Builder scanTime(Duration time) {
                this.scanTime = time;
                return this;
            }

            public ScanStatistics build() {
                double fps = scanTime.toMillis() > 0 
                    ? (scannedFiles * 1000.0 / scanTime.toMillis()) 
                    : 0;
                return new ScanStatistics(totalFiles, scannedFiles, totalElements, scanTime, fps);
            }
        }
    }

    public record ScanError(
        String filePath,
        String message,
        boolean fatal
    ) {
        public static ScanError from(ParseError error) {
            return new ScanError(
                error.filePath().toString(),
                error.message(),
                error.isFatal()
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Issue> issues = new ArrayList<>();
        private List<CodeElement> allElements = new ArrayList<>();
        private ScanStatistics statistics;
        private List<ScanError> errors = new ArrayList<>();

        public Builder issues(List<Issue> issues) {
            this.issues = new ArrayList<>(issues);
            return this;
        }

        public Builder addIssue(Issue issue) {
            this.issues.add(issue);
            return this;
        }

        public Builder allElements(List<CodeElement> elements) {
            this.allElements = new ArrayList<>(elements);
            return this;
        }

        public Builder statistics(ScanStatistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder errors(List<ScanError> errors) {
            this.errors = new ArrayList<>(errors);
            return this;
        }

        public Builder addError(ScanError error) {
            this.errors.add(error);
            return this;
        }

        public ScanResult build() {
            return new ScanResult(issues, allElements, statistics, errors);
        }
    }
}
