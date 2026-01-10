package io.github.sainm.weblegacyscan.core.model;

/**
 * 规则分类
 */
public enum RuleCategory {
    HTML_DEPRECATED_TAG("HTML Deprecated Tag", "html-deprecated-tag"),
    HTML_DEPRECATED_ATTR("HTML Deprecated Attribute", "html-deprecated-attr"),
    CSS_DEPRECATED_PROP("CSS Deprecated Property", "css-deprecated-prop"),
    CSS_VENDOR_PREFIX("CSS Vendor Prefix", "css-vendor-prefix"),
    JS_DEPRECATED_API("JavaScript Deprecated API", "js-deprecated-api"),
    JS_DEPRECATED_SYNTAX("JavaScript Deprecated Syntax", "js-deprecated-syntax");

    private final String displayName;
    private final String id;

    RuleCategory(String displayName, String id) {
        this.displayName = displayName;
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }

    public static RuleCategory fromId(String id) {
        for (RuleCategory category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category id: " + id);
    }
}
