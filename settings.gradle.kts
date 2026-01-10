rootProject.name = "web-legacy-scan"

// 核心模块
include("web-legacy-scan-core")
include("web-legacy-scan-rules")
include("web-legacy-scan-report")

// 解析器模块
include("web-legacy-scan-parser-html")
include("web-legacy-scan-parser-css")
include("web-legacy-scan-parser-js")

// 应用模块
include("web-legacy-scan-cli")
include("web-legacy-scan-gradle")
