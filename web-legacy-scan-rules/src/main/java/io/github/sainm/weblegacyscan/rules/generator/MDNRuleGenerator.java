package io.github.sainm.weblegacyscan.rules.generator;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * 从 MDN browser-compat-data 自动生成 rules.json
 * 
 * 数据源: https://github.com/mdn/browser-compat-data
 */
public class MDNRuleGenerator {

    private static final String MDN_BASE_URL = "https://raw.githubusercontent.com/mdn/browser-compat-data/main/";
    private static final String MDN_DOC_BASE = "https://developer.mozilla.org/en-US/docs/Web/";
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final HttpClient httpClient;
    
    // MDN 数据文件路径
    private static final Map<String, String> DATA_SOURCES = Map.of(
        "html-elements", "html/elements/",
        "html-global-attrs", "html/global_attributes/",
        "css-properties", "css/properties/",
        "javascript-builtins", "javascript/builtins/"
    );

    public MDNRuleGenerator() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public static void main(String[] args) throws Exception {
        MDNRuleGenerator generator = new MDNRuleGenerator();
        
        Path outputDir = args.length > 0 ? Path.of(args[0]) : Path.of("config/generated");
        Files.createDirectories(outputDir);
        
        System.out.println("Generating rules from MDN browser-compat-data...");
        
        // 生成 HTML 废弃标签规则
        List<Map<String, Object>> htmlRules = generator.generateHTMLDeprecatedTagRules();
        System.out.println("Generated " + htmlRules.size() + " HTML deprecated tag rules");
        
        // 生成 CSS 废弃属性规则
        List<Map<String, Object>> cssRules = generator.generateCSSDeprecatedPropRules();
        System.out.println("Generated " + cssRules.size() + " CSS deprecated property rules");
        
        // 生成 JS 废弃 API 规则
        List<Map<String, Object>> jsRules = generator.generateJSDeprecatedAPIRules();
        System.out.println("Generated " + jsRules.size() + " JavaScript deprecated API rules");
        
        // 合并所有规则
        List<Map<String, Object>> allRules = new ArrayList<>();
        allRules.addAll(htmlRules);
        allRules.addAll(cssRules);
        allRules.addAll(jsRules);
        
        // 写入文件
        Map<String, Object> output = Map.of("rules", allRules);
        String json = GSON.toJson(output);
        
        Path outputFile = outputDir.resolve("rules.json");
        Files.writeString(outputFile, json);
        System.out.println("\nGenerated rules written to: " + outputFile);
        System.out.println("Total rules: " + allRules.size());
    }

    /**
     * 生成 HTML 废弃标签规则
     */
    public List<Map<String, Object>> generateHTMLDeprecatedTagRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        // 已知的废弃 HTML 标签列表
        List<DeprecatedItem> deprecatedTags = List.of(
            new DeprecatedItem("font", "Use CSS font properties", "CSS font-family, font-size, color"),
            new DeprecatedItem("center", "Use CSS for centering", "CSS text-align: center or flexbox"),
            new DeprecatedItem("marquee", "Use CSS animations", "CSS @keyframes animation"),
            new DeprecatedItem("blink", "Obsolete - remove or use CSS animation", "CSS animation"),
            new DeprecatedItem("frame", "Use iframe or modern layouts", "iframe or CSS Grid"),
            new DeprecatedItem("frameset", "Use modern CSS layouts", "CSS Grid or Flexbox"),
            new DeprecatedItem("noframes", "Remove - frames are obsolete", "Not needed"),
            new DeprecatedItem("applet", "Java Applets are obsolete", "JavaScript or WebAssembly"),
            new DeprecatedItem("acronym", "Use abbr tag", "abbr element"),
            new DeprecatedItem("big", "Use CSS font-size", "CSS font-size: larger"),
            new DeprecatedItem("strike", "Use del or s tag", "del or s element"),
            new DeprecatedItem("tt", "Use code, kbd, or samp", "code, kbd, or samp element"),
            new DeprecatedItem("basefont", "Use CSS for default fonts", "CSS body font styles"),
            new DeprecatedItem("dir", "Use ul tag", "ul element"),
            new DeprecatedItem("isindex", "Use form with input", "form with input element"),
            new DeprecatedItem("listing", "Use pre tag", "pre element"),
            new DeprecatedItem("plaintext", "Use pre tag", "pre element"),
            new DeprecatedItem("xmp", "Use pre and code tags", "pre with code element"),
            new DeprecatedItem("nextid", "Obsolete - remove", "Not needed"),
            new DeprecatedItem("noembed", "Obsolete - remove", "Not needed"),
            new DeprecatedItem("bgsound", "Use audio tag", "audio element"),
            new DeprecatedItem("keygen", "Obsolete - use Web Crypto API", "Web Crypto API"),
            new DeprecatedItem("menu", "Limited support - use nav or ul", "nav or ul element"),
            new DeprecatedItem("menuitem", "Obsolete - remove", "Custom context menu with JS")
        );

        for (DeprecatedItem item : deprecatedTags) {
            rules.add(createHTMLTagRule(item));
        }

        // 尝试从 MDN 获取更多数据
        try {
            List<Map<String, Object>> mdnRules = fetchHTMLDeprecatedFromMDN();
            // 合并，避免重复
            Set<String> existingPatterns = new HashSet<>();
            for (Map<String, Object> rule : rules) {
                existingPatterns.add((String) rule.get("pattern"));
            }
            for (Map<String, Object> mdnRule : mdnRules) {
                if (!existingPatterns.contains(mdnRule.get("pattern"))) {
                    rules.add(mdnRule);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not fetch MDN data: " + e.getMessage());
        }

        return rules;
    }

    /**
     * 生成 CSS 废弃属性规则
     */
    public List<Map<String, Object>> generateCSSDeprecatedPropRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        List<DeprecatedItem> deprecatedProps = List.of(
            new DeprecatedItem("clip", "Use clip-path instead", "clip-path property"),
            new DeprecatedItem("zoom", "Non-standard - use transform: scale()", "transform: scale()"),
            new DeprecatedItem("behavior", "IE-specific - use JavaScript", "JavaScript"),
            new DeprecatedItem("ime-mode", "Non-standard - avoid", "inputmode attribute"),
            new DeprecatedItem("scrollbar-base-color", "IE-specific - use standard scrollbar styling", "scrollbar-color"),
            new DeprecatedItem("scrollbar-face-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("scrollbar-arrow-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("scrollbar-track-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("scrollbar-shadow-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("scrollbar-highlight-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("scrollbar-3dlight-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("scrollbar-darkshadow-color", "IE-specific", "scrollbar-color"),
            new DeprecatedItem("-webkit-box-orient", "Old flexbox - use flex-direction", "flex-direction"),
            new DeprecatedItem("-webkit-box-pack", "Old flexbox - use justify-content", "justify-content"),
            new DeprecatedItem("-webkit-box-align", "Old flexbox - use align-items", "align-items"),
            new DeprecatedItem("-webkit-box-flex", "Old flexbox - use flex", "flex property"),
            new DeprecatedItem("-webkit-box-ordinal-group", "Old flexbox - use order", "order property")
        );

        for (DeprecatedItem item : deprecatedProps) {
            rules.add(createCSSPropRule(item));
        }

        // 添加 vendor prefix 规则
        rules.addAll(createVendorPrefixRules());

        return rules;
    }

    /**
     * 生成 JavaScript 废弃 API 规则
     */
    public List<Map<String, Object>> generateJSDeprecatedAPIRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        List<DeprecatedItem> deprecatedAPIs = List.of(
            // Global functions
            new DeprecatedItem("escape", "Use encodeURIComponent()", "encodeURIComponent()"),
            new DeprecatedItem("unescape", "Use decodeURIComponent()", "decodeURIComponent()"),
            
            // Document methods
            new DeprecatedItem("document.write", "Use DOM manipulation", "createElement, appendChild"),
            new DeprecatedItem("document.writeln", "Use DOM manipulation", "createElement, innerHTML"),
            new DeprecatedItem("document.all", "Non-standard - use querySelector", "querySelector, querySelectorAll"),
            new DeprecatedItem("document.layers", "Netscape 4 only - obsolete", "getElementById"),
            new DeprecatedItem("document.captureEvents", "Obsolete", "addEventListener"),
            new DeprecatedItem("document.releaseEvents", "Obsolete", "removeEventListener"),
            
            // String methods
            new DeprecatedItem("substr", "Use substring() or slice()", "substring() or slice()"),
            new DeprecatedItem("anchor", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("big", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("blink", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("bold", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("fixed", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("fontcolor", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("fontsize", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("italics", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("link", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("small", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("strike", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("sub", "String HTML wrapper - obsolete", "DOM manipulation"),
            new DeprecatedItem("sup", "String HTML wrapper - obsolete", "DOM manipulation"),
            
            // Date methods
            new DeprecatedItem("getYear", "Use getFullYear()", "getFullYear()"),
            new DeprecatedItem("setYear", "Use setFullYear()", "setFullYear()"),
            new DeprecatedItem("toGMTString", "Use toUTCString()", "toUTCString()"),
            
            // RegExp
            new DeprecatedItem("compile", "RegExp.prototype.compile is deprecated", "Create new RegExp"),
            
            // Other
            new DeprecatedItem("eval", "Security risk - avoid", "JSON.parse for JSON"),
            new DeprecatedItem("with", "Forbidden in strict mode", "Destructuring or explicit access")
        );

        for (DeprecatedItem item : deprecatedAPIs) {
            rules.add(createJSAPIRule(item));
        }

        // 添加语法规则
        rules.add(createJSSyntaxRule("var", "Function-scoped - use let/const", "let or const", "INFO"));

        return rules;
    }

    private Map<String, Object> createHTMLTagRule(DeprecatedItem item) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "html-" + item.pattern + "-tag");
        rule.put("category", "html-deprecated-tag");
        rule.put("severity", item.pattern.matches("blink|frame|frameset|applet|basefont") ? "ERROR" : "WARNING");
        rule.put("pattern", item.pattern);
        rule.put("description", "The <" + item.pattern + "> tag is deprecated in HTML5");
        rule.put("mdnReference", MDN_DOC_BASE + "HTML/Element/" + item.pattern);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", item.description,
            "modernAlternative", item.alternative,
            "codeExample", generateHTMLExample(item.pattern, item.alternative)
        ));
        return rule;
    }

    private Map<String, Object> createCSSPropRule(DeprecatedItem item) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "css-" + item.pattern.replace("-", "").replace("webkit", "wk") + "-prop");
        rule.put("category", item.pattern.startsWith("-") ? "css-vendor-prefix" : "css-deprecated-prop");
        rule.put("severity", item.pattern.contains("behavior") ? "ERROR" : "WARNING");
        rule.put("pattern", item.pattern);
        rule.put("description", item.description);
        rule.put("mdnReference", MDN_DOC_BASE + "CSS/" + item.pattern);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", item.description,
            "modernAlternative", item.alternative,
            "codeExample", item.alternative + ";"
        ));
        return rule;
    }

    private Map<String, Object> createJSAPIRule(DeprecatedItem item) {
        Map<String, Object> rule = new LinkedHashMap<>();
        String id = item.pattern.replace(".", "-").replace("(", "").replace(")", "");
        rule.put("id", "js-" + id);
        rule.put("category", "js-deprecated-api");
        rule.put("severity", item.pattern.matches("eval|with") ? "WARNING" : "WARNING");
        rule.put("pattern", item.pattern);
        rule.put("description", item.description);
        rule.put("mdnReference", generateJSMdnUrl(item.pattern));
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", item.description,
            "modernAlternative", item.alternative,
            "codeExample", item.alternative
        ));
        return rule;
    }

    private Map<String, Object> createJSSyntaxRule(String pattern, String desc, String alt, String severity) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "js-" + pattern + "-syntax");
        rule.put("category", "js-deprecated-syntax");
        rule.put("severity", severity);
        rule.put("pattern", pattern);
        rule.put("description", desc);
        rule.put("mdnReference", MDN_DOC_BASE + "JavaScript/Reference/Statements/" + pattern);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", desc,
            "modernAlternative", alt,
            "codeExample", alt.equals("let or const") ? "const x = 1;\nlet y = 2;" : alt
        ));
        return rule;
    }

    private List<Map<String, Object>> createVendorPrefixRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        String[] prefixes = {"-webkit-", "-moz-", "-ms-", "-o-"};
        String[] names = {"WebKit", "Mozilla", "Microsoft", "Opera"};
        
        for (int i = 0; i < prefixes.length; i++) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("id", "css-" + prefixes[i].replace("-", "") + "-prefix");
            rule.put("category", "css-vendor-prefix");
            rule.put("severity", "INFO");
            rule.put("pattern", prefixes[i]);
            rule.put("description", names[i] + " vendor prefix - add standard property");
            rule.put("mdnReference", MDN_DOC_BASE + "Glossary/Vendor_Prefix");
            rule.put("enabled", true);
            rule.put("suggestion", Map.of(
                "description", "Add standard property alongside vendor prefix",
                "modernAlternative", "Standard CSS property without prefix",
                "codeExample", prefixes[i] + "transform: rotate(45deg);\ntransform: rotate(45deg);"
            ));
            rules.add(rule);
        }
        return rules;
    }

    private String generateHTMLExample(String tag, String alternative) {
        return switch (tag) {
            case "font" -> "<span style=\"font-family: Arial;\">Text</span>";
            case "center" -> "<div style=\"text-align: center;\">Content</div>";
            case "marquee" -> "<div class=\"animate\">Text</div> with CSS animation";
            case "acronym" -> "<abbr title=\"Full text\">ABBR</abbr>";
            case "strike" -> "<del>Deleted</del> or <s>Strikethrough</s>";
            default -> "Use " + alternative + " instead";
        };
    }

    private String generateJSMdnUrl(String pattern) {
        if (pattern.startsWith("document.")) {
            return MDN_DOC_BASE + "API/Document/" + pattern.substring(9);
        }
        if (pattern.equals("eval") || pattern.equals("escape") || pattern.equals("unescape")) {
            return MDN_DOC_BASE + "JavaScript/Reference/Global_Objects/" + pattern;
        }
        if (pattern.equals("with")) {
            return MDN_DOC_BASE + "JavaScript/Reference/Statements/with";
        }
        return MDN_DOC_BASE + "JavaScript/Reference/Global_Objects/String/" + pattern;
    }

    /**
     * 从 MDN GitHub 获取 HTML 废弃元素数据
     */
    private List<Map<String, Object>> fetchHTMLDeprecatedFromMDN() throws IOException, InterruptedException {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        // MDN browser-compat-data 的 HTML 元素索引
        String indexUrl = MDN_BASE_URL + "html/elements/__dir.json";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(indexUrl))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonArray elements = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement elem : elements) {
                String elementName = elem.getAsString();
                // 获取每个元素的详细数据检查是否废弃
                // (简化处理，实际需要解析每个元素的 JSON)
            }
        }
        
        return rules;
    }

    /**
     * 废弃项数据结构
     */
    private record DeprecatedItem(String pattern, String description, String alternative) {}
}
