package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;
import com.google.gson.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Property-based tests for Report Completeness.
 * 
 * Property 13: Report Issue Completeness
 * For any Issue in a generated report (regardless of format), the report SHALL contain:
 * file path, line number, column number, rule ID, category, description, severity, 
 * and replacement suggestion.
 * 
 * Property 14: Report Summary Accuracy
 * For any ScanResult, the summary counts grouped by Severity_Level and Rule_Category 
 * SHALL exactly match the actual counts of Issues in the result.
 * 
 * Property 15: Report Sorting Order
 * For any generated report, Issues SHALL be sorted first by file path (lexicographically),
 * then by line number (ascending).
 * 
 * Validates: Requirements 6.3, 6.4, 6.9
 */
class ReportCompletenessPropertyTest {

    private final JSONFormatter jsonFormatter = new JSONFormatter();
    private final TextFormatter textFormatter = new TextFormatter();

    // ========== Property 13: Report Issue Completeness ==========

    /**
     * Property 13: Report Issue Completeness (JSON format)
     * 
     * For any Issue in a generated JSON report, the report SHALL contain:
     * file path, line number, column number, rule ID, category, description, severity.
     * 
     * Feature: web-legacy-scan, Property 13: Report Issue Completeness
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void jsonReportContainsAllRequiredIssueFields(
            @ForAll("validScanResults") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .includeSnippets(true)
                .build();
        
        String json = jsonFormatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray issuesArray = root.getAsJsonArray("issues");
        
        // Verify each issue contains all required fields
        for (int i = 0; i < issuesArray.size(); i++) {
            JsonObject issueJson = issuesArray.get(i).getAsJsonObject();
            Issue originalIssue = result.getSortedIssues().get(i);
            
            // Required fields must be present
            assert issueJson.has("ruleId") : "Issue must have ruleId";
            assert issueJson.has("category") : "Issue must have category";
            assert issueJson.has("severity") : "Issue must have severity";
            assert issueJson.has("description") : "Issue must have description";
            assert issueJson.has("location") : "Issue must have location";
            
            JsonObject location = issueJson.getAsJsonObject("location");
            assert location.has("file") : "Location must have file path";
            assert location.has("startLine") : "Location must have startLine";
            assert location.has("startColumn") : "Location must have startColumn";
            assert location.has("endLine") : "Location must have endLine";
            assert location.has("endColumn") : "Location must have endColumn";
            
            // Verify values match original
            assert originalIssue.ruleId().equals(issueJson.get("ruleId").getAsString()) :
                    "Rule ID mismatch";
            assert originalIssue.category().getId().equals(issueJson.get("category").getAsString()) :
                    "Category mismatch";
            assert originalIssue.severity().getDisplayName().equals(issueJson.get("severity").getAsString()) :
                    "Severity mismatch";
            assert originalIssue.description().equals(issueJson.get("description").getAsString()) :
                    "Description mismatch";
            assert originalIssue.location().filePath().toString().equals(location.get("file").getAsString()) :
                    "File path mismatch";
            assert originalIssue.location().startLine() == location.get("startLine").getAsInt() :
                    "Start line mismatch";
            assert originalIssue.location().startColumn() == location.get("startColumn").getAsInt() :
                    "Start column mismatch";
            
            // Replacement suggestion if present
            if (originalIssue.suggestion() != null) {
                assert issueJson.has("suggestion") : "Suggestion should be present when original has it";
                JsonObject suggestion = issueJson.getAsJsonObject("suggestion");
                assert suggestion.has("description") : "Suggestion must have description";
                assert suggestion.has("modernAlternative") : "Suggestion must have modernAlternative";
            }
        }
    }

    /**
     * Property 13: Report Issue Completeness (Text format)
     * 
     * For any Issue in a generated text report, the report SHALL contain:
     * file path, line number, column number, rule ID, severity, description.
     * 
     * Feature: web-legacy-scan, Property 13: Report Issue Completeness
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void textReportContainsAllRequiredIssueFields(
            @ForAll("validScanResults") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.TEXT)
                .colorOutput(false)
                .includeSnippets(false)
                .build();
        
        String text = textFormatter.format(result, config);
        
        // Each issue should appear in the text report with required fields
        for (Issue issue : result.issues()) {
            // File path and line number format: "path:line:column:"
            String locationPrefix = String.format("%s:%d:%d:",
                    issue.location().filePath(),
                    issue.location().startLine(),
                    issue.location().startColumn());
            assert text.contains(locationPrefix) :
                    "Text report must contain location: " + locationPrefix;
            
            // Severity
            assert text.contains(issue.severity().getDisplayName()) :
                    "Text report must contain severity: " + issue.severity().getDisplayName();
            
            // Rule ID
            assert text.contains("[" + issue.ruleId() + "]") :
                    "Text report must contain rule ID: " + issue.ruleId();
            
            // Description
            assert text.contains(issue.description()) :
                    "Text report must contain description: " + issue.description();
        }
    }

    // ========== Property 14: Report Summary Accuracy ==========

    /**
     * Property 14: Report Summary Accuracy
     * 
     * For any ScanResult, the summary counts grouped by Severity_Level and Rule_Category
     * SHALL exactly match the actual counts of Issues in the result.
     * 
     * Feature: web-legacy-scan, Property 14: Report Summary Accuracy
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    void jsonReportSummaryMatchesActualCounts(
            @ForAll("validScanResults") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .build();
        
        String json = jsonFormatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        
        // Verify total issues count
        int reportedTotal = summary.get("totalIssues").getAsInt();
        assert reportedTotal == result.issues().size() :
                "Total issues mismatch: expected " + result.issues().size() + ", got " + reportedTotal;
        
        // Verify counts by severity
        JsonObject bySeverity = summary.getAsJsonObject("bySeverity");
        Map<SeverityLevel, Long> actualBySeverity = result.getIssuesBySeverity();
        
        for (SeverityLevel severity : SeverityLevel.values()) {
            long actualCount = actualBySeverity.getOrDefault(severity, 0L);
            String severityKey = severity.getDisplayName();
            
            if (bySeverity.has(severityKey)) {
                long reportedCount = bySeverity.get(severityKey).getAsLong();
                assert reportedCount == actualCount :
                        "Severity count mismatch for " + severityKey + 
                        ": expected " + actualCount + ", got " + reportedCount;
            } else {
                assert actualCount == 0 :
                        "Missing severity " + severityKey + " with count " + actualCount;
            }
        }
        
        // Verify counts by category
        JsonObject byCategory = summary.getAsJsonObject("byCategory");
        Map<RuleCategory, Long> actualByCategory = result.getIssuesByCategory();
        
        for (RuleCategory category : RuleCategory.values()) {
            long actualCount = actualByCategory.getOrDefault(category, 0L);
            String categoryKey = category.getId();
            
            if (byCategory.has(categoryKey)) {
                long reportedCount = byCategory.get(categoryKey).getAsLong();
                assert reportedCount == actualCount :
                        "Category count mismatch for " + categoryKey + 
                        ": expected " + actualCount + ", got " + reportedCount;
            } else {
                assert actualCount == 0 :
                        "Missing category " + categoryKey + " with count " + actualCount;
            }
        }
    }

    /**
     * Property 14: Report Summary Accuracy - Sum of severity counts equals total
     * 
     * The sum of all severity counts SHALL equal the total issues count.
     * 
     * Feature: web-legacy-scan, Property 14: Report Summary Accuracy
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    void severityCountsSumToTotal(
            @ForAll("validScanResults") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .build();
        
        String json = jsonFormatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        
        int totalIssues = summary.get("totalIssues").getAsInt();
        JsonObject bySeverity = summary.getAsJsonObject("bySeverity");
        
        long severitySum = 0;
        for (String key : bySeverity.keySet()) {
            severitySum += bySeverity.get(key).getAsLong();
        }
        
        assert severitySum == totalIssues :
                "Sum of severity counts (" + severitySum + ") must equal total issues (" + totalIssues + ")";
    }

    /**
     * Property 14: Report Summary Accuracy - Sum of category counts equals total
     * 
     * The sum of all category counts SHALL equal the total issues count.
     * 
     * Feature: web-legacy-scan, Property 14: Report Summary Accuracy
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    void categoryCountsSumToTotal(
            @ForAll("validScanResults") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .build();
        
        String json = jsonFormatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        
        int totalIssues = summary.get("totalIssues").getAsInt();
        JsonObject byCategory = summary.getAsJsonObject("byCategory");
        
        long categorySum = 0;
        for (String key : byCategory.keySet()) {
            categorySum += byCategory.get(key).getAsLong();
        }
        
        assert categorySum == totalIssues :
                "Sum of category counts (" + categorySum + ") must equal total issues (" + totalIssues + ")";
    }

    // ========== Property 15: Report Sorting Order ==========

    /**
     * Property 15: Report Sorting Order
     * 
     * For any generated report, Issues SHALL be sorted first by file path (lexicographically),
     * then by line number (ascending).
     * 
     * Feature: web-legacy-scan, Property 15: Report Sorting Order
     * Validates: Requirements 6.9
     */
    @Property(tries = 100)
    void jsonReportIssuesAreSortedByFilePathThenLineNumber(
            @ForAll("validScanResults") ScanResult result
    ) {
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .build();
        
        String json = jsonFormatter.format(result, config);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray issuesArray = root.getAsJsonArray("issues");
        
        if (issuesArray.size() <= 1) {
            return; // Nothing to verify for 0 or 1 issues
        }
        
        // Extract locations from JSON
        List<IssueLocation> locations = new ArrayList<>();
        for (int i = 0; i < issuesArray.size(); i++) {
            JsonObject issue = issuesArray.get(i).getAsJsonObject();
            JsonObject location = issue.getAsJsonObject("location");
            locations.add(new IssueLocation(
                    location.get("file").getAsString(),
                    location.get("startLine").getAsInt(),
                    location.get("startColumn").getAsInt()
            ));
        }
        
        // Verify sorting order
        for (int i = 1; i < locations.size(); i++) {
            IssueLocation prev = locations.get(i - 1);
            IssueLocation curr = locations.get(i);
            
            int pathCompare = prev.filePath.compareTo(curr.filePath);
            if (pathCompare > 0) {
                throw new AssertionError(
                        "Issues not sorted by file path: " + prev.filePath + " should come before " + curr.filePath);
            }
            if (pathCompare == 0) {
                if (prev.startLine > curr.startLine) {
                    throw new AssertionError(
                            "Issues not sorted by line number within same file: line " + 
                            prev.startLine + " should come before line " + curr.startLine);
                }
                if (prev.startLine == curr.startLine && prev.startColumn > curr.startColumn) {
                    throw new AssertionError(
                            "Issues not sorted by column within same line: column " + 
                            prev.startColumn + " should come before column " + curr.startColumn);
                }
            }
        }
    }

    /**
     * Property 15: Report Sorting Order - Sorted issues match Issue.compareTo
     * 
     * The sorted issues in the report SHALL match the natural ordering defined by Issue.compareTo.
     * 
     * Feature: web-legacy-scan, Property 15: Report Sorting Order
     * Validates: Requirements 6.9
     */
    @Property(tries = 100)
    void reportSortingMatchesIssueCompareTo(
            @ForAll("validScanResults") ScanResult result
    ) {
        // Get sorted issues from ScanResult
        List<Issue> sortedIssues = result.getSortedIssues();
        
        // Verify they are sorted according to Issue.compareTo
        for (int i = 1; i < sortedIssues.size(); i++) {
            Issue prev = sortedIssues.get(i - 1);
            Issue curr = sortedIssues.get(i);
            
            assert prev.compareTo(curr) <= 0 :
                    "Issues not properly sorted: " + prev.location() + " should come before " + curr.location();
        }
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<ScanResult> validScanResults() {
        return validIssues().list().ofMaxSize(15)
                .map(issues -> ScanResult.builder()
                        .issues(issues)
                        .statistics(createStatistics(issues.size()))
                        .build());
    }

    @Provide
    Arbitrary<Issue> validIssues() {
        Arbitrary<String> ruleIds = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(15)
                .map(s -> "rule-" + s);
        
        Arbitrary<RuleCategory> categories = Arbitraries.of(RuleCategory.values());
        Arbitrary<SeverityLevel> severities = Arbitraries.of(SeverityLevel.values());
        
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(5)
                .ofMaxLength(80);
        
        Arbitrary<Location> locations = validLocations();
        
        Arbitrary<ReplacementSuggestion> suggestions = validSuggestions().injectNull(0.3);
        
        Arbitrary<String> mdnRefs = Arbitraries.of(
                "https://developer.mozilla.org/en-US/docs/Web/HTML",
                "https://developer.mozilla.org/en-US/docs/Web/CSS",
                "https://developer.mozilla.org/en-US/docs/Web/JavaScript",
                null
        );
        
        return Combinators.combine(ruleIds, categories, severities, descriptions, locations, suggestions, mdnRefs)
                .as((ruleId, category, severity, description, location, suggestion, mdnRef) ->
                        Issue.builder()
                                .ruleId(ruleId)
                                .category(category)
                                .severity(severity)
                                .description(description)
                                .location(location)
                                .suggestion(suggestion)
                                .mdnReference(mdnRef)
                                .build());
    }

    @Provide
    Arbitrary<Location> validLocations() {
        // Generate different file paths to test sorting
        Arbitrary<Path> paths = Arbitraries.of("a", "b", "c", "src/main", "src/test", "lib")
                .flatMap(dir -> Arbitraries.of("file1", "file2", "index", "main", "util")
                        .map(name -> Path.of(dir, name + ".html")));
        
        Arbitrary<Integer> startLines = Arbitraries.integers().between(1, 500);
        Arbitrary<Integer> startColumns = Arbitraries.integers().between(1, 100);
        
        Arbitrary<String> snippets = Arbitraries.strings()
                .alpha()
                .withChars(' ', '<', '>', '/')
                .ofMinLength(5)
                .ofMaxLength(40)
                .injectNull(0.4);
        
        return Combinators.combine(paths, startLines, startColumns, snippets)
                .as((path, startLine, startCol, snippet) -> {
                    int endLine = startLine + Arbitraries.integers().between(0, 3).sample();
                    int endCol = endLine == startLine 
                            ? startCol + Arbitraries.integers().between(1, 30).sample()
                            : Arbitraries.integers().between(1, 100).sample();
                    return new Location(path, startLine, startCol, endLine, endCol, -1, -1, snippet, null);
                });
    }

    @Provide
    Arbitrary<ReplacementSuggestion> validSuggestions() {
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(5)
                .ofMaxLength(40);
        
        Arbitrary<String> alternatives = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(3)
                .ofMaxLength(25);
        
        Arbitrary<String> codeExamples = Arbitraries.strings()
                .alpha()
                .withChars(' ', '<', '>', '/', '=', '"')
                .ofMinLength(5)
                .ofMaxLength(60)
                .injectNull(0.4);
        
        return Combinators.combine(descriptions, alternatives, codeExamples)
                .as(ReplacementSuggestion::new);
    }

    private ScanResult.ScanStatistics createStatistics(int fileCount) {
        return ScanResult.ScanStatistics.builder()
                .totalFiles(Math.max(1, fileCount))
                .scannedFiles(Math.max(1, fileCount))
                .totalElements(fileCount * 10)
                .scanTime(Duration.ofMillis(100))
                .build();
    }

    // Helper record for sorting verification
    private record IssueLocation(String filePath, int startLine, int startColumn) {}
}
