package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult;
import com.google.gson.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Property-based tests for JSON Report Round-Trip.
 * 
 * Property 12: JSON Report Round-Trip
 * For any ScanResult, formatting to JSON and parsing back SHALL produce 
 * an equivalent ScanResult with all Issues preserved.
 * 
 * Validates: Requirements 6.1
 */
class JSONReportRoundTripPropertyTest {

    private static final Gson GSON = new GsonBuilder().create();
    private final JSONFormatter formatter = new JSONFormatter();

    /**
     * Property 12: JSON Report Round-Trip
     * 
     * For any ScanResult, formatting to JSON and parsing back SHALL produce
     * an equivalent ScanResult with all Issues preserved.
     * 
     * Feature: web-legacy-scan, Property 12: JSON Report Round-Trip
     * Validates: Requirements 6.1
     */
    @Property(tries = 100)
    void scanResultRoundTrip(
            @ForAll("validScanResults") ScanResult original
    ) {
        // Format to JSON
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .includeSnippets(true)
                .build();
        String json = formatter.format(original, config);
        
        // Parse JSON back
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        
        // Verify summary
        JsonObject summary = root.getAsJsonObject("summary");
        assert summary.get("totalIssues").getAsInt() == original.issues().size() :
                "Total issues mismatch: expected " + original.issues().size() + 
                ", got " + summary.get("totalIssues").getAsInt();
        
        // Verify issues array
        JsonArray issuesArray = root.getAsJsonArray("issues");
        assert issuesArray.size() == original.issues().size() :
                "Issues array size mismatch: expected " + original.issues().size() + 
                ", got " + issuesArray.size();
        
        // Verify each issue is preserved
        List<Issue> sortedOriginal = original.getSortedIssues();
        for (int i = 0; i < sortedOriginal.size(); i++) {
            Issue expectedIssue = sortedOriginal.get(i);
            JsonObject actualIssue = issuesArray.get(i).getAsJsonObject();
            assertIssuePreserved(expectedIssue, actualIssue);
        }
    }

    /**
     * Property: Issues with all fields are fully preserved in JSON
     * 
     * For any Issue with all optional fields populated, the JSON output
     * SHALL contain all fields with correct values.
     * 
     * Feature: web-legacy-scan, Property 12: JSON Report Round-Trip
     * Validates: Requirements 6.1
     */
    @Property(tries = 100)
    void issueWithAllFieldsPreserved(
            @ForAll("validIssues") Issue issue
    ) {
        ScanResult result = ScanResult.builder()
                .issues(List.of(issue))
                .statistics(createStatistics(1))
                .build();
        
        ReportConfig config = ReportConfig.builder()
                .format(OutputFormat.JSON)
                .includeSnippets(true)
                .build();
        String json = formatter.format(result, config);
        
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray issuesArray = root.getAsJsonArray("issues");
        JsonObject jsonIssue = issuesArray.get(0).getAsJsonObject();
        
        assertIssuePreserved(issue, jsonIssue);
    }

    @Provide
    Arbitrary<ScanResult> validScanResults() {
        return validIssues().list().ofMaxSize(10)
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
                .ofMaxLength(20)
                .map(s -> "rule-" + s);
        
        Arbitrary<RuleCategory> categories = Arbitraries.of(RuleCategory.values());
        Arbitrary<SeverityLevel> severities = Arbitraries.of(SeverityLevel.values());
        
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(5)
                .ofMaxLength(100);
        
        Arbitrary<Location> locations = validLocations();
        
        Arbitrary<ReplacementSuggestion> suggestions = validSuggestions().injectNull(0.2);
        
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
        Arbitrary<Path> paths = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(s -> Path.of("src", s + ".html"));
        
        Arbitrary<Integer> startLines = Arbitraries.integers().between(1, 1000);
        Arbitrary<Integer> startColumns = Arbitraries.integers().between(1, 200);
        
        Arbitrary<String> snippets = Arbitraries.strings()
                .alpha()
                .withChars(' ', '<', '>', '/')
                .ofMinLength(5)
                .ofMaxLength(50)
                .injectNull(0.3);
        
        return Combinators.combine(paths, startLines, startColumns, snippets)
                .as((path, startLine, startCol, snippet) -> {
                    int endLine = startLine + Arbitraries.integers().between(0, 5).sample();
                    int endCol = endLine == startLine 
                            ? startCol + Arbitraries.integers().between(1, 50).sample()
                            : Arbitraries.integers().between(1, 200).sample();
                    return new Location(path, startLine, startCol, endLine, endCol, -1, -1, snippet, null);
                });
    }

    @Provide
    Arbitrary<ReplacementSuggestion> validSuggestions() {
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(5)
                .ofMaxLength(50);
        
        Arbitrary<String> alternatives = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(3)
                .ofMaxLength(30);
        
        Arbitrary<String> codeExamples = Arbitraries.strings()
                .alpha()
                .withChars(' ', '<', '>', '/', '=', '"')
                .ofMinLength(5)
                .ofMaxLength(100)
                .injectNull(0.3);
        
        return Combinators.combine(descriptions, alternatives, codeExamples)
                .as(ReplacementSuggestion::new);
    }

    private ScanResult.ScanStatistics createStatistics(int fileCount) {
        return ScanResult.ScanStatistics.builder()
                .totalFiles(fileCount)
                .scannedFiles(fileCount)
                .totalElements(fileCount * 10)
                .scanTime(Duration.ofMillis(100))
                .build();
    }

    private void assertIssuePreserved(Issue expected, JsonObject actual) {
        // Verify required fields
        assert expected.ruleId().equals(actual.get("ruleId").getAsString()) :
                "Rule ID mismatch: expected '" + expected.ruleId() + 
                "', got '" + actual.get("ruleId").getAsString() + "'";
        
        assert expected.category().getId().equals(actual.get("category").getAsString()) :
                "Category mismatch: expected '" + expected.category().getId() + 
                "', got '" + actual.get("category").getAsString() + "'";
        
        assert expected.severity().getDisplayName().equals(actual.get("severity").getAsString()) :
                "Severity mismatch: expected '" + expected.severity().getDisplayName() + 
                "', got '" + actual.get("severity").getAsString() + "'";
        
        assert expected.description().equals(actual.get("description").getAsString()) :
                "Description mismatch: expected '" + expected.description() + 
                "', got '" + actual.get("description").getAsString() + "'";
        
        // Verify location
        JsonObject location = actual.getAsJsonObject("location");
        assert location != null : "Location should not be null";
        assert expected.location().filePath().toString().equals(location.get("file").getAsString()) :
                "File path mismatch";
        assert expected.location().startLine() == location.get("startLine").getAsInt() :
                "Start line mismatch";
        assert expected.location().startColumn() == location.get("startColumn").getAsInt() :
                "Start column mismatch";
        assert expected.location().endLine() == location.get("endLine").getAsInt() :
                "End line mismatch";
        assert expected.location().endColumn() == location.get("endColumn").getAsInt() :
                "End column mismatch";
        
        // Verify snippet if present
        if (expected.location().sourceSnippet() != null) {
            assert location.has("snippet") : "Snippet should be present";
            assert expected.location().sourceSnippet().equals(location.get("snippet").getAsString()) :
                    "Snippet mismatch";
        }
        
        // Verify suggestion if present
        if (expected.suggestion() != null) {
            JsonObject suggestion = actual.getAsJsonObject("suggestion");
            assert suggestion != null : "Suggestion should not be null";
            assert expected.suggestion().description().equals(suggestion.get("description").getAsString()) :
                    "Suggestion description mismatch";
            assert expected.suggestion().modernAlternative().equals(suggestion.get("modernAlternative").getAsString()) :
                    "Suggestion modernAlternative mismatch";
            if (expected.suggestion().codeExample() != null) {
                assert suggestion.has("codeExample") : "Code example should be present";
                assert expected.suggestion().codeExample().equals(suggestion.get("codeExample").getAsString()) :
                        "Code example mismatch";
            }
        }
        
        // Verify MDN reference if present
        if (expected.mdnReference() != null) {
            assert actual.has("mdnReference") : "MDN reference should be present";
            assert expected.mdnReference().equals(actual.get("mdnReference").getAsString()) :
                    "MDN reference mismatch";
        }
    }
}
