package io.github.sainm.weblegacyscan.core.scanner;

import io.github.sainm.weblegacyscan.core.executor.*;
import io.github.sainm.weblegacyscan.core.file.*;
import io.github.sainm.weblegacyscan.core.model.*;
import io.github.sainm.weblegacyscan.core.parser.*;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult.ScanStatistics;
import io.github.sainm.weblegacyscan.core.scanner.ScanResult.ScanError;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认扫描器实�?- Facade 模式
 */
public final class DefaultScanner implements Scanner {

    private final FileDiscoverer fileDiscoverer;
    private final RuleEvaluator ruleEvaluator;
    private final VirtualThreadExecutor executor;
    private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public DefaultScanner(FileDiscoverer fileDiscoverer, RuleEvaluator ruleEvaluator) {
        this(fileDiscoverer, ruleEvaluator, null);
    }

    public DefaultScanner(FileDiscoverer fileDiscoverer, RuleEvaluator ruleEvaluator, 
                          VirtualThreadExecutor executor) {
        this.fileDiscoverer = Objects.requireNonNull(fileDiscoverer);
        this.ruleEvaluator = Objects.requireNonNull(ruleEvaluator);
        this.executor = executor;
    }

    @Override
    public ScanResult scan(ScanConfig config) {
        cancelled.set(false);
        Instant start = Instant.now();

        // 加载规则配置
        if (config.configDir() != null) {
            try {
                ruleEvaluator.loadRules(config.configDir());
            } catch (IOException e) {
                return ScanResult.builder()
                    .addError(new ScanError(config.configDir().toString(), 
                        "Failed to load rules: " + e.getMessage(), true))
                    .build();
            }
        }

        // 发现文件
        List<SourceFile> files = fileDiscoverer.discover(config.targetPath(), config.filterConfig())
            .toList();

        if (files.isEmpty()) {
            return ScanResult.builder()
                .statistics(ScanStatistics.builder()
                    .totalFiles(0)
                    .scannedFiles(0)
                    .scanTime(Duration.between(start, Instant.now()))
                    .build())
                .build();
        }

        // 创建执行�?
        VirtualThreadExecutor exec = executor != null 
            ? executor 
            : new DefaultVirtualThreadExecutor(config.maxConcurrency());

        // 转发进度监听
        for (ProgressListener listener : listeners) {
            exec.addProgressListener(listener);
        }

        // 创建扫描任务
        List<Callable<FileScanResult>> tasks = files.stream()
            .map(file -> (Callable<FileScanResult>) () -> scanFile(file, config))
            .toList();

        // 并行执行扫描
        List<FileScanResult> results = exec.executeAll(tasks);

        // 合并结果
        return mergeResults(results, files.size(), start, config);
    }

    private FileScanResult scanFile(SourceFile file, ScanConfig config) {
        if (cancelled.get()) {
            return FileScanResult.cancelled(file);
        }

        List<CodeElement> elements = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // 获取解析�?
        Optional<Parser> parserOpt = ParserFactory.getParser(file.type());
        if (parserOpt.isEmpty()) {
            return new FileScanResult(file, List.of(), List.of(), List.of(), false);
        }

        Parser parser = parserOpt.get();

        try {
            // 检测编�?
            Charset encoding = detectEncoding(file, config.defaultEncoding());
            SourceFile fileWithEncoding = SourceFile.of(file.path(), file.type(), encoding, file.size());

            // 解析文件
            ParseResult parseResult = parser.parse(fileWithEncoding);
            elements.addAll(parseResult.elements());
            errors.addAll(parseResult.errors());

            // strict 模式下，解析错误导致失败
            if (config.strictMode() && parseResult.hasFatalErrors()) {
                return new FileScanResult(file, elements, issues, errors, false);
            }

            // 应用规则
            for (CodeElement element : parseResult.getAllElementsFlat()) {
                List<Issue> elementIssues = ruleEvaluator.evaluate(element, file.path().toString());
                issues.addAll(elementIssues);
            }

        } catch (Exception e) {
            errors.add(ParseError.fatal(file.path(), 1, 1, "Scan error: " + e.getMessage()));
            if (config.strictMode()) {
                return new FileScanResult(file, elements, issues, errors, false);
            }
        }

        return new FileScanResult(file, elements, issues, errors, true);
    }

    private Charset detectEncoding(SourceFile file, Charset defaultEncoding) {
        try {
            byte[] bytes = Files.readAllBytes(file.path());
            
            // 检�?BOM
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && 
                bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return java.nio.charset.StandardCharsets.UTF_8;
            }
            if (bytes.length >= 2) {
                if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    return java.nio.charset.StandardCharsets.UTF_16BE;
                }
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    return java.nio.charset.StandardCharsets.UTF_16LE;
                }
            }
        } catch (IOException ignored) {
        }
        return defaultEncoding;
    }

    private ScanResult mergeResults(List<FileScanResult> results, int totalFiles, 
                                    Instant start, ScanConfig config) {
        List<Issue> allIssues = new ArrayList<>();
        List<CodeElement> allElements = new ArrayList<>();
        List<ScanError> allErrors = new ArrayList<>();
        int scannedFiles = 0;

        for (FileScanResult result : results) {
            if (result.success()) {
                scannedFiles++;
            }
            
            // 过滤严重级别
            result.issues().stream()
                .filter(i -> i.severity().isAtLeast(config.minSeverity()))
                .forEach(allIssues::add);

            if (config.outputAllElements()) {
                allElements.addAll(result.elements());
            }

            result.errors().stream()
                .map(ScanError::from)
                .forEach(allErrors::add);
        }

        Duration scanTime = Duration.between(start, Instant.now());
        ScanStatistics stats = ScanStatistics.builder()
            .totalFiles(totalFiles)
            .scannedFiles(scannedFiles)
            .totalElements(allElements.size())
            .scanTime(scanTime)
            .build();

        return ScanResult.builder()
            .issues(allIssues)
            .allElements(allElements)
            .statistics(stats)
            .errors(allErrors)
            .build();
    }

    @Override
    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProgressListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        if (executor != null) {
            executor.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 单文件扫描结�?
     */
    private record FileScanResult(
        SourceFile file,
        List<CodeElement> elements,
        List<Issue> issues,
        List<ParseError> errors,
        boolean success
    ) {
        static FileScanResult cancelled(SourceFile file) {
            return new FileScanResult(file, List.of(), List.of(), List.of(), false);
        }
    }
}
