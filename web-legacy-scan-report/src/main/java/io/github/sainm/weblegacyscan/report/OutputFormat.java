package io.github.sainm.weblegacyscan.report;

/**
 * 输出格式
 */
public enum OutputFormat {
    JSON("json"),
    TEXT("text"),
    HTML("html"),
    SARIF("sarif");

    private final String name;

    OutputFormat(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static OutputFormat fromString(String value) {
        if (value == null) return TEXT;
        return switch (value.toLowerCase()) {
            case "json" -> JSON;
            case "html" -> HTML;
            case "sarif" -> SARIF;
            default -> TEXT;
        };
    }
}
