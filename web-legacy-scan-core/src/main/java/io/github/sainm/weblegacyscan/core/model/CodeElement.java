package io.github.sainm.weblegacyscan.core.model;

/**
 * 代码元素的密封接口，所有代码元素都包含完整的位置信�?
 */
public sealed interface CodeElement 
    permits HTMLElement, CSSElement, JSElement {
    
    Location getLocation();
    String getRawContent();
    ElementType getElementType();
}
