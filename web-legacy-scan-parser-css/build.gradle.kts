plugins {
    `java-library`
}

dependencies {
    api(project(":web-legacy-scan-core"))
    implementation("com.helger:ph-css:7.0.2")
    
    // Property-based testing with jqwik
    testImplementation("net.jqwik:jqwik:1.8.2")
}

// Exclude module-info.java from compilation to work around automatic module issues
// The ph-css library doesn't have a proper module descriptor
sourceSets {
    main {
        java {
            exclude("module-info.java")
        }
    }
}
