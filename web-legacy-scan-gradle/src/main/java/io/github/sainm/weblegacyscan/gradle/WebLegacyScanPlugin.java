package io.github.sainm.weblegacyscan.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.List;

/**
 * Web Legacy Scan Gradle plugin.
 */
public class WebLegacyScanPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "webLegacyScan";
    public static final String TASK_NAME = "scanLegacyCode";

    @Override
    public void apply(Project project) {
        // Create extension
        WebLegacyScanExtension extension = project.getExtensions()
            .create(EXTENSION_NAME, WebLegacyScanExtension.class);

        // Set defaults
        extension.getSourceDirs().convention(List.of("src/main/webapp", "src/main/resources/static"));
        extension.getReportDir().convention(project.getLayout().getBuildDirectory().dir("reports/web-legacy-scan"));
        extension.getFailOnError().convention(false);
        extension.getIncremental().convention(true);
        extension.getOutputFormat().convention("HTML");
        extension.getOutputAllElements().convention(false);
        extension.getMaxThreads().convention(Runtime.getRuntime().availableProcessors());
        extension.getMinSeverity().convention("INFO");

        // Register task
        TaskProvider<WebLegacyScanTask> scanTask = project.getTasks()
            .register(TASK_NAME, WebLegacyScanTask.class, task -> {
                task.setGroup("verification");
                task.setDescription("Scan web files for deprecated HTML, CSS, and JavaScript patterns");

                // Configure task from extension
                task.getSourceDirs().set(extension.getSourceDirs());
                task.getConfigDir().set(extension.getConfigDir());
                task.getReportDir().set(extension.getReportDir());
                task.getIncludePatterns().set(extension.getIncludePatterns());
                task.getExcludePatterns().set(extension.getExcludePatterns());
                task.getMinSeverity().set(extension.getMinSeverity());
                task.getFailOnSeverity().set(extension.getFailOnSeverity());
                task.getFailOnError().set(extension.getFailOnError());
                task.getOutputFormat().set(extension.getOutputFormat());
                task.getOutputAllElements().set(extension.getOutputAllElements());
                task.getMaxThreads().set(extension.getMaxThreads());
            });

        // If Java plugin is present, add task to check task
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            project.getTasks().named("check").configure(check -> {
                check.dependsOn(scanTask);
            });
        });
    }
}
