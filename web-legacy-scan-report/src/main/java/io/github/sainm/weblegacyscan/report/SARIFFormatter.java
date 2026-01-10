package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.Issue;
import io.github.sainm.weblegacyscan.core.model.SeverityLevel;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;
import com.google.gson.*;

/**
 * SARIF 格式报告生成器（用于 IDE 集成�?
 */
public final class SARIFFormatter implements ReportFormatter {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    @Override
    public String format(ScanResult result, ReportConfig config) {
        JsonObject root = new JsonObject();
        root.addProperty("$schema", "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json");
        root.addProperty("version", "2.1.0");

        JsonArray runs = new JsonArray();
        JsonObject run = new JsonObject();

        // Tool 信息
        JsonObject tool = new JsonObject();
        JsonObject driver = new JsonObject();
        driver.addProperty("name", "web-legacy-scan");
        driver.addProperty("version", "1.0.0");
        driver.addProperty("informationUri", "https://github.com/example/web-legacy-scan");

        // 规则定义
        JsonArray rules = new JsonArray();
        result.issues().stream()
            .map(Issue::ruleId)
            .distinct()
            .forEach(ruleId -> {
                JsonObject rule = new JsonObject();
                rule.addProperty("id", ruleId);
                rules.add(rule);
            });
        driver.add("rules", rules);
        tool.add("driver", driver);
        run.add("tool", tool);

        // 结果
        JsonArray results = new JsonArray();
        for (Issue issue : result.getSortedIssues()) {
            results.add(formatResult(issue));
        }
        run.add("results", results);

        runs.add(run);
        root.add("runs", runs);

        return GSON.toJson(root);
    }

    private JsonObject formatResult(Issue issue) {
        JsonObject result = new JsonObject();
        result.addProperty("ruleId", issue.ruleId());
        result.addProperty("level", mapSeverity(issue.severity()));

        // 消息
        JsonObject message = new JsonObject();
        message.addProperty("text", issue.description());
        result.add("message", message);

        // 位置
        JsonArray locations = new JsonArray();
        JsonObject location = new JsonObject();
        JsonObject physicalLocation = new JsonObject();

        JsonObject artifactLocation = new JsonObject();
        artifactLocation.addProperty("uri", issue.location().filePath().toString().replace("\\", "/"));
        physicalLocation.add("artifactLocation", artifactLocation);

        JsonObject region = new JsonObject();
        region.addProperty("startLine", issue.location().startLine());
        region.addProperty("startColumn", issue.location().startColumn());
        region.addProperty("endLine", issue.location().endLine());
        region.addProperty("endColumn", issue.location().endColumn());
        
        if (issue.location().sourceSnippet() != null) {
            JsonObject snippet = new JsonObject();
            snippet.addProperty("text", issue.location().sourceSnippet());
            region.add("snippet", snippet);
        }
        
        physicalLocation.add("region", region);
        location.add("physicalLocation", physicalLocation);
        locations.add(location);
        result.add("locations", locations);

        // 修复建议
        if (issue.suggestion() != null) {
            JsonArray fixes = new JsonArray();
            JsonObject fix = new JsonObject();
            JsonObject fixDescription = new JsonObject();
            fixDescription.addProperty("text", issue.suggestion().description());
            fix.add("description", fixDescription);
            fixes.add(fix);
            result.add("fixes", fixes);
        }

        // 帮助链接
        if (issue.mdnReference() != null) {
            JsonObject help = new JsonObject();
            help.addProperty("text", "See MDN documentation");
            help.addProperty("markdown", "[MDN Documentation](" + issue.mdnReference() + ")");
            result.add("help", help);
        }

        return result;
    }

    private String mapSeverity(SeverityLevel severity) {
        return switch (severity) {
            case ERROR -> "error";
            case WARNING -> "warning";
            case INFO -> "note";
        };
    }

    @Override
    public OutputFormat getFormat() {
        return OutputFormat.SARIF;
    }
}
