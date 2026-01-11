package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;
import com.google.gson.*;

/**
 * JSON format report generator.
 */
public final class JSONFormatter implements ReportFormatter {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    @Override
    public String format(ScanResult result, ReportConfig config) {
        JsonObject root = new JsonObject();

        // Summary
        JsonObject summary = new JsonObject();
        summary.addProperty("totalIssues", result.issues().size());
        summary.addProperty("totalFiles", result.statistics().totalFiles());
        summary.addProperty("scannedFiles", result.statistics().scannedFiles());
        summary.addProperty("scanTimeMs", result.statistics().scanTime().toMillis());
        summary.addProperty("filesPerSecond", result.statistics().filesPerSecond());

        // By severity
        JsonObject bySeverity = new JsonObject();
        result.getIssuesBySeverity().forEach((severity, count) -> 
            bySeverity.addProperty(severity.getDisplayName(), count));
        summary.add("bySeverity", bySeverity);

        // By category
        JsonObject byCategory = new JsonObject();
        result.getIssuesByCategory().forEach((category, count) -> 
            byCategory.addProperty(category.getId(), count));
        summary.add("byCategory", byCategory);

        root.add("summary", summary);

        // Issues list
        JsonArray issuesArray = new JsonArray();
        for (Issue issue : result.getSortedIssues()) {
            issuesArray.add(formatIssue(issue, config));
        }
        root.add("issues", issuesArray);

        // All elements (if outputAllElements is enabled)
        if (config.outputAllElements() && !result.allElements().isEmpty()) {
            JsonArray elementsArray = new JsonArray();
            for (CodeElement element : result.allElements()) {
                elementsArray.add(formatElement(element));
            }
            root.add("elements", elementsArray);
        }

        // Errors
        if (!result.errors().isEmpty()) {
            JsonArray errorsArray = new JsonArray();
            for (ScanResult.ScanError error : result.errors()) {
                JsonObject errorObj = new JsonObject();
                errorObj.addProperty("file", error.filePath());
                errorObj.addProperty("message", error.message());
                errorObj.addProperty("fatal", error.fatal());
                errorsArray.add(errorObj);
            }
            root.add("errors", errorsArray);
        }

        return GSON.toJson(root);
    }

    private JsonObject formatIssue(Issue issue, ReportConfig config) {
        JsonObject obj = new JsonObject();
        obj.addProperty("ruleId", issue.ruleId());
        obj.addProperty("category", issue.category().getId());
        obj.addProperty("severity", issue.severity().getDisplayName());
        obj.addProperty("description", issue.description());

        // Location
        JsonObject location = new JsonObject();
        location.addProperty("file", issue.location().filePath().toString());
        location.addProperty("startLine", issue.location().startLine());
        location.addProperty("startColumn", issue.location().startColumn());
        location.addProperty("endLine", issue.location().endLine());
        location.addProperty("endColumn", issue.location().endColumn());
        if (config.includeSnippets() && issue.location().sourceSnippet() != null) {
            location.addProperty("snippet", issue.location().sourceSnippet());
        }
        obj.add("location", location);

        // Replacement suggestion
        if (issue.suggestion() != null) {
            JsonObject suggestion = new JsonObject();
            suggestion.addProperty("description", issue.suggestion().description());
            suggestion.addProperty("modernAlternative", issue.suggestion().modernAlternative());
            if (issue.suggestion().codeExample() != null) {
                suggestion.addProperty("codeExample", issue.suggestion().codeExample());
            }
            obj.add("suggestion", suggestion);
        }

        if (issue.mdnReference() != null) {
            obj.addProperty("mdnReference", issue.mdnReference());
        }

        return obj;
    }

    private JsonObject formatElement(CodeElement element) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", element.getElementType().name());

        JsonObject location = new JsonObject();
        location.addProperty("file", element.getLocation().filePath().toString());
        location.addProperty("startLine", element.getLocation().startLine());
        location.addProperty("startColumn", element.getLocation().startColumn());
        location.addProperty("endLine", element.getLocation().endLine());
        location.addProperty("endColumn", element.getLocation().endColumn());
        obj.add("location", location);

        switch (element) {
            case HTMLElement html -> {
                obj.addProperty("tagName", html.tagName());
                obj.addProperty("elementType", html.elementType().name());
            }
            case CSSElement css -> {
                obj.addProperty("cssType", css.type().name());
                if (css.property() != null) obj.addProperty("property", css.property());
                if (css.value() != null) obj.addProperty("value", css.value());
            }
            case JSElement js -> {
                obj.addProperty("jsType", js.type().name());
                if (js.identifier() != null) obj.addProperty("identifier", js.identifier());
            }
        }

        return obj;
    }

    @Override
    public OutputFormat getFormat() {
        return OutputFormat.JSON;
    }
}
