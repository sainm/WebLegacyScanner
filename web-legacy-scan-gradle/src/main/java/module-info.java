module web.legacy.scan.gradle {
    requires web.legacy.scan.core;
    requires web.legacy.scan.rules;
    requires web.legacy.scan.report;
    requires web.legacy.scan.parser.html;
    requires web.legacy.scan.parser.css;
    requires web.legacy.scan.parser.js;
    requires org.gradle.api;
    
    exports io.github.sainm.weblegacyscan.gradle;
}
