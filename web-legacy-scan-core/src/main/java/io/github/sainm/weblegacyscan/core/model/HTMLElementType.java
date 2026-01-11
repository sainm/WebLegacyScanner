package io.github.sainm.weblegacyscan.core.model;

/**
 * HTML/JSP element type
 */
public enum HTMLElementType {
    // Standard HTML types
    OPEN_TAG,
    CLOSE_TAG,
    SELF_CLOSING,
    DOCTYPE,
    COMMENT,
    TEXT,
    CDATA,
    
    // JSP specific types
    JSP_DIRECTIVE,      // <%@ page/include/taglib %>
    JSP_EXPRESSION,     // <%= expression %>
    JSP_SCRIPTLET,      // <% code %>
    JSP_DECLARATION,    // <%! declaration %>
    JSP_COMMENT,        // <%-- comment --%>
    JSP_ACTION,         // <jsp:xxx>
    EL_EXPRESSION,      // ${expression}
    
    // JSTL tags
    JSTL_CORE,          // <c:xxx> - core tags
    JSTL_FMT,           // <fmt:xxx> - formatting tags
    JSTL_SQL,           // <sql:xxx> - SQL tags
    JSTL_XML,           // <x:xxx> - XML tags
    JSTL_FN,            // <fn:xxx> - function tags
    
    // Struts1 tags
    STRUTS_HTML,        // <html:xxx> - HTML form tags
    STRUTS_BEAN,        // <bean:xxx> - bean tags
    STRUTS_LOGIC,       // <logic:xxx> - logic tags
    STRUTS_NESTED,      // <nested:xxx> - nested tags
    STRUTS_TILES,       // <tiles:xxx> - tiles tags
    
    // Struts2 tags
    STRUTS2_TAG,        // <s:xxx> - Struts2 tags
    
    // Spring tags
    SPRING_FORM,        // <form:xxx> - Spring form tags
    SPRING_TAG,         // <spring:xxx> - Spring tags
    
    // Other common tag libraries
    DISPLAY_TAG,        // <display:xxx> - display tag
    
    // Generic custom tag (any other namespace)
    CUSTOM_TAG          // <prefix:xxx> - other custom tags
}
