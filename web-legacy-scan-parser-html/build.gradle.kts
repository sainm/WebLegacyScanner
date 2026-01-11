plugins {
    `java-library`
}

dependencies {
    api(project(":web-legacy-scan-core"))
    api("net.htmlparser.jericho:jericho-html:3.4")
    api("com.helger:ph-css:7.0.2")
    
    // Property-based testing with jqwik
    testImplementation("net.jqwik:jqwik:1.8.2")
}

// Exclude module-info.java from compilation to work around automatic module issues
// The jericho-html library doesn't have a proper module descriptor
tasks.withType<JavaCompile> {
    exclude("**/module-info.java")
}
