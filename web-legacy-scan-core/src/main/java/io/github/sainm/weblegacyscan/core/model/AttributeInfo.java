package io.github.sainm.weblegacyscan.core.model;

import java.util.Objects;

/**
 * HTML 属性信息，包含名称和值的独立位置
 */
public record AttributeInfo(
    String name,
    Location nameLocation,
    String value,
    Location valueLocation,
    Location fullLocation
) {
    public AttributeInfo {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(nameLocation, "nameLocation cannot be null");
        Objects.requireNonNull(fullLocation, "fullLocation cannot be null");
    }

    public static AttributeInfo of(String name, Location nameLocation, String value, 
                                   Location valueLocation, Location fullLocation) {
        return new AttributeInfo(name, nameLocation, value, valueLocation, fullLocation);
    }

    public static AttributeInfo booleanAttribute(String name, Location location) {
        return new AttributeInfo(name, location, null, null, location);
    }

    public boolean hasValue() {
        return value != null;
    }
}
