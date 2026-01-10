plugins {
    `java-library`
}

dependencies {
    api("com.google.code.gson:gson:2.10.1")
    
    // Property-based testing with jqwik
    testImplementation("net.jqwik:jqwik:1.8.2")
}
