package io.github.sainm.weblegacyscan.core.file;

import java.nio.file.Path;
import java.util.Set;

/**
 * 文件类型
 */
public enum FileType {
    HTML(Set.of(".html", ".htm")),
    JSP(Set.of(".jsp", ".jspx")),
    CSS(Set.of(".css", ".scss", ".less")),
    JAVASCRIPT(Set.of(".js", ".mjs", ".jsx")),
    TYPESCRIPT(Set.of(".ts", ".tsx")),
    UNKNOWN(Set.of());

    private final Set<String> extensions;

    FileType(Set<String> extensions) {
        this.extensions = extensions;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public static FileType fromPath(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return UNKNOWN;
        
        String extension = fileName.substring(dotIndex);
        for (FileType type : values()) {
            if (type.extensions.contains(extension)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public static FileType fromExtension(String extension) {
        String ext = extension.startsWith(".") ? extension.toLowerCase() : "." + extension.toLowerCase();
        for (FileType type : values()) {
            if (type.extensions.contains(ext)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public boolean isWebFile() {
        return this != UNKNOWN;
    }
}
