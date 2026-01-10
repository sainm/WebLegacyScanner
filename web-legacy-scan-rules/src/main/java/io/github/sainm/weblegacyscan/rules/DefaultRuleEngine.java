package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * 默认规则引擎实现
 */
public final class DefaultRuleEngine implements RuleEngine {

    private final RuleRegistry registry = new RuleRegistry();
    private final RuleLoader loader = new RuleLoader();
    private final RuleFactory factory = new RuleFactory();
    private Path configDir;

    @Override
    public void loadRules(Path configDir) throws IOException {
        this.configDir = configDir;
        registry.clear();

        // 加载默认规则
        List<Rule> defaultRules = factory.createDefaultRules();
        registry.registerAll(defaultRules);

        // 加载自定义规则（覆盖默认规则�?
        if (configDir != null && configDir.toFile().exists()) {
            List<RuleDefinition> customDefinitions = loader.loadRules(configDir);
            for (RuleDefinition def : customDefinitions) {
                Rule rule = factory.createRule(def);
                registry.register(rule);
            }
        }
    }

    @Override
    public void reloadRules() throws IOException {
        if (configDir != null) {
            loadRules(configDir);
        }
    }

    @Override
    public List<Issue> evaluate(CodeElement element, String filePath) {
        List<Issue> issues = new ArrayList<>();
        List<Rule> applicableRules = registry.getRulesForFile(filePath);

        for (Rule rule : applicableRules) {
            rule.evaluate(element).ifPresent(issues::add);
        }

        return issues;
    }

    @Override
    public List<Issue> evaluateAll(List<CodeElement> elements, String filePath) {
        List<Issue> issues = new ArrayList<>();
        for (CodeElement element : elements) {
            issues.addAll(evaluate(element, filePath));
        }
        return issues;
    }

    @Override
    public List<Rule> getRulesByCategory(RuleCategory category) {
        return registry.getRulesByCategory(category);
    }

    @Override
    public List<Rule> getAllRules() {
        return registry.getAllRules();
    }

    @Override
    public RuleRegistry getRegistry() {
        return registry;
    }
}
