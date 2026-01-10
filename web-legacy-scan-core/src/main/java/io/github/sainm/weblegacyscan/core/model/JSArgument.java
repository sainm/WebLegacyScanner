package io.github.sainm.weblegacyscan.core.model;

import java.util.Objects;

/**
 * JavaScript 参数信息
 */
public record JSArgument(
    String value,
    Location location,
    JSArgumentType type
) {
    public JSArgument {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
    }

    public static JSArgument of(String value, Location location, JSArgumentType type) {
        return new JSArgument(value, location, type);
    }

    public static JSArgument string(String value, Location location) {
        return new JSArgument(value, location, JSArgumentType.STRING);
    }

    public static JSArgument number(String value, Location location) {
        return new JSArgument(value, location, JSArgumentType.NUMBER);
    }

    public static JSArgument identifier(String value, Location location) {
        return new JSArgument(value, location, JSArgumentType.IDENTIFIER);
    }
}
