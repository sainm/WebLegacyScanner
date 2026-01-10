package io.github.sainm.weblegacyscan.core.file;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * File discoverer interface
 */
public sealed interface FileDiscoverer permits DefaultFileDiscoverer {
    
    /**
     * Discover all matching files under the specified path
     * @param root root path (file or directory)
     * @param config filter configuration
     * @return stream of matching source files
     */
    Stream<SourceFile> discover(Path root, FilterConfig config);
    
    /**
     * Discover files using default configuration
     */
    default Stream<SourceFile> discover(Path root) {
        return discover(root, FilterConfig.defaults());
    }
}
