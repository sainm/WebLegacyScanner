package io.github.sainm.weblegacyscan.core.file;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * Default file discoverer implementation
 */
public final class DefaultFileDiscoverer implements FileDiscoverer {

    private final Set<Path> visitedPaths = Collections.synchronizedSet(new HashSet<>());

    @Override
    public Stream<SourceFile> discover(Path root, FilterConfig config) {
        visitedPaths.clear();
        
        if (!Files.exists(root)) {
            return Stream.empty();
        }

        if (Files.isRegularFile(root)) {
            return handleSingleFile(root, config);
        }

        return discoverDirectory(root, config);
    }

    private Stream<SourceFile> handleSingleFile(Path file, FilterConfig config) {
        if (shouldIncludeFile(file, config)) {
            return Stream.of(createSourceFile(file));
        }
        return Stream.empty();
    }

    private Stream<SourceFile> discoverDirectory(Path root, FilterConfig config) {
        List<SourceFile> files = new ArrayList<>();
        Set<String> gitignorePatterns = config.respectGitignore() 
            ? loadGitignorePatterns(root) 
            : Set.of();
        Set<String> legacyScanIgnorePatterns = loadLegacyScanIgnorePatterns(root);

        try {
            Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        // Detect circular symbolic links
                        try {
                            Path realPath = dir.toRealPath();
                            if (!visitedPaths.add(realPath)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        } catch (IOException e) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        // Check if directory should be excluded
                        if (shouldExcludeDirectory(dir, root, config, gitignorePatterns, legacyScanIgnorePatterns)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (shouldIncludeFile(file, root, config, gitignorePatterns, legacyScanIgnorePatterns, attrs)) {
                            files.add(createSourceFile(file, attrs.size()));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        // 忽略无法访问的文件，继续扫描
                        return FileVisitResult.CONTINUE;
                    }
                });
        } catch (IOException e) {
            // 返回已发现的文件
        }

        return files.stream();
    }

    private boolean shouldIncludeFile(Path file, FilterConfig config) {
        String extension = getExtension(file);
        return config.includeExtensions().contains(extension);
    }

    private boolean shouldIncludeFile(Path file, Path root, FilterConfig config,
                                      Set<String> gitignorePatterns, 
                                      Set<String> legacyScanIgnorePatterns,
                                      BasicFileAttributes attrs) {
        // Check file size
        if (attrs.size() > config.maxFileSizeBytes()) {
            return false;
        }

        // Check if binary file
        if (isBinaryFile(file)) {
            return false;
        }

        // Check extension
        String extension = getExtension(file);
        if (!config.includeExtensions().contains(extension)) {
            return false;
        }

        String relativePath = root.relativize(file).toString().replace('\\', '/');

        // Check gitignore patterns
        if (matchesAnyPattern(relativePath, gitignorePatterns)) {
            return false;
        }

        // Check legacyscanignore patterns
        if (matchesAnyPattern(relativePath, legacyScanIgnorePatterns)) {
            return false;
        }

        // Check exclude patterns
        if (matchesAnyGlob(relativePath, config.excludePatterns())) {
            return false;
        }

        // Check include patterns (if specified)
        if (!config.includePatterns().isEmpty()) {
            return matchesAnyGlob(relativePath, config.includePatterns());
        }

        return true;
    }

    private boolean shouldExcludeDirectory(Path dir, Path root, FilterConfig config,
                                           Set<String> gitignorePatterns,
                                           Set<String> legacyScanIgnorePatterns) {
        String dirName = dir.getFileName().toString();
        
        // Common excluded directories
        if (dirName.equals("node_modules") || dirName.equals(".git") || 
            dirName.equals("vendor") || dirName.equals("dist") ||
            dirName.equals("build") || dirName.equals("target")) {
            return true;
        }

        if (dir.equals(root)) {
            return false;
        }

        String relativePath = root.relativize(dir).toString().replace('\\', '/') + "/";

        if (matchesAnyPattern(relativePath, gitignorePatterns)) {
            return true;
        }

        if (matchesAnyPattern(relativePath, legacyScanIgnorePatterns)) {
            return true;
        }

        return matchesAnyGlob(relativePath, config.excludePatterns());
    }

    private Set<String> loadGitignorePatterns(Path root) {
        Path gitignore = root.resolve(".gitignore");
        return loadPatternsFromFile(gitignore);
    }

    private Set<String> loadLegacyScanIgnorePatterns(Path root) {
        Path ignoreFile = root.resolve(".legacyscanignore");
        return loadPatternsFromFile(ignoreFile);
    }

    private Set<String> loadPatternsFromFile(Path file) {
        if (!Files.exists(file)) {
            return Set.of();
        }
        try {
            Set<String> patterns = new HashSet<>();
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    patterns.add(line);
                }
            }
            return patterns;
        } catch (IOException e) {
            return Set.of();
        }
    }

    private boolean matchesAnyPattern(String path, Set<String> patterns) {
        for (String pattern : patterns) {
            if (matchesGitignorePattern(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGitignorePattern(String path, String pattern) {
        // Simplified gitignore pattern matching
        if (pattern.endsWith("/")) {
            // Directory pattern
            String dirPattern = pattern.substring(0, pattern.length() - 1);
            return path.startsWith(dirPattern + "/") || path.contains("/" + dirPattern + "/");
        }
        
        if (pattern.contains("/")) {
            // Path pattern
            return matchesGlob(path, pattern);
        }
        
        // Filename pattern
        String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return matchesGlob(fileName, pattern) || matchesGlob(path, "**/" + pattern);
    }

    private boolean matchesAnyGlob(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesGlob(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String path, String pattern) {
        // Convert glob pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("**", "§§")
            .replace("*", "[^/]*")
            .replace("§§", ".*")
            .replace("?", ".");
        return path.matches(regex);
    }

    private boolean isBinaryFile(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            int checkLength = Math.min(bytes.length, 8000);
            for (int i = 0; i < checkLength; i++) {
                if (bytes[i] == 0) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private String getExtension(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    private SourceFile createSourceFile(Path file) {
        try {
            long size = Files.size(file);
            return createSourceFile(file, size);
        } catch (IOException e) {
            return createSourceFile(file, 0);
        }
    }

    private SourceFile createSourceFile(Path file, long size) {
        FileType type = FileType.fromPath(file);
        Charset encoding = detectEncoding(file);
        return new SourceFile(file, type, encoding, size);
    }

    private Charset detectEncoding(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && 
                bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
            if (bytes.length >= 2) {
                if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    return StandardCharsets.UTF_16BE;
                }
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    return StandardCharsets.UTF_16LE;
                }
            }
            return StandardCharsets.UTF_8;
        } catch (IOException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
