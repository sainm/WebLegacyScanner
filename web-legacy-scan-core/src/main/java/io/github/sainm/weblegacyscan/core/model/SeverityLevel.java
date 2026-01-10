package io.github.sainm.weblegacyscan.core.model;

/**
 * 问题严重程度级别
 */
public enum SeverityLevel {
    ERROR(3, "error"),
    WARNING(2, "warning"),
    INFO(1, "info");

    private final int priority;
    private final String displayName;

    SeverityLevel(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }

    public int getPriority() {
        return priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAtLeast(SeverityLevel other) {
        return this.priority >= other.priority;
    }

    public static SeverityLevel fromString(String value) {
        if (value == null) return WARNING;
        return switch (value.toLowerCase()) {
            case "error" -> ERROR;
            case "warning" -> WARNING;
            case "info" -> INFO;
            default -> WARNING;
        };
    }
}
