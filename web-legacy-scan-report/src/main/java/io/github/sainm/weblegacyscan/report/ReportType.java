package io.github.sainm.weblegacyscan.report;

/**
 * Report type - determines what content to include
 */
public enum ReportType {
    /**
     * Full report - all parsed elements (HTML/JSP tags, CSS, JS)
     */
    FULL("full"),
    
    /**
     * Deprecated report - only deprecated/obsolete elements (violations)
     */
    DEPRECATED("deprecated");

    private final String name;

    ReportType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ReportType fromString(String value) {
        if (value == null) return DEPRECATED;
        return switch (value.toLowerCase()) {
            case "full", "all" -> FULL;
            default -> DEPRECATED;
        };
    }
}
