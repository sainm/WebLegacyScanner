package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.rules.impl.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule factory
 */
public class RuleFactory {

    public Rule createRule(RuleDefinition definition) {
        return switch (definition.category()) {
            case HTML_DEPRECATED_TAG -> new HTMLDeprecatedTagRule(definition);
            case HTML_DEPRECATED_ATTR -> new HTMLDeprecatedAttrRule(definition);
            case CSS_DEPRECATED_PROP -> new CSSDeprecatedPropRule(definition);
            case CSS_VENDOR_PREFIX -> new CSSVendorPrefixRule(definition);
            case JS_DEPRECATED_API -> new JSDeprecatedAPIRule(definition);
            case JS_DEPRECATED_SYNTAX -> new JSDeprecatedSyntaxRule(definition);
        };
    }

    public List<Rule> createDefaultRules() {
        List<Rule> rules = new ArrayList<>();
        
        // HTML deprecated tag rules
        rules.addAll(createDefaultHTMLTagRules());
        
        // HTML deprecated attribute rules
        rules.addAll(createDefaultHTMLAttrRules());
        
        // CSS deprecated property rules
        rules.addAll(createDefaultCSSRules());
        
        // JS deprecated API rules
        rules.addAll(createDefaultJSRules());
        
        return rules;
    }

    private List<Rule> createDefaultHTMLTagRules() {
        List<Rule> rules = new ArrayList<>();
        
        String[][] deprecatedTags = {
            {"font", "Use CSS font-family, font-size, color instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/font"},
            {"center", "Use CSS text-align: center instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/center"},
            {"marquee", "Use CSS animations instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/marquee"},
            {"blink", "Use CSS animations instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/blink"},
            {"basefont", "Use CSS font properties instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/basefont"},
            {"big", "Use CSS font-size instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/big"},
            {"strike", "Use <del> or CSS text-decoration instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/strike"},
            {"tt", "Use <code> or CSS font-family instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/tt"},
            {"frame", "Use <iframe> instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/frame"},
            {"frameset", "Use CSS layout instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/frameset"},
            {"noframes", "Not needed with modern browsers", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/noframes"},
            {"applet", "Use <object> or <embed> instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/applet"},
            {"acronym", "Use <abbr> instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/acronym"},
            {"dir", "Use <ul> instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/dir"}
        };

        for (String[] tag : deprecatedTags) {
            RuleDefinition def = RuleDefinition.builder()
                .id("html-deprecated-tag-" + tag[0])
                .category(RuleCategory.HTML_DEPRECATED_TAG)
                .severity(SeverityLevel.WARNING)
                .pattern(tag[0])
                .description("Deprecated HTML tag <" + tag[0] + ">")
                .suggestion(ReplacementSuggestion.of(tag[1], tag[1]))
                .mdnReference(tag[2])
                .build();
            rules.add(new HTMLDeprecatedTagRule(def));
        }

        return rules;
    }

    private List<Rule> createDefaultHTMLAttrRules() {
        List<Rule> rules = new ArrayList<>();
        
        String[][] deprecatedAttrs = {
            {"align", "Use CSS text-align or margin instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes"},
            {"bgcolor", "Use CSS background-color instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes"},
            {"border", "Use CSS border instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes"},
            {"language", "Use type attribute instead", "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script"}
        };

        for (String[] attr : deprecatedAttrs) {
            RuleDefinition def = RuleDefinition.builder()
                .id("html-deprecated-attr-" + attr[0])
                .category(RuleCategory.HTML_DEPRECATED_ATTR)
                .severity(SeverityLevel.WARNING)
                .pattern(attr[0])
                .description("Deprecated HTML attribute '" + attr[0] + "'")
                .suggestion(ReplacementSuggestion.of(attr[1], attr[1]))
                .mdnReference(attr[2])
                .build();
            rules.add(new HTMLDeprecatedAttrRule(def));
        }

        return rules;
    }

    private List<Rule> createDefaultCSSRules() {
        List<Rule> rules = new ArrayList<>();
        
        String[][] deprecatedProps = {
            {"clip", "Use clip-path instead", "https://developer.mozilla.org/en-US/docs/Web/CSS/clip"},
            {"zoom", "Use transform: scale() instead", "https://developer.mozilla.org/en-US/docs/Web/CSS/zoom"}
        };

        for (String[] prop : deprecatedProps) {
            RuleDefinition def = RuleDefinition.builder()
                .id("css-deprecated-prop-" + prop[0])
                .category(RuleCategory.CSS_DEPRECATED_PROP)
                .severity(SeverityLevel.WARNING)
                .pattern(prop[0])
                .description("Deprecated CSS property '" + prop[0] + "'")
                .suggestion(ReplacementSuggestion.of(prop[1], prop[1]))
                .mdnReference(prop[2])
                .build();
            rules.add(new CSSDeprecatedPropRule(def));
        }

        return rules;
    }

    private List<Rule> createDefaultJSRules() {
        List<Rule> rules = new ArrayList<>();
        
        String[][] deprecatedAPIs = {
            {"escape", "Use encodeURIComponent() instead", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/escape"},
            {"unescape", "Use decodeURIComponent() instead", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/unescape"},
            {"document.write", "Use DOM manipulation instead", "https://developer.mozilla.org/en-US/docs/Web/API/Document/write"},
            {"document.writeln", "Use DOM manipulation instead", "https://developer.mozilla.org/en-US/docs/Web/API/Document/writeln"},
            {"eval", "Avoid eval() for security reasons", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval"}
        };

        for (String[] api : deprecatedAPIs) {
            RuleDefinition def = RuleDefinition.builder()
                .id("js-deprecated-api-" + api[0].replace(".", "-"))
                .category(RuleCategory.JS_DEPRECATED_API)
                .severity(api[0].equals("eval") ? SeverityLevel.WARNING : SeverityLevel.WARNING)
                .pattern(api[0])
                .description("Deprecated JavaScript API '" + api[0] + "'")
                .suggestion(ReplacementSuggestion.of(api[1], api[1]))
                .mdnReference(api[2])
                .build();
            rules.add(new JSDeprecatedAPIRule(def));
        }

        // var keyword
        RuleDefinition varDef = RuleDefinition.builder()
            .id("js-deprecated-syntax-var")
            .category(RuleCategory.JS_DEPRECATED_SYNTAX)
            .severity(SeverityLevel.INFO)
            .pattern("var")
            .description("Use 'let' or 'const' instead of 'var'")
            .suggestion(ReplacementSuggestion.of("Use let or const", "let/const"))
            .mdnReference("https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/var")
            .build();
        rules.add(new JSDeprecatedSyntaxRule(varDef));

        return rules;
    }
}
