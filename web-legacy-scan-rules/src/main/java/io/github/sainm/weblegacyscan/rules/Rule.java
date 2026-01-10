package io.github.sainm.weblegacyscan.rules;

import io.github.sainm.weblegacyscan.core.model.*;

import java.util.Optional;

/**
 * 规则接口（Chain of Responsibility 模式�?
 */
public interface Rule {
    
    /**
     * 获取规则 ID
     */
    String getId();

    /**
     * 获取规则分类
     */
    RuleCategory getCategory();

    /**
     * 获取严重级别
     */
    SeverityLevel getSeverity();

    /**
     * 评估代码元素是否违反规则
     * @param element 代码元素
     * @return 如果违反规则，返�?Issue；否则返回空
     */
    Optional<Issue> evaluate(CodeElement element);

    /**
     * 获取下一个规则（责任链）
     */
    Optional<Rule> getNext();

    /**
     * 设置下一个规�?
     */
    void setNext(Rule next);

    /**
     * 检查规则是否启�?
     */
    boolean isEnabled();

    /**
     * 检查规则是否适用于指定文�?
     */
    boolean matchesFile(String filePath);
}
