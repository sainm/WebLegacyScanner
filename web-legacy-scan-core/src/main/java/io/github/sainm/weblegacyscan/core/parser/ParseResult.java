package io.github.sainm.weblegacyscan.core.parser;

import io.github.sainm.weblegacyscan.core.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析结果
 */
public record ParseResult(
    List<CodeElement> elements,
    List<ParseError> errors,
    boolean success,
    ParseStatistics statistics
) {
    public ParseResult {
        elements = elements != null ? List.copyOf(elements) : List.of();
        errors = errors != null ? List.copyOf(errors) : List.of();
        statistics = statistics != null ? statistics : ParseStatistics.empty();
    }

    public static ParseResult success(List<CodeElement> elements, ParseStatistics statistics) {
        return new ParseResult(elements, List.of(), true, statistics);
    }

    public static ParseResult failure(List<ParseError> errors) {
        return new ParseResult(List.of(), errors, false, ParseStatistics.empty());
    }

    public static ParseResult partial(List<CodeElement> elements, List<ParseError> errors, ParseStatistics statistics) {
        return new ParseResult(elements, errors, !elements.isEmpty(), statistics);
    }

    public List<CodeElement> getAllElementsFlat() {
        List<CodeElement> result = new ArrayList<>();
        collectElements(elements, result);
        return result;
    }

    private void collectElements(List<? extends CodeElement> source, List<CodeElement> target) {
        for (CodeElement element : source) {
            target.add(element);
            switch (element) {
                case HTMLElement html -> collectElements(html.children(), target);
                case CSSElement css -> collectElements(css.children(), target);
                case JSElement js -> collectElements(js.children(), target);
            }
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasFatalErrors() {
        return errors.stream().anyMatch(ParseError::isFatal);
    }
}
