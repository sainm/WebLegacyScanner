package io.github.sainm.weblegacyscan.core.file;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 源文件信�?
 */
public record SourceFile(
    Path path,
    FileType type,
    Charset encoding,
    long size
) {
    public SourceFile {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        if (encoding == null) encoding = StandardCharsets.UTF_8;
        if (size < 0) size = 0;
    }

    public static SourceFile of(Path path) {
        return new SourceFile(path, FileType.fromPath(path), StandardCharsets.UTF_8, 0);
    }

    public static SourceFile of(Path path, long size) {
        return new SourceFile(path, FileType.fromPath(path), StandardCharsets.UTF_8, size);
    }

    public static SourceFile of(Path path, FileType type, Charset encoding, long size) {
        return new SourceFile(path, type, encoding, size);
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public String getExtension() {
        String fileName = getFileName();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    public boolean isWebFile() {
        return type.isWebFile();
    }
}
