package io.github.sainm.weblegacyscan.core.file;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Source file information
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

    /**
     * Create SourceFile from path, auto-detect file size
     */
    public static SourceFile of(Path path) {
        long size = 0;
        try {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                size = Files.size(path);
            }
        } catch (IOException e) {
            // ignore, use 0
        }
        return new SourceFile(path, FileType.fromPath(path), StandardCharsets.UTF_8, size);
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
