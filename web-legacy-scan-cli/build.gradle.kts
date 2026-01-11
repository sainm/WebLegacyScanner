plugins {
    `java-library`
    application
}

application {
    mainClass.set("io.github.sainm.weblegacyscan.cli.ScannerMain")
}

dependencies {
    implementation(project(":web-legacy-scan-core"))
    implementation(project(":web-legacy-scan-rules"))
    implementation(project(":web-legacy-scan-report"))
    implementation(project(":web-legacy-scan-parser-html"))
    implementation(project(":web-legacy-scan-parser-css"))
    implementation(project(":web-legacy-scan-parser-js"))
}
