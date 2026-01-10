module web.legacy.scan.core {
    requires transitive com.google.gson;
    
    exports io.github.sainm.weblegacyscan.core.model;
    exports io.github.sainm.weblegacyscan.core.file;
    exports io.github.sainm.weblegacyscan.core.parser;
    exports io.github.sainm.weblegacyscan.core.executor;
    exports io.github.sainm.weblegacyscan.core.scanner;
    
    uses io.github.sainm.weblegacyscan.core.parser.Parser;
}
