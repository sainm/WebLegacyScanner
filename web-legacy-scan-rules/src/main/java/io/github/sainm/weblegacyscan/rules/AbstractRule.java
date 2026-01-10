package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;

import java.util.Optional;

/**
 * 抽象规则基类
 */
public abstract class AbstractRule implements Rule {
    
    protected final RuleDefinition definition;
    private Rule next;

    protected AbstractRule(RuleDefinition definition) {
        this.definition = definition;
    }

    @Override
    public String getId() {
        return definition.id();
    }

    @Override
    public RuleCategory getCategory() {
        return definition.category();
    }

    @Override
    public SeverityLevel getSeverity() {
        return definition.severity();
    }

    @Override
    public Optional<Issue> evaluate(CodeElement element) {
        if (!isEnabled()) {
            return evaluateNext(element);
        }

        Optional<Issue> result = doEvaluate(element);
        if (result.isPresent()) {
            return result;
        }
        return evaluateNext(element);
    }

    protected abstract Optional<Issue> doEvaluate(CodeElement element);

    private Optional<Issue> evaluateNext(CodeElement element) {
        return next != null ? next.evaluate(element) : Optional.empty();
    }

    @Override
    public Optional<Rule> getNext() {
        return Optional.ofNullable(next);
    }

    @Override
    public void setNext(Rule next) {
        this.next = next;
    }

    @Override
    public boolean isEnabled() {
        return definition.enabled();
    }

    @Override
    public boolean matchesFile(String filePath) {
        return definition.matchesFile(filePath);
    }

    protected Issue createIssue(CodeElement element, String description) {
        return Issue.builder()
            .ruleId(getId())
            .category(getCategory())
            .severity(getSeverity())
            .location(element.getLocation())
            .description(description)
            .suggestion(definition.suggestion())
            .mdnReference(definition.mdnReference())
            .build();
    }
}
