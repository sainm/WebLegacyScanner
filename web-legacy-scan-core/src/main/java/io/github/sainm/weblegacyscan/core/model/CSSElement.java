package io.github.sainm.weblegacyscan.core.model;

import java.util.List;
import java.util.Objects;

/**
 * CSS 元素，包含属性、值及选择器的位置
 */
public record CSSElement(
    CSSElementType type,
    String selector,
    Location selectorLocation,
    String property,
    Location propertyLocation,
    String value,
    Location valueLocation,
    Location location,
    String rawContent,
    List<CSSElement> children
) implements CodeElement {

    public CSSElement {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
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
        return ElementType.CSS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CSSElementType type;
        private String selector;
        private Location selectorLocation;
        private String property;
        private Location propertyLocation;
        private String value;
        private Location valueLocation;
        private Location location;
        private String rawContent;
        private List<CSSElement> children = List.of();

        public Builder type(CSSElementType type) {
            this.type = type;
            return this;
        }

        public Builder selector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder selectorLocation(Location selectorLocation) {
            this.selectorLocation = selectorLocation;
            return this;
        }

        public Builder property(String property) {
            this.property = property;
            return this;
        }

        public Builder propertyLocation(Location propertyLocation) {
            this.propertyLocation = propertyLocation;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder valueLocation(Location valueLocation) {
            this.valueLocation = valueLocation;
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

        public Builder children(List<CSSElement> children) {
            this.children = children;
            return this;
        }

        public CSSElement build() {
            return new CSSElement(type, selector, selectorLocation, property, propertyLocation,
                                 value, valueLocation, location, rawContent, children);
        }
    }
}
