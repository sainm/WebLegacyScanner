package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.RuleCategory;
import io.github.sainm.weblegacyscan.core.model.ReplacementSuggestion;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Rule loader
 */
public class RuleLoader {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public List<RuleDefinition> loadRules(Path configDir) throws IOException {
        Path rulesFile = configDir.resolve("rules.json");
        Path replacementsFile = configDir.resolve("replacements.json");

        List<RuleDefinition> rules = new ArrayList<>();

        if (Files.exists(rulesFile)) {
            rules.addAll(loadRulesFromFile(rulesFile));
        }

        // Load replacement suggestions and merge
        if (Files.exists(replacementsFile)) {
            Map<String, ReplacementSuggestion> replacements = loadReplacements(replacementsFile);
            rules = mergeReplacements(rules, replacements);
        }

        return rules;
    }

    private List<RuleDefinition> loadRulesFromFile(Path file) throws IOException {
        String content = Files.readString(file);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        
        List<RuleDefinition> rules = new ArrayList<>();
        JsonArray rulesArray = root.getAsJsonArray("rules");
        
        if (rulesArray != null) {
            for (JsonElement element : rulesArray) {
                RuleDefinition rule = parseRuleDefinition(element.getAsJsonObject());
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return rules;
    }

    private RuleDefinition parseRuleDefinition(JsonObject obj) {
        try {
            String id = obj.get("id").getAsString();
            String categoryStr = obj.get("category").getAsString();
            RuleCategory category = RuleCategory.fromId(categoryStr);
            
            String description = obj.get("description").getAsString();
            
            SeverityLevel severity = SeverityLevel.WARNING;
            if (obj.has("severity")) {
                severity = SeverityLevel.fromString(obj.get("severity").getAsString());
            }

            String pattern = obj.has("pattern") ? obj.get("pattern").getAsString() : null;
            String mdnReference = obj.has("mdnReference") ? obj.get("mdnReference").getAsString() : null;
            
            // 验证 MDN URL 格式
            if (mdnReference != null && !isValidMdnUrl(mdnReference)) {
                throw new IllegalArgumentException("Invalid MDN reference URL: " + mdnReference);
            }

            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();

            List<String> filePatterns = new ArrayList<>();
            if (obj.has("filePatterns")) {
                for (JsonElement fp : obj.getAsJsonArray("filePatterns")) {
                    filePatterns.add(fp.getAsString());
                }
            }

            ReplacementSuggestion suggestion = null;
            if (obj.has("suggestion")) {
                suggestion = parseSuggestion(obj.getAsJsonObject("suggestion"));
            }

            return RuleDefinition.builder()
                .id(id)
                .category(category)
                .severity(severity)
                .pattern(pattern)
                .description(description)
                .suggestion(suggestion)
                .mdnReference(mdnReference)
                .enabled(enabled)
                .filePatterns(filePatterns)
                .build();

        } catch (Exception e) {
            System.err.println("Failed to parse rule: " + e.getMessage());
            return null;
        }
    }

    private ReplacementSuggestion parseSuggestion(JsonObject obj) {
        String description = obj.get("description").getAsString();
        String modernAlternative = obj.get("modernAlternative").getAsString();
        String codeExample = obj.has("codeExample") ? obj.get("codeExample").getAsString() : null;
        return new ReplacementSuggestion(description, modernAlternative, codeExample);
    }

    private Map<String, ReplacementSuggestion> loadReplacements(Path file) throws IOException {
        String content = Files.readString(file);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        
        Map<String, ReplacementSuggestion> replacements = new HashMap<>();
        JsonObject replacementsObj = root.getAsJsonObject("replacements");
        
        if (replacementsObj != null) {
            for (String key : replacementsObj.keySet()) {
                ReplacementSuggestion suggestion = parseSuggestion(replacementsObj.getAsJsonObject(key));
                replacements.put(key, suggestion);
            }
        }

        return replacements;
    }

    private List<RuleDefinition> mergeReplacements(List<RuleDefinition> rules, 
                                                   Map<String, ReplacementSuggestion> replacements) {
        List<RuleDefinition> merged = new ArrayList<>();
        for (RuleDefinition rule : rules) {
            if (rule.suggestion() == null && replacements.containsKey(rule.id())) {
                merged.add(RuleDefinition.builder()
                    .id(rule.id())
                    .category(rule.category())
                    .severity(rule.severity())
                    .pattern(rule.pattern())
                    .description(rule.description())
                    .suggestion(replacements.get(rule.id()))
                    .mdnReference(rule.mdnReference())
                    .enabled(rule.enabled())
                    .filePatterns(rule.filePatterns())
                    .build());
            } else {
                merged.add(rule);
            }
        }
        return merged;
    }

    private boolean isValidMdnUrl(String url) {
        return url.startsWith("https://developer.mozilla.org/");
    }

    public String serializeRules(List<RuleDefinition> rules) {
        JsonObject root = new JsonObject();
        JsonArray rulesArray = new JsonArray();
        
        for (RuleDefinition rule : rules) {
            JsonObject ruleObj = new JsonObject();
            ruleObj.addProperty("id", rule.id());
            ruleObj.addProperty("category", rule.category().getId());
            ruleObj.addProperty("severity", rule.severity().getDisplayName());
            ruleObj.addProperty("description", rule.description());
            
            if (rule.pattern() != null) {
                ruleObj.addProperty("pattern", rule.pattern());
            }
            if (rule.mdnReference() != null) {
                ruleObj.addProperty("mdnReference", rule.mdnReference());
            }
            ruleObj.addProperty("enabled", rule.enabled());
            
            if (!rule.filePatterns().isEmpty()) {
                JsonArray patterns = new JsonArray();
                rule.filePatterns().forEach(patterns::add);
                ruleObj.add("filePatterns", patterns);
            }

            if (rule.suggestion() != null) {
                JsonObject suggestionObj = new JsonObject();
                suggestionObj.addProperty("description", rule.suggestion().description());
                suggestionObj.addProperty("modernAlternative", rule.suggestion().modernAlternative());
                if (rule.suggestion().codeExample() != null) {
                    suggestionObj.addProperty("codeExample", rule.suggestion().codeExample());
                }
                ruleObj.add("suggestion", suggestionObj);
            }

            rulesArray.add(ruleObj);
        }

        root.add("rules", rulesArray);
        return GSON.toJson(root);
    }
}
