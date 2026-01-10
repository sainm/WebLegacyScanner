package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.RuleCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 规则注册�?
 */
public class RuleRegistry {

    private final Map<String, Rule> rulesById = new ConcurrentHashMap<>();
    private final Map<RuleCategory, List<Rule>> rulesByCategory = new ConcurrentHashMap<>();

    public void register(Rule rule) {
        rulesById.put(rule.getId(), rule);
        rulesByCategory.computeIfAbsent(rule.getCategory(), k -> new ArrayList<>()).add(rule);
    }

    public void registerAll(Collection<Rule> rules) {
        rules.forEach(this::register);
    }

    public Optional<Rule> getRule(String id) {
        return Optional.ofNullable(rulesById.get(id));
    }

    public List<Rule> getRulesByCategory(RuleCategory category) {
        return rulesByCategory.getOrDefault(category, List.of());
    }

    public List<Rule> getAllRules() {
        return new ArrayList<>(rulesById.values());
    }

    public List<Rule> getEnabledRules() {
        return rulesById.values().stream()
            .filter(Rule::isEnabled)
            .collect(Collectors.toList());
    }

    public List<Rule> getRulesForFile(String filePath) {
        return rulesById.values().stream()
            .filter(Rule::isEnabled)
            .filter(rule -> rule.matchesFile(filePath))
            .collect(Collectors.toList());
    }

    public void clear() {
        rulesById.clear();
        rulesByCategory.clear();
    }

    public int size() {
        return rulesById.size();
    }
}
