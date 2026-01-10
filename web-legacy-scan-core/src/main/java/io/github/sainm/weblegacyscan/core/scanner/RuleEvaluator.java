package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.model.CodeElement;
import io.github.sainm.weblegacyscan.core.model.Issue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 规则评估器接�?- 用于 Scanner 与规则引擎的解�?
 */
public interface RuleEvaluator {
    
    /**
     * 从配置目录加载规�?
     */
    void loadRules(Path configDir) throws IOException;

    /**
     * 评估代码元素
     * @param element 代码元素
     * @param filePath 文件路径（用于规则匹配）
     * @return 检测到的问题列�?
     */
    List<Issue> evaluate(CodeElement element, String filePath);

    /**
     * 评估多个代码元素
     */
    default List<Issue> evaluateAll(List<CodeElement> elements, String filePath) {
        return elements.stream()
            .flatMap(e -> evaluate(e, filePath).stream())
            .toList();
    }
}
