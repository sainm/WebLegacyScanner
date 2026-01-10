package io.github.sainm.weblegacyscan.report;

import io.github.sainm.weblegacyscan.core.scanner.ScanResult;

/**
 * 报告格式化器接口
 */
public sealed interface ReportFormatter 
    permits JSONFormatter, TextFormatter, HTMLFormatter, SARIFFormatter {
    
    /**
     * 格式化扫描结�?
     * @param result 扫描结果
     * @param config 报告配置
     * @return 格式化后的字符串
     */
    String format(ScanResult result, ReportConfig config);

    /**
     * 获取输出格式
     */
    OutputFormat getFormat();
}
