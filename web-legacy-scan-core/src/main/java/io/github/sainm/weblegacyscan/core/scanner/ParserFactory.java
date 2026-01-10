package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.parser.Parser;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parser factory - creates parsers based on file type
 */
public final class ParserFactory {

    private static final Map<FileType, Parser> parsers = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private ParserFactory() {}

    /**
     * Get parser for specified file type
     */
    public static Optional<Parser> getParser(FileType fileType) {
        ensureInitialized();
        return Optional.ofNullable(parsers.get(fileType));
    }

    /**
     * Register a parser
     */
    public static void registerParser(Parser parser) {
        parsers.put(parser.getSupportedType(), parser);
    }

    /**
     * Check if specified file type is supported
     */
    public static boolean isSupported(FileType fileType) {
        ensureInitialized();
        return parsers.containsKey(fileType);
    }

    /**
     * Initialize parsers (via ServiceLoader or manual registration)
     */
    private static synchronized void ensureInitialized() {
        if (initialized) return;
        
        // Try to load parsers via ServiceLoader
        ServiceLoader<Parser> loader = ServiceLoader.load(Parser.class);
        for (Parser parser : loader) {
            parsers.put(parser.getSupportedType(), parser);
        }
        
        initialized = true;
    }

    /**
     * Reset factory (for testing)
     */
    public static synchronized void reset() {
        parsers.clear();
        initialized = false;
    }
}
