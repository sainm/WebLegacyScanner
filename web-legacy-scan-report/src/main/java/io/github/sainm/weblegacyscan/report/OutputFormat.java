package io.github.sainm.weblegacyscan.report;

/**
 * Output format
 */
public enum OutputFormat {
    JSON("json"),
    TEXT("text"),
    HTML("html"),
    SARIF("sarif"),
    CSV("csv");

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
            case "csv" -> CSV;
            default -> TEXT;
        };
    }
}
