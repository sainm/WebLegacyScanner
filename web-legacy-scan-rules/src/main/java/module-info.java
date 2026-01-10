module web.legacy.scan.rules {
    requires transitive web.legacy.scan.core;
    requires com.google.gson;
    requires java.net.http;
    
    exports io.github.sainm.weblegacyscan.rules;
    exports io.github.sainm.weblegacyscan.rules.generator;
}
