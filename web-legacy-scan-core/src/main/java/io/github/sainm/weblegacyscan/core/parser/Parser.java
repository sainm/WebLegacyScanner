package io.github.sainm.weblegacyscan.core.parser;

import io.github.sainm.weblegacyscan.core.file.FileType;
import io.github.sainm.weblegacyscan.core.file.SourceFile;

/**
 * 解析器接�?
 */
public interface Parser {
    
    /**
     * 解析源文�?
     * @param file 源文�?
     * @return 解析结果
     */
    ParseResult parse(SourceFile file);

    /**
     * 解析字符串内�?
     * @param content 内容
     * @param file 源文件（用于位置信息�?
     * @return 解析结果
     */
    ParseResult parseContent(String content, SourceFile file);

    /**
     * 获取支持的文件类�?
     */
    FileType getSupportedType();
}
