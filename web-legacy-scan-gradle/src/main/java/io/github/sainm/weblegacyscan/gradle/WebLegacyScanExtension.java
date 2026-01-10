package io.github.sainm.weblegacyscan.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Gradle 插件扩展配置
 */
public abstract class WebLegacyScanExtension {

    /**
     * 源目录列�?
     */
    public abstract ListProperty<String> getSourceDirs();

    /**
     * 配置目录
     */
    public abstract DirectoryProperty getConfigDir();

    /**
     * 报告输出目录
     */
    public abstract DirectoryProperty getReportDir();

    /**
     * 包含的文件模�?
     */
    public abstract ListProperty<String> getIncludePatterns();

    /**
     * 排除的文件模�?
     */
    public abstract ListProperty<String> getExcludePatterns();

    /**
     * 最小严重级�?
     */
    public abstract Property<String> getMinSeverity();

    /**
     * 失败时的严重级别
     */
    public abstract Property<String> getFailOnSeverity();

    /**
     * 是否在发现问题时失败构建
     */
    public abstract Property<Boolean> getFailOnError();

    /**
     * 是否启用增量构建
     */
    public abstract Property<Boolean> getIncremental();

    /**
     * 输出格式
     */
    public abstract Property<String> getOutputFormat();

    /**
     * 是否输出所有元�?
     */
    public abstract Property<Boolean> getOutputAllElements();

    /**
     * 最大并发线程数
     */
    public abstract Property<Integer> getMaxThreads();
}
