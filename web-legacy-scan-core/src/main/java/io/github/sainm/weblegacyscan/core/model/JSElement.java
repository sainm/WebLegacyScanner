package io.github.sainm.weblegacyscan.core.model;

import java.util.List;
import java.util.Objects;

/**
 * JavaScript 元素，包含标识符、参数的位置
 */
public record JSElement(
    JSElementType type,
    String identifier,
    Location identifierLocation,
    List<JSArgument> arguments,
    Location location,
    String rawContent,
    List<JSElement> children
) implements CodeElement {

    public JSElement {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        arguments = arguments != null ? List.copyOf(arguments) : List.of();
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
        return ElementType.JAVASCRIPT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JSElementType type;
        private String identifier;
        private Location identifierLocation;
        private List<JSArgument> arguments = List.of();
        private Location location;
        private String rawContent;
        private List<JSElement> children = List.of();

        public Builder type(JSElementType type) {
            this.type = type;
            return this;
        }

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder identifierLocation(Location identifierLocation) {
            this.identifierLocation = identifierLocation;
            return this;
        }

        public Builder arguments(List<JSArgument> arguments) {
            this.arguments = arguments;
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

        public Builder children(List<JSElement> children) {
            this.children = children;
            return this;
        }

        public JSElement build() {
            return new JSElement(type, identifier, identifierLocation, arguments,
                                location, rawContent, children);
        }
    }
}
