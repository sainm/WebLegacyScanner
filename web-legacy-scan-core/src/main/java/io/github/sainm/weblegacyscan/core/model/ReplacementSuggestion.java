package io.github.sainm.weblegacyscan.core.model;

import java.util.Objects;

/**
 * 替换建议
 */
public record ReplacementSuggestion(
    String description,
    String modernAlternative,
    String codeExample
) {
    public ReplacementSuggestion {
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(modernAlternative, "modernAlternative cannot be null");
    }

    public static ReplacementSuggestion of(String description, String modernAlternative) {
        return new ReplacementSuggestion(description, modernAlternative, null);
    }

    public static ReplacementSuggestion of(String description, String modernAlternative, String codeExample) {
        return new ReplacementSuggestion(description, modernAlternative, codeExample);
    }
}
