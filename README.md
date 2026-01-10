# Web-Legacy-Scan

基于 MDN 权威标准的 Web 遗留代码检测与现代化建议工具。使用 Java 21 + Gradle KTS + 虚拟线程 + 模块化架构，精准识别 HTML、CSS、JavaScript 中已被现代 Web 标准废弃或不推荐使用的语法，并提供明确的替换建议。

## 特性

- 🔍 **全面检测** - 支持 HTML、CSS、JavaScript 废弃语法检测
- 📚 **MDN 标准** - 基于 MDN 文档提供权威的替换建议和参考链接
- ⚡ **高性能** - 使用 Java 21 虚拟线程实现高效并行扫描
- 🧩 **模块化** - JPMS 模块化架构，组件可独立使用
- 🔧 **可配置** - 支持自定义规则、严重级别、文件过滤
- 📊 **多格式报告** - 支持 JSON、Text、HTML、SARIF 输出格式
- 🔌 **多种集成** - 提供 CLI 工具和 Gradle 插件

## 系统要求

- Java 21 或更高版本
- Gradle 8.0 或更高版本（使用 Gradle 插件时）

## 快速开始

### 构建项目

```bash
./gradlew build
```

### CLI 使用

#### 基本扫描

```bash
# 扫描目录
java -jar web-legacy-scan-cli.jar /path/to/project

# 扫描单个文件
java -jar web-legacy-scan-cli.jar /path/to/file.html
```

#### 指定输出格式

```bash
# JSON 格式
java -jar web-legacy-scan-cli.jar /path/to/project --format JSON

# HTML 报告
java -jar web-legacy-scan-cli.jar /path/to/project --format HTML --output report.html

# SARIF 格式（IDE 集成）
java -jar web-legacy-scan-cli.jar /path/to/project --format SARIF --output results.sarif
```

#### 过滤和配置

```bash
# 只报告 WARNING 及以上级别
java -jar web-legacy-scan-cli.jar /path/to/project --severity WARNING

# 包含/排除文件模式
java -jar web-legacy-scan-cli.jar /path/to/project \
  --include "src/**/*.html,src/**/*.js" \
  --exclude "**/node_modules/**,**/vendor/**"

# 使用自定义配置
java -jar web-legacy-scan-cli.jar /path/to/project --config /path/to/config

# 严格模式（解析错误时失败）
java -jar web-legacy-scan-cli.jar /path/to/project --strict
```

#### 初始化配置

```bash
# 在当前目录生成默认配置文件
java -jar web-legacy-scan-cli.jar --init

# 在指定目录生成配置
java -jar web-legacy-scan-cli.jar --init --config /path/to/config
```

### CLI 完整选项

```
Usage: web-legacy-scan [OPTIONS] <targetPath>

Arguments:
  <targetPath>              Target path to scan (file or directory)

Options:
  -f, --format=<format>     Output format: JSON, TEXT, HTML, SARIF (default: TEXT)
  -c, --config=<path>       Configuration directory
  -o, --output=<path>       Output file path
  -s, --severity=<level>    Minimum severity: ERROR, WARNING, INFO (default: INFO)
      --fail-on=<level>     Fail on severity level
      --strict              Strict mode - fail on any parse error
      --output-all-elements Output all scanned elements with locations
      --no-progress         Disable progress output
  -j, --threads=<n>         Max concurrent threads (default: CPU cores)
      --include=<patterns>  Include glob patterns (comma-separated)
      --exclude=<patterns>  Exclude glob patterns (comma-separated)
      --init                Initialize default configuration files
  -q, --quiet               Quiet mode - minimal output
  -h, --help                Show help message
  -V, --version             Print version information
```

### 退出码

| 退出码 | 含义 |
|--------|------|
| 0 | 成功，无问题 |
| 1 | 发现问题 |
| 2 | 执行错误 |

## Gradle 插件使用

### 添加插件

```kotlin
// build.gradle.kts
plugins {
    id("io.github.sainm.web-legacy-scan") version "1.0.0"
}
```

### 配置插件

```kotlin
// build.gradle.kts
webLegacyScan {
    // 源目录
    sourceDirs.set(listOf("src/main/webapp", "src/main/resources/static"))
    
    // 配置目录
    configDir.set(file("config/legacy-scan"))
    
    // 报告输出目录
    reportDir.set(layout.buildDirectory.dir("reports/web-legacy-scan"))
    
    // 文件过滤
    includePatterns.set(listOf("**/*.html", "**/*.css", "**/*.js"))
    excludePatterns.set(listOf("**/node_modules/**", "**/vendor/**"))
    
    // 严重级别
    minSeverity.set("INFO")
    failOnSeverity.set("ERROR")
    
    // 发现问题时是否失败构建
    failOnError.set(true)
    
    // 输出格式
    outputFormat.set("HTML")
    
    // 并发线程数
    maxThreads.set(4)
}
```

### 运行扫描

```bash
# 运行扫描任务
./gradlew scanLegacyCode

# 作为 check 任务的一部分运行
./gradlew check
```

## 配置文件

### rules.json

```json
{
  "rules": [
    {
      "id": "html-deprecated-tag",
      "name": "Deprecated HTML Tag",
      "category": "HTML_DEPRECATED_TAG",
      "severity": "WARNING",
      "enabled": true,
      "patterns": ["font", "center", "marquee", "blink", "frame", "frameset"]
    },
    {
      "id": "css-vendor-prefix",
      "name": "CSS Vendor Prefix",
      "category": "CSS_VENDOR_PREFIX",
      "severity": "INFO",
      "enabled": true,
      "patterns": ["-webkit-", "-moz-", "-ms-", "-o-"]
    },
    {
      "id": "js-deprecated-api",
      "name": "Deprecated JavaScript API",
      "category": "JS_DEPRECATED_API",
      "severity": "WARNING",
      "enabled": true,
      "patterns": ["escape", "unescape", "document.write", "document.writeln"]
    }
  ]
}
```

### replacements.json

```json
{
  "replacements": {
    "font": {
      "suggestion": "Use CSS font properties (font-family, font-size, color)",
      "example": "<span style=\"font-family: Arial; font-size: 14px;\">Text</span>",
      "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/CSS/font"
    },
    "center": {
      "suggestion": "Use CSS text-align: center or flexbox",
      "example": "<div style=\"text-align: center;\">Content</div>",
      "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/CSS/text-align"
    },
    "escape": {
      "suggestion": "Use encodeURIComponent() instead",
      "example": "encodeURIComponent(str)",
      "mdnUrl": "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent"
    }
  }
}
```

## 检测规则

### HTML 废弃标签

| 标签 | 替代方案 |
|------|----------|
| `<font>` | CSS `font-family`, `font-size`, `color` |
| `<center>` | CSS `text-align: center` 或 Flexbox |
| `<marquee>` | CSS 动画 |
| `<blink>` | CSS 动画 |
| `<frame>`, `<frameset>` | `<iframe>` 或现代布局 |
| `<applet>` | 移除（Java Applet 已废弃）|

### HTML 废弃属性

| 属性 | 替代方案 |
|------|----------|
| `align` | CSS `text-align`, `vertical-align` |
| `bgcolor` | CSS `background-color` |
| `border` | CSS `border` |
| `onclick` 等内联事件 | `addEventListener()` |

### CSS 废弃属性

| 属性 | 替代方案 |
|------|----------|
| `clip` | `clip-path` |
| `zoom` | `transform: scale()` |
| Vendor prefixes | 标准属性 |

### JavaScript 废弃 API

| API | 替代方案 |
|-----|----------|
| `escape()` | `encodeURIComponent()` |
| `unescape()` | `decodeURIComponent()` |
| `document.write()` | DOM 操作方法 |
| `document.all` | `document.getElementById()` 等 |
| `var` | `let` / `const` |
| `eval()` | 避免使用 |

## 项目结构

```
web-legacy-scan/
├── web-legacy-scan-core/        # 核心扫描引擎
├── web-legacy-scan-rules/       # 规则引擎
├── web-legacy-scan-report/      # 报告生成器
├── web-legacy-scan-parser-html/ # HTML 解析器 (Jericho-HTML)
├── web-legacy-scan-parser-css/  # CSS 解析器 (ph-css)
├── web-legacy-scan-parser-js/   # JavaScript 解析器 (Closure Compiler)
├── web-legacy-scan-cli/         # CLI 应用
└── web-legacy-scan-gradle/      # Gradle 插件
```

## 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :web-legacy-scan-core:test

# 运行属性测试
./gradlew test --tests "*PropertyTest"
```

## 示例输出

### Text 格式

```
Scanning: /path/to/project
Progress: 42/42 files (100.0%)
Completed: 42 files in 1.23s

=== Web Legacy Scan Report ===

Issues Found: 5

[WARNING] src/main/webapp/index.html:15:5
  Rule: HTML_DEPRECATED_TAG
  Deprecated <font> tag detected
  Suggestion: Use CSS font properties (font-family, font-size, color)
  MDN: https://developer.mozilla.org/en-US/docs/Web/CSS/font

[WARNING] src/main/webapp/scripts/app.js:42:1
  Rule: JS_DEPRECATED_API
  Deprecated escape() function detected
  Suggestion: Use encodeURIComponent() instead
  MDN: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent

Summary:
  Total Files: 42
  Issues: 5 (ERROR: 0, WARNING: 5, INFO: 0)
  Scan Time: 1.23s
```

### JSON 格式

```json
{
  "issues": [
    {
      "ruleId": "html-deprecated-tag",
      "category": "HTML_DEPRECATED_TAG",
      "severity": "WARNING",
      "location": {
        "filePath": "src/main/webapp/index.html",
        "startLine": 15,
        "startColumn": 5,
        "endLine": 15,
        "endColumn": 25
      },
      "description": "Deprecated <font> tag detected",
      "suggestion": {
        "description": "Use CSS font properties",
        "modernAlternative": "font-family, font-size, color",
        "codeExample": "<span style=\"...\">Text</span>"
      },
      "mdnReference": "https://developer.mozilla.org/en-US/docs/Web/CSS/font"
    }
  ],
  "statistics": {
    "totalFiles": 42,
    "scannedFiles": 42,
    "totalIssues": 5,
    "scanTime": "1.23s"
  }
}
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
