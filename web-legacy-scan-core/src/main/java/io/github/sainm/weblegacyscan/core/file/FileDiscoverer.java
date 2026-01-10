package io.github.sainm.weblegacyscan.core.file;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 文件发现器接�?
 */
public sealed interface FileDiscoverer permits DefaultFileDiscoverer {
    
    /**
     * 发现指定路径下的所有匹配文�?
     * @param root 根路径（文件或目录）
     * @param config 过滤配置
     * @return 匹配的源文件�?
     */
    Stream<SourceFile> discover(Path root, FilterConfig config);
    
    /**
     * 使用默认配置发现文件
     */
    default Stream<SourceFile> discover(Path root) {
        return discover(root, FilterConfig.defaults());
    }
}
