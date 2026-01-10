plugins {
    `java-library`
}

dependencies {
    api(project(":web-legacy-scan-core"))
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Property-based testing with jqwik
    testImplementation("net.jqwik:jqwik:1.8.2")
}

// 规则生成任务 (使用硬编码数据)
tasks.register<JavaExec>("generateRules") {
    group = "generation"
    description = "Generate rules.json from MDN browser-compat-data"
    mainClass.set("io.github.sainm.weblegacyscan.rules.generator.MDNRuleGenerator")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(rootProject.file("config/generated").absolutePath)
    jvmArgs("--enable-preview")
}

// 从 MDN API 动态获取规则
tasks.register<JavaExec>("fetchMDNRules") {
    group = "generation"
    description = "Fetch deprecated rules dynamically from MDN browser-compat-data API"
    mainClass.set("io.github.sainm.weblegacyscan.rules.generator.MDNDataFetcher")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(rootProject.file("config/generated").absolutePath)
    jvmArgs("--enable-preview")
}
