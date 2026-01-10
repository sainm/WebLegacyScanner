package io.github.sainm.weblegacyscan.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTML 元素，包含标签、属性及其各自的位置
 */
public record HTMLElement(
    String tagName,
    Location tagNameLocation,
    Map<String, AttributeInfo> attributes,
    Location location,
    String rawContent,
    List<HTMLElement> children,
    HTMLElementType elementType
) implements CodeElement {

    public HTMLElement {
        Objects.requireNonNull(tagName, "tagName cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(elementType, "elementType cannot be null");
        attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
        children = children != null ? List.copyOf(children) : List.of();
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getRawContent() {
        return rawContent;
    }

    @Override
    public ElementType getElementType() {
        return ElementType.HTML;
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name.toLowerCase());
    }

    public AttributeInfo getAttribute(String name) {
        return attributes.get(name.toLowerCase());
    }

    public String getAttributeValue(String name) {
        AttributeInfo attr = attributes.get(name.toLowerCase());
        return attr != null ? attr.value() : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tagName;
        private Location tagNameLocation;
        private Map<String, AttributeInfo> attributes = Map.of();
        private Location location;
        private String rawContent;
        private List<HTMLElement> children = List.of();
        private HTMLElementType elementType = HTMLElementType.OPEN_TAG;

        public Builder tagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        public Builder tagNameLocation(Location tagNameLocation) {
            this.tagNameLocation = tagNameLocation;
            return this;
        }

        public Builder attributes(Map<String, AttributeInfo> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder rawContent(String rawContent) {
            this.rawContent = rawContent;
            return this;
        }

        public Builder children(List<HTMLElement> children) {
            this.children = children;
            return this;
        }

        public Builder elementType(HTMLElementType elementType) {
            this.elementType = elementType;
            return this;
        }

        public HTMLElement build() {
            return new HTMLElement(tagName, tagNameLocation, attributes, location, 
                                  rawContent, children, elementType);
        }
    }
}
