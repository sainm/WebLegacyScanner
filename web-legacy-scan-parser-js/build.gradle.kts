plugins {
    `java-library`
}

dependencies {
    api(project(":web-legacy-scan-core"))
    implementation("com.google.javascript:closure-compiler:v20231112")
    
    // Property-based testing with jqwik
    testImplementation("net.jqwik:jqwik:1.8.2")
}

// Exclude module-info.java from compilation to work around automatic module issues
// The closure-compiler library doesn't have a proper module descriptor
sourceSets {
    main {
        java {
            exclude("module-info.java")
        }
    }
}
