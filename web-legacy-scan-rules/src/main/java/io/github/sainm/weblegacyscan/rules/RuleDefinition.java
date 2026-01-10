package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.RuleCategory;
import io.github.sainm.weblegacyscan.core.model.ReplacementSuggestion;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;

import java.util.List;
import java.util.Objects;

/**
 * 规则定义
 */
public record RuleDefinition(
    String id,
    RuleCategory category,
    SeverityLevel severity,
    String pattern,
    String description,
    ReplacementSuggestion suggestion,
    String mdnReference,
    boolean enabled,
    List<String> filePatterns
) {
    public RuleDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        severity = severity != null ? severity : SeverityLevel.WARNING;
        filePatterns = filePatterns != null ? List.copyOf(filePatterns) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean matchesFile(String filePath) {
        if (filePatterns.isEmpty()) return true;
        for (String pattern : filePatterns) {
            if (matchesGlob(filePath, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String path, String pattern) {
        // Normalize path separators to forward slashes
        String normalizedPath = path.replace("\\", "/");
        
        // Build regex from glob pattern
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                    // ** matches any path including empty
                    if (i + 2 < pattern.length() && pattern.charAt(i + 2) == '/') {
                        // **/ matches zero or more directories
                        regex.append("(?:.*/)?");
                        i += 3;
                    } else {
                        // ** at end matches everything
                        regex.append(".*");
                        i += 2;
                    }
                } else {
                    // * matches any characters except /
                    regex.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                // ? matches any single character except /
                regex.append("[^/]");
                i++;
            } else if (c == '.') {
                regex.append("\\.");
                i++;
            } else if (c == '/') {
                regex.append("/");
                i++;
            } else if ("[]{}()^$|+\\".indexOf(c) >= 0) {
                // Escape regex special characters
                regex.append("\\").append(c);
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }
        
        return normalizedPath.matches(regex.toString());
    }

    public static class Builder {
        private String id;
        private RuleCategory category;
        private SeverityLevel severity = SeverityLevel.WARNING;
        private String pattern;
        private String description;
        private ReplacementSuggestion suggestion;
        private String mdnReference;
        private boolean enabled = true;
        private List<String> filePatterns = List.of();

        public Builder id(String id) {
            this.id = id;
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

        public Builder pattern(String pattern) {
            this.pattern = pattern;
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

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder filePatterns(List<String> filePatterns) {
            this.filePatterns = filePatterns;
            return this;
        }

        public RuleDefinition build() {
            return new RuleDefinition(id, category, severity, pattern, description,
                suggestion, mdnReference, enabled, filePatterns);
        }
    }
}
