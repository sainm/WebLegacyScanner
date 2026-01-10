module web.legacy.scan.cli {
    requires web.legacy.scan.core;
    requires web.legacy.scan.rules;
    requires web.legacy.scan.report;
    requires web.legacy.scan.parser.html;
    requires web.legacy.scan.parser.css;
    requires web.legacy.scan.parser.js;
    requires info.picocli;
    
    opens io.github.sainm.weblegacyscan.cli to info.picocli;
    
    exports io.github.sainm.weblegacyscan.cli;
}
