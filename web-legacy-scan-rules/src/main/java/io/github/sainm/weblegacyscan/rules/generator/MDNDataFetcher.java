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
 * 从 MDN browser-compat-data 动态获取废弃 API 数据
 * 
 * 数据源: https://github.com/mdn/browser-compat-data
 * NPM: @mdn/browser-compat-data
 */
public class MDNDataFetcher {

    // MDN browser-compat-data 的 GitHub raw 地址
    private static final String BCD_BASE = "https://raw.githubusercontent.com/mdn/browser-compat-data/main/";
    private static final String MDN_DOC_BASE = "https://developer.mozilla.org/en-US/docs/Web/";
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final HttpClient httpClient;

    public MDNDataFetcher() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public static void main(String[] args) throws Exception {
        MDNDataFetcher fetcher = new MDNDataFetcher();
        
        Path outputDir = args.length > 0 ? Path.of(args[0]) : Path.of("config/generated");
        Files.createDirectories(outputDir);
        
        System.out.println("Fetching deprecated data from MDN browser-compat-data...\n");
        
        List<Map<String, Object>> allRules = new ArrayList<>();
        
        // 1. 获取 HTML 废弃元素
        System.out.println("=== Fetching HTML deprecated elements ===");
        List<Map<String, Object>> htmlRules = fetcher.fetchHTMLDeprecated();
        allRules.addAll(htmlRules);
        System.out.println("Found " + htmlRules.size() + " HTML rules\n");
        
        // 2. 获取 CSS 废弃属性
        System.out.println("=== Fetching CSS deprecated properties ===");
        List<Map<String, Object>> cssRules = fetcher.fetchCSSDeprecated();
        allRules.addAll(cssRules);
        System.out.println("Found " + cssRules.size() + " CSS rules\n");
        
        // 3. 获取 JS 废弃 API
        System.out.println("=== Fetching JavaScript deprecated APIs ===");
        List<Map<String, Object>> jsRules = fetcher.fetchJSDeprecated();
        allRules.addAll(jsRules);
        System.out.println("Found " + jsRules.size() + " JavaScript rules\n");
        
        // 写入文件
        Map<String, Object> output = Map.of(
            "generatedAt", java.time.Instant.now().toString(),
            "source", "MDN browser-compat-data",
            "rules", allRules
        );
        
        Path outputFile = outputDir.resolve("rules-from-mdn.json");
        Files.writeString(outputFile, GSON.toJson(output));
        
        System.out.println("========================================");
        System.out.println("Total rules generated: " + allRules.size());
        System.out.println("Output: " + outputFile);
    }

    /**
     * 获取 HTML 废弃元素
     */
    public List<Map<String, Object>> fetchHTMLDeprecated() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        // HTML 元素列表
        String[] elements = {
            "font", "center", "marquee", "blink", "frame", "frameset", "noframes",
            "applet", "acronym", "big", "strike", "tt", "basefont", "dir", "isindex",
            "listing", "plaintext", "xmp", "nextid", "bgsound", "keygen", "menuitem",
            "spacer", "multicol", "nobr", "noembed", "rb", "rtc"
        };
        
        for (String element : elements) {
            try {
                String url = BCD_BASE + "html/elements/" + element + ".json";
                JsonObject data = fetchJson(url);
                
                if (data != null && isDeprecated(data, "html.elements." + element)) {
                    Map<String, Object> rule = createHTMLRule(element, data);
                    if (rule != null) {
                        rules.add(rule);
                        System.out.println("  ✓ <" + element + "> - deprecated");
                    }
                }
            } catch (Exception e) {
                // 元素不存在或获取失败，跳过
            }
        }
        
        // 获取废弃的全局属性
        rules.addAll(fetchHTMLDeprecatedAttributes());
        
        return rules;
    }

    /**
     * 获取 HTML 废弃属性
     */
    private List<Map<String, Object>> fetchHTMLDeprecatedAttributes() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        String[] attrs = {"align", "bgcolor", "border", "color", "face", "size", "type"};
        
        for (String attr : attrs) {
            try {
                String url = BCD_BASE + "html/global_attributes/" + attr + ".json";
                JsonObject data = fetchJson(url);
                
                if (data != null) {
                    Map<String, Object> rule = createHTMLAttrRule(attr, data);
                    if (rule != null) {
                        rules.add(rule);
                        System.out.println("  ✓ " + attr + " attribute - deprecated");
                    }
                }
            } catch (Exception e) {
                // 跳过
            }
        }
        
        return rules;
    }

    /**
     * 获取 CSS 废弃属性
     */
    public List<Map<String, Object>> fetchCSSDeprecated() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        // 先获取 CSS 属性索引
        try {
            String indexUrl = BCD_BASE + "css/properties/__meta.json";
            // 由于没有索引文件，我们检查已知的废弃属性
            String[] knownDeprecated = {
                "clip", "zoom", "ime-mode", "azimuth", "box-align", "box-direction",
                "box-flex", "box-flex-group", "box-lines", "box-ordinal-group",
                "box-orient", "box-pack", "marker-offset", "page-policy"
            };
            
            for (String prop : knownDeprecated) {
                try {
                    String url = BCD_BASE + "css/properties/" + prop + ".json";
                    JsonObject data = fetchJson(url);
                    
                    if (data != null && isDeprecated(data, "css.properties." + prop)) {
                        Map<String, Object> rule = createCSSRule(prop, data);
                        if (rule != null) {
                            rules.add(rule);
                            System.out.println("  ✓ " + prop + " - deprecated");
                        }
                    }
                } catch (Exception e) {
                    // 跳过
                }
            }
            
            // 添加 vendor prefix 规则
            rules.addAll(createVendorPrefixRules());
            
        } catch (Exception e) {
            System.err.println("Error fetching CSS data: " + e.getMessage());
        }
        
        return rules;
    }

    /**
     * 获取 JavaScript 废弃 API
     */
    public List<Map<String, Object>> fetchJSDeprecated() {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        // 全局函数
        String[][] globalFunctions = {
            {"escape", "javascript/builtins/escape.json"},
            {"unescape", "javascript/builtins/unescape.json"},
            {"eval", "javascript/builtins/eval.json"}
        };
        
        for (String[] func : globalFunctions) {
            try {
                String url = BCD_BASE + func[1];
                JsonObject data = fetchJson(url);
                
                if (data != null) {
                    boolean deprecated = isDeprecated(data, "javascript.builtins." + func[0]);
                    Map<String, Object> rule = createJSRule(func[0], data, deprecated);
                    if (rule != null) {
                        rules.add(rule);
                        System.out.println("  ✓ " + func[0] + "() - " + (deprecated ? "deprecated" : "discouraged"));
                    }
                }
            } catch (Exception e) {
                // 跳过
            }
        }
        
        // String 方法
        String[] stringMethods = {
            "substr", "anchor", "big", "blink", "bold", "fixed", 
            "fontcolor", "fontsize", "italics", "link", "small", 
            "strike", "sub", "sup"
        };
        
        for (String method : stringMethods) {
            try {
                String url = BCD_BASE + "javascript/builtins/String/" + method + ".json";
                JsonObject data = fetchJson(url);
                
                if (data != null && isDeprecated(data, "javascript.builtins.String." + method)) {
                    Map<String, Object> rule = createJSStringMethodRule(method, data);
                    if (rule != null) {
                        rules.add(rule);
                        System.out.println("  ✓ String." + method + "() - deprecated");
                    }
                }
            } catch (Exception e) {
                // 跳过
            }
        }
        
        // Date 方法
        String[] dateMethods = {"getYear", "setYear", "toGMTString"};
        for (String method : dateMethods) {
            try {
                String url = BCD_BASE + "javascript/builtins/Date/" + method + ".json";
                JsonObject data = fetchJson(url);
                
                if (data != null && isDeprecated(data, "javascript.builtins.Date." + method)) {
                    Map<String, Object> rule = createJSDateMethodRule(method, data);
                    if (rule != null) {
                        rules.add(rule);
                        System.out.println("  ✓ Date." + method + "() - deprecated");
                    }
                }
            } catch (Exception e) {
                // 跳过
            }
        }
        
        // Document API
        String[] documentAPIs = {"write", "writeln", "all", "captureEvents", "releaseEvents"};
        for (String api : documentAPIs) {
            try {
                String url = BCD_BASE + "api/Document/" + api + ".json";
                JsonObject data = fetchJson(url);
                
                if (data != null) {
                    Map<String, Object> rule = createDocumentAPIRule(api, data);
                    if (rule != null) {
                        rules.add(rule);
                        System.out.println("  ✓ document." + api + " - deprecated/discouraged");
                    }
                }
            } catch (Exception e) {
                // 跳过
            }
        }
        
        return rules;
    }

    /**
     * 从 URL 获取 JSON 数据
     */
    private JsonObject fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
        return null;
    }

    /**
     * 检查是否标记为废弃
     */
    private boolean isDeprecated(JsonObject data, String path) {
        try {
            String[] parts = path.split("\\.");
            JsonObject current = data;
            
            for (String part : parts) {
                if (current.has(part)) {
                    current = current.getAsJsonObject(part);
                } else {
                    return false;
                }
            }
            
            if (current.has("__compat")) {
                JsonObject compat = current.getAsJsonObject("__compat");
                if (compat.has("status")) {
                    JsonObject status = compat.getAsJsonObject("status");
                    return status.has("deprecated") && status.get("deprecated").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // 解析失败
        }
        return false;
    }

    /**
     * 获取 MDN URL
     */
    private String getMdnUrl(JsonObject data, String path) {
        try {
            String[] parts = path.split("\\.");
            JsonObject current = data;
            
            for (String part : parts) {
                if (current.has(part)) {
                    current = current.getAsJsonObject(part);
                }
            }
            
            if (current.has("__compat")) {
                JsonObject compat = current.getAsJsonObject("__compat");
                if (compat.has("mdn_url")) {
                    return compat.get("mdn_url").getAsString();
                }
            }
        } catch (Exception e) {
            // 解析失败
        }
        return null;
    }

    // ========== 规则创建方法 ==========

    private Map<String, Object> createHTMLRule(String element, JsonObject data) {
        String mdnUrl = getMdnUrl(data, "html.elements." + element);
        if (mdnUrl == null) {
            mdnUrl = MDN_DOC_BASE + "HTML/Element/" + element;
        }
        
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "html-" + element + "-tag");
        rule.put("category", "html-deprecated-tag");
        rule.put("severity", isObsolete(element) ? "ERROR" : "WARNING");
        rule.put("pattern", element);
        rule.put("description", "The <" + element + "> element is deprecated");
        rule.put("mdnReference", mdnUrl);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", "This element is deprecated in HTML5",
            "modernAlternative", getHTMLAlternative(element),
            "codeExample", getHTMLExample(element)
        ));
        return rule;
    }

    private Map<String, Object> createHTMLAttrRule(String attr, JsonObject data) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "html-" + attr + "-attr");
        rule.put("category", "html-deprecated-attr");
        rule.put("severity", "WARNING");
        rule.put("pattern", attr);
        rule.put("description", "The " + attr + " attribute is deprecated");
        rule.put("mdnReference", MDN_DOC_BASE + "HTML/Global_attributes/" + attr);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", "Use CSS instead of the " + attr + " attribute",
            "modernAlternative", "CSS properties",
            "codeExample", "Use CSS styling"
        ));
        return rule;
    }

    private Map<String, Object> createCSSRule(String prop, JsonObject data) {
        String mdnUrl = getMdnUrl(data, "css.properties." + prop);
        if (mdnUrl == null) {
            mdnUrl = MDN_DOC_BASE + "CSS/" + prop;
        }
        
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "css-" + prop.replace("-", "") + "-prop");
        rule.put("category", "css-deprecated-prop");
        rule.put("severity", "WARNING");
        rule.put("pattern", prop);
        rule.put("description", "The " + prop + " property is deprecated");
        rule.put("mdnReference", mdnUrl);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", "This CSS property is deprecated",
            "modernAlternative", getCSSAlternative(prop),
            "codeExample", getCSSAlternative(prop)
        ));
        return rule;
    }

    private Map<String, Object> createJSRule(String func, JsonObject data, boolean deprecated) {
        String mdnUrl = getMdnUrl(data, "javascript.builtins." + func);
        if (mdnUrl == null) {
            mdnUrl = MDN_DOC_BASE + "JavaScript/Reference/Global_Objects/" + func;
        }
        
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "js-" + func);
        rule.put("category", "js-deprecated-api");
        rule.put("severity", func.equals("eval") ? "WARNING" : "WARNING");
        rule.put("pattern", func);
        rule.put("description", func + "() is " + (deprecated ? "deprecated" : "discouraged"));
        rule.put("mdnReference", mdnUrl);
        rule.put("enabled", true);
        rule.put("suggestion", Map.of(
            "description", getJSDescription(func),
            "modernAlternative", getJSAlternative(func),
            "codeExample", getJSAlternative(func)
        ));
        return rule;
    }

    private Map<String, Object> createJSStringMethodRule(String method, JsonObject data) {
        String mdnUrl = getMdnUrl(data, "javascript.builtins.String." + method);
        if (mdnUrl == null) {
            mdnUrl = MDN_DOC_BASE + "JavaScript/Reference/Global_Objects/String/" + method;
        }
        
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "js-string-" + method);
        rule.put("category", "js-deprecated-api");
        rule.put("severity", "WARNING");
        rule.put("pattern", "." + method + "(");
        rule.put("description", "String.prototype." + method + "() is deprecated");
        rule.put("mdnReference", mdnUrl);
        rule.put("enabled", true);
        
        String alternative = method.equals("substr") ? "substring() or slice()" : "DOM manipulation";
        rule.put("suggestion", Map.of(
            "description", "This String method is deprecated",
            "modernAlternative", alternative,
            "codeExample", alternative
        ));
        return rule;
    }

    private Map<String, Object> createJSDateMethodRule(String method, JsonObject data) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "js-date-" + method.toLowerCase());
        rule.put("category", "js-deprecated-api");
        rule.put("severity", "WARNING");
        rule.put("pattern", "." + method + "(");
        rule.put("description", "Date.prototype." + method + "() is deprecated");
        rule.put("mdnReference", MDN_DOC_BASE + "JavaScript/Reference/Global_Objects/Date/" + method);
        rule.put("enabled", true);
        
        String alternative = switch (method) {
            case "getYear" -> "getFullYear()";
            case "setYear" -> "setFullYear()";
            case "toGMTString" -> "toUTCString()";
            default -> "modern Date methods";
        };
        
        rule.put("suggestion", Map.of(
            "description", "Use " + alternative + " instead",
            "modernAlternative", alternative,
            "codeExample", alternative
        ));
        return rule;
    }

    private Map<String, Object> createDocumentAPIRule(String api, JsonObject data) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", "js-document-" + api.toLowerCase());
        rule.put("category", "js-deprecated-api");
        rule.put("severity", "WARNING");
        rule.put("pattern", "document." + api);
        rule.put("description", "document." + api + " is deprecated or discouraged");
        rule.put("mdnReference", MDN_DOC_BASE + "API/Document/" + api);
        rule.put("enabled", true);
        
        String alternative = switch (api) {
            case "write", "writeln" -> "DOM manipulation (createElement, appendChild)";
            case "all" -> "querySelector, querySelectorAll";
            case "captureEvents" -> "addEventListener";
            case "releaseEvents" -> "removeEventListener";
            default -> "modern DOM API";
        };
        
        rule.put("suggestion", Map.of(
            "description", "Use modern DOM API instead",
            "modernAlternative", alternative,
            "codeExample", alternative
        ));
        return rule;
    }

    private List<Map<String, Object>> createVendorPrefixRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        String[][] prefixes = {
            {"-webkit-", "WebKit"},
            {"-moz-", "Mozilla"},
            {"-ms-", "Microsoft"},
            {"-o-", "Opera"}
        };
        
        for (String[] prefix : prefixes) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("id", "css-" + prefix[0].replace("-", "") + "-prefix");
            rule.put("category", "css-vendor-prefix");
            rule.put("severity", "INFO");
            rule.put("pattern", prefix[0]);
            rule.put("description", prefix[1] + " vendor prefix - consider adding standard property");
            rule.put("mdnReference", MDN_DOC_BASE + "Glossary/Vendor_Prefix");
            rule.put("enabled", true);
            rule.put("suggestion", Map.of(
                "description", "Add standard property alongside vendor prefix",
                "modernAlternative", "Standard CSS property",
                "codeExample", prefix[0] + "transform: ...;\ntransform: ...;"
            ));
            rules.add(rule);
        }
        return rules;
    }

    // ========== 辅助方法 ==========

    private boolean isObsolete(String element) {
        return Set.of("blink", "frame", "frameset", "applet", "basefont", "isindex").contains(element);
    }

    private String getHTMLAlternative(String element) {
        return switch (element) {
            case "font" -> "CSS font-family, font-size, color";
            case "center" -> "CSS text-align: center or flexbox";
            case "marquee" -> "CSS animations";
            case "acronym" -> "<abbr> element";
            case "strike" -> "<del> or <s> element";
            case "big" -> "CSS font-size";
            case "tt" -> "<code>, <kbd>, or <samp>";
            case "frame", "frameset" -> "<iframe> or CSS layouts";
            case "applet" -> "JavaScript or WebAssembly";
            case "bgsound" -> "<audio> element";
            default -> "Modern HTML/CSS";
        };
    }

    private String getHTMLExample(String element) {
        return switch (element) {
            case "font" -> "<span style=\"font-family: Arial;\">Text</span>";
            case "center" -> "<div style=\"text-align: center;\">Content</div>";
            case "acronym" -> "<abbr title=\"...\">ABBR</abbr>";
            case "strike" -> "<del>Deleted</del>";
            default -> "Use modern alternative";
        };
    }

    private String getCSSAlternative(String prop) {
        return switch (prop) {
            case "clip" -> "clip-path";
            case "zoom" -> "transform: scale()";
            case "ime-mode" -> "inputmode attribute";
            case "azimuth" -> "Not needed (audio positioning)";
            default -> "Modern CSS property";
        };
    }

    private String getJSDescription(String func) {
        return switch (func) {
            case "escape" -> "Use encodeURIComponent() for URL encoding";
            case "unescape" -> "Use decodeURIComponent() for URL decoding";
            case "eval" -> "Avoid eval() - security risk";
            default -> "This function is deprecated";
        };
    }

    private String getJSAlternative(String func) {
        return switch (func) {
            case "escape" -> "encodeURIComponent()";
            case "unescape" -> "decodeURIComponent()";
            case "eval" -> "JSON.parse() for JSON data";
            default -> "Modern JavaScript API";
        };
    }
}
