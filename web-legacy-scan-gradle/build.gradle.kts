plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("webLegacyScan") {
            id = "com.example.web-legacy-scan"
            implementationClass = "com.example.weblegacyscan.gradle.WebLegacyScanPlugin"
        }
    }
}

dependencies {
    implementation(project(":web-legacy-scan-core"))
    implementation(project(":web-legacy-scan-rules"))
    implementation(project(":web-legacy-scan-report"))
    implementation(project(":web-legacy-scan-parser-html"))
    implementation(project(":web-legacy-scan-parser-css"))
    implementation(project(":web-legacy-scan-parser-js"))
}
