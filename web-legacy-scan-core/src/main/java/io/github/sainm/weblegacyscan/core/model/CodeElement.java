package io.github.sainm.weblegacyscan.core.model;

/**
 * Sealed interface for code elements, all code elements contain complete location information
 */
public sealed interface CodeElement 
    permits HTMLElement, CSSElement, JSElement {
    
    Location getLocation();
    String getRawContent();
    ElementType getElementType();
}
