package io.github.sainm.weblegacyscan.core.file;

import java.util.List;
import java.util.Set;

/**
 * 文件过滤配置
 */
public record FilterConfig(
    Set<String> includeExtensions,
    List<String> includePatterns,
    List<String> excludePatterns,
    boolean respectGitignore,
    long maxFileSizeBytes
) {
    public static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    public static final Set<String> DEFAULT_EXTENSIONS = Set.of(
        ".html", ".htm", ".jsp", ".jspx",
        ".css", ".scss", ".less",
        ".js", ".mjs", ".jsx",
        ".ts", ".tsx"
    );

    public FilterConfig {
        includeExtensions = includeExtensions != null ? Set.copyOf(includeExtensions) : DEFAULT_EXTENSIONS;
        includePatterns = includePatterns != null ? List.copyOf(includePatterns) : List.of();
        excludePatterns = excludePatterns != null ? List.copyOf(excludePatterns) : List.of();
        if (maxFileSizeBytes <= 0) maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE;
    }

    public static FilterConfig defaults() {
        return new FilterConfig(DEFAULT_EXTENSIONS, List.of(), List.of(), true, DEFAULT_MAX_FILE_SIZE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Set<String> includeExtensions = DEFAULT_EXTENSIONS;
        private List<String> includePatterns = List.of();
        private List<String> excludePatterns = List.of();
        private boolean respectGitignore = true;
        private long maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE;

        public Builder includeExtensions(Set<String> extensions) {
            this.includeExtensions = extensions;
            return this;
        }

        public Builder includePatterns(List<String> patterns) {
            this.includePatterns = patterns;
            return this;
        }

        public Builder excludePatterns(List<String> patterns) {
            this.excludePatterns = patterns;
            return this;
        }

        public Builder respectGitignore(boolean respect) {
            this.respectGitignore = respect;
            return this;
        }

        public Builder maxFileSizeBytes(long size) {
            this.maxFileSizeBytes = size;
            return this;
        }

        public FilterConfig build() {
            return new FilterConfig(includeExtensions, includePatterns, excludePatterns,
                                   respectGitignore, maxFileSizeBytes);
        }
    }
}
