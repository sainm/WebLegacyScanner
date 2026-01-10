package io.github.sainm.weblegacyscan.core.model;

import java.util.Objects;

/**
 * 检测到的问题记�?
 */
public record Issue(
    String ruleId,
    RuleCategory category,
    SeverityLevel severity,
    Location location,
    String description,
    ReplacementSuggestion suggestion,
    String mdnReference
) implements Comparable<Issue> {

    public Issue {
        Objects.requireNonNull(ruleId, "ruleId cannot be null");
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
    }

    @Override
    public int compareTo(Issue other) {
        int pathCompare = location.filePath().compareTo(other.location.filePath());
        if (pathCompare != 0) return pathCompare;
        int lineCompare = Integer.compare(location.startLine(), other.location.startLine());
        if (lineCompare != 0) return lineCompare;
        return Integer.compare(location.startColumn(), other.location.startColumn());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String ruleId;
        private RuleCategory category;
        private SeverityLevel severity = SeverityLevel.WARNING;
        private Location location;
        private String description;
        private ReplacementSuggestion suggestion;
        private String mdnReference;

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder category(RuleCategory category) {
            this.category = category;
            return this;
        }

        public Builder severity(SeverityLevel severity) {
            this.severity = severity;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder suggestion(ReplacementSuggestion suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        public Builder mdnReference(String mdnReference) {
            this.mdnReference = mdnReference;
            return this;
        }

        public Issue build() {
            return new Issue(ruleId, category, severity, location, description, suggestion, mdnReference);
        }
    }
}
