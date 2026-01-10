module web.legacy.scan.parser.html {
    requires transitive web.legacy.scan.core;
    requires jericho.html;
    
    exports io.github.sainm.weblegacyscan.parser.html;
    
    provides io.github.sainm.weblegacyscan.core.parser.Parser 
        with io.github.sainm.weblegacyscan.parser.html.JerichoHTMLParser;
}
