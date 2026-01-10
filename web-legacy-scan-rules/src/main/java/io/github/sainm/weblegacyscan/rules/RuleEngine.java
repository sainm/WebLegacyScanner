package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.scanner.RuleEvaluator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 规则引擎接口
 */
public sealed interface RuleEngine extends RuleEvaluator permits DefaultRuleEngine {
    
    /**
     * 从配置目录加载规�?
     */
    @Override
    void loadRules(Path configDir) throws IOException;

    /**
     * 重新加载规则
     */
    void reloadRules() throws IOException;

    /**
     * 评估代码元素
     * @param element 代码元素
     * @param filePath 文件路径（用于规则匹配）
     * @return 检测到的问题列�?
     */
    @Override
    List<Issue> evaluate(CodeElement element, String filePath);

    /**
     * 评估多个代码元素
     */
    @Override
    List<Issue> evaluateAll(List<CodeElement> elements, String filePath);

    /**
     * 获取指定分类的规�?
     */
    List<Rule> getRulesByCategory(RuleCategory category);

    /**
     * 获取所有规�?
     */
    List<Rule> getAllRules();

    /**
     * 获取规则注册�?
     */
    RuleRegistry getRegistry();
}
