package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.parser.Parser;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析器工�?- 根据文件类型创建对应的解析器
 */
public final class ParserFactory {

    private static final Map<FileType, Parser> parsers = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private ParserFactory() {}

    /**
     * 获取指定文件类型的解析器
     */
    public static Optional<Parser> getParser(FileType fileType) {
        ensureInitialized();
        return Optional.ofNullable(parsers.get(fileType));
    }

    /**
     * 注册解析�?
     */
    public static void registerParser(Parser parser) {
        parsers.put(parser.getSupportedType(), parser);
    }

    /**
     * 检查是否支持指定文件类�?
     */
    public static boolean isSupported(FileType fileType) {
        ensureInitialized();
        return parsers.containsKey(fileType);
    }

    /**
     * 初始化解析器（通过 ServiceLoader 或手动注册）
     */
    private static synchronized void ensureInitialized() {
        if (initialized) return;
        
        // 尝试通过 ServiceLoader 加载解析�?
        ServiceLoader<Parser> loader = ServiceLoader.load(Parser.class);
        for (Parser parser : loader) {
            parsers.put(parser.getSupportedType(), parser);
        }
        
        initialized = true;
    }

    /**
     * 重置工厂（用于测试）
     */
    public static synchronized void reset() {
        parsers.clear();
        initialized = false;
    }
}
