package io.github.sainm.weblegacyscan.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Gradle plugin extension configuration.
 */
public abstract class WebLegacyScanExtension {

    /**
     * Source directories list.
     */
    public abstract ListProperty<String> getSourceDirs();

    /**
     * Configuration directory.
     */
    public abstract DirectoryProperty getConfigDir();

    /**
     * Report output directory.
     */
    public abstract DirectoryProperty getReportDir();

    /**
     * Include file patterns.
     */
    public abstract ListProperty<String> getIncludePatterns();

    /**
     * Exclude file patterns.
     */
    public abstract ListProperty<String> getExcludePatterns();

    /**
     * Minimum severity level.
     */
    public abstract Property<String> getMinSeverity();

    /**
     * Severity level that triggers build failure.
     */
    public abstract Property<String> getFailOnSeverity();

    /**
     * Whether to fail the build when issues are found.
     */
    public abstract Property<Boolean> getFailOnError();

    /**
     * Whether to enable incremental build.
     */
    public abstract Property<Boolean> getIncremental();

    /**
     * Output format.
     */
    public abstract Property<String> getOutputFormat();

    /**
     * Whether to output all elements.
     */
    public abstract Property<Boolean> getOutputAllElements();

    /**
     * Maximum concurrent threads.
     */
    public abstract Property<Integer> getMaxThreads();
}
