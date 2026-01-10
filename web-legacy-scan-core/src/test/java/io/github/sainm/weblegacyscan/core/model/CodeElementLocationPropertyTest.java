package io.github.sainm.weblegacyscan.core.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Property-based tests for CodeElement location tracking.
 * 
 * Property 22: HTML Attribute Location Tracking
 * Property 23: CSS Declaration Location Tracking
 * Property 24: JS Expression Location Tracking
 * 
 * Validates: Requirements 13.2, 13.3, 13.4
 */
class CodeElementLocationPropertyTest {

    private static final Path TEST_FILE = Path.of("test.html");

    // ========== Property 22: HTML Attribute Location Tracking ==========

    /**
     * Property 22: HTML Attribute Location Tracking
     * 
     * For any HTMLElement with attributes, each AttributeInfo SHALL have valid nameLocation,
     * and if the attribute has a value, a valid valueLocation.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void htmlAttributesMustHaveValidNameLocation(
            @ForAll("validAttributeInfo") AttributeInfo attr
    ) {
        // nameLocation must be valid
        assert attr.nameLocation() != null : "nameLocation must not be null";
        assert attr.nameLocation().startLine() >= 1 : "nameLocation startLine must be >= 1";
        assert attr.nameLocation().startColumn() >= 1 : "nameLocation startColumn must be >= 1";
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Value Location
     * 
     * For any HTMLElement attribute with a value, the valueLocation SHALL be valid.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void htmlAttributesWithValueMustHaveValidValueLocation(
            @ForAll("attributeInfoWithValue") AttributeInfo attr
    ) {
        // If attribute has a value, valueLocation must be valid
        assert attr.hasValue() : "Attribute should have a value";
        assert attr.valueLocation() != null : "valueLocation must not be null when attribute has value";
        assert attr.valueLocation().startLine() >= 1 : "valueLocation startLine must be >= 1";
        assert attr.valueLocation().startColumn() >= 1 : "valueLocation startColumn must be >= 1";
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Boolean Attributes
     * 
     * For any boolean attribute (no value), valueLocation may be null.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void booleanAttributesMayHaveNullValueLocation(
            @ForAll("booleanAttributeInfo") AttributeInfo attr
    ) {
        // Boolean attributes have no value
        assert !attr.hasValue() : "Boolean attribute should not have a value";
        assert attr.valueLocation() == null : "Boolean attribute valueLocation should be null";
        // But nameLocation and fullLocation must still be valid
        assert attr.nameLocation() != null : "nameLocation must not be null";
        assert attr.fullLocation() != null : "fullLocation must not be null";
    }

    /**
     * Property 22: HTML Attribute Location Tracking - Full Location
     * 
     * For any HTMLElement attribute, fullLocation SHALL be valid and encompass the entire attribute.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void htmlAttributesMustHaveValidFullLocation(
            @ForAll("validAttributeInfo") AttributeInfo attr
    ) {
        assert attr.fullLocation() != null : "fullLocation must not be null";
        assert attr.fullLocation().startLine() >= 1 : "fullLocation startLine must be >= 1";
        assert attr.fullLocation().startColumn() >= 1 : "fullLocation startColumn must be >= 1";
    }

    /**
     * Property 22: HTML Attribute Location Tracking - HTMLElement with Attributes
     * 
     * For any HTMLElement with attributes, each attribute SHALL have valid location tracking.
     * 
     * Feature: web-legacy-scan, Property 22: HTML Attribute Location Tracking
     * Validates: Requirements 13.2
     */
    @Property(tries = 100)
    void htmlElementAttributesHaveValidLocations(
            @ForAll("htmlElementWithAttributes") HTMLElement element
    ) {
        for (Map.Entry<String, AttributeInfo> entry : element.attributes().entrySet()) {
            AttributeInfo attr = entry.getValue();
            
            // nameLocation must always be valid
            assert attr.nameLocation() != null : "Attribute nameLocation must not be null";
            assert attr.nameLocation().startLine() >= 1 : "Attribute nameLocation startLine must be >= 1";
            
            // fullLocation must always be valid
            assert attr.fullLocation() != null : "Attribute fullLocation must not be null";
            assert attr.fullLocation().startLine() >= 1 : "Attribute fullLocation startLine must be >= 1";
            
            // If has value, valueLocation must be valid
            if (attr.hasValue()) {
                assert attr.valueLocation() != null : "Attribute valueLocation must not be null when has value";
                assert attr.valueLocation().startLine() >= 1 : "Attribute valueLocation startLine must be >= 1";
            }
        }
    }

    // ========== Property 23: CSS Declaration Location Tracking ==========

    /**
     * Property 23: CSS Declaration Location Tracking
     * 
     * For any CSSElement of type DECLARATION, the element SHALL have valid propertyLocation
     * and valueLocation in addition to the full declaration location.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void cssDeclarationMustHaveValidPropertyLocation(
            @ForAll("cssDeclarationElement") CSSElement element
    ) {
        assert element.type() == CSSElementType.DECLARATION : "Element must be a DECLARATION";
        
        // propertyLocation must be valid
        assert element.propertyLocation() != null : "propertyLocation must not be null for DECLARATION";
        assert element.propertyLocation().startLine() >= 1 : "propertyLocation startLine must be >= 1";
        assert element.propertyLocation().startColumn() >= 1 : "propertyLocation startColumn must be >= 1";
    }

    /**
     * Property 23: CSS Declaration Location Tracking - Value Location
     * 
     * For any CSSElement of type DECLARATION, valueLocation SHALL be valid.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void cssDeclarationMustHaveValidValueLocation(
            @ForAll("cssDeclarationElement") CSSElement element
    ) {
        assert element.type() == CSSElementType.DECLARATION : "Element must be a DECLARATION";
        
        // valueLocation must be valid
        assert element.valueLocation() != null : "valueLocation must not be null for DECLARATION";
        assert element.valueLocation().startLine() >= 1 : "valueLocation startLine must be >= 1";
        assert element.valueLocation().startColumn() >= 1 : "valueLocation startColumn must be >= 1";
    }

    /**
     * Property 23: CSS Declaration Location Tracking - Full Location
     * 
     * For any CSSElement of type DECLARATION, the full location SHALL be valid.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void cssDeclarationMustHaveValidFullLocation(
            @ForAll("cssDeclarationElement") CSSElement element
    ) {
        assert element.type() == CSSElementType.DECLARATION : "Element must be a DECLARATION";
        
        // Full location must be valid
        assert element.location() != null : "location must not be null";
        assert element.location().startLine() >= 1 : "location startLine must be >= 1";
        assert element.location().startColumn() >= 1 : "location startColumn must be >= 1";
    }

    /**
     * Property 23: CSS Declaration Location Tracking - Property and Value Content
     * 
     * For any CSSElement of type DECLARATION, property and value strings SHALL be non-null.
     * 
     * Feature: web-legacy-scan, Property 23: CSS Declaration Location Tracking
     * Validates: Requirements 13.3
     */
    @Property(tries = 100)
    void cssDeclarationMustHavePropertyAndValue(
            @ForAll("cssDeclarationElement") CSSElement element
    ) {
        assert element.type() == CSSElementType.DECLARATION : "Element must be a DECLARATION";
        assert element.property() != null : "property must not be null for DECLARATION";
        assert element.value() != null : "value must not be null for DECLARATION";
    }

    // ========== Property 24: JS Expression Location Tracking ==========

    /**
     * Property 24: JS Expression Location Tracking
     * 
     * For any JSElement representing a function or method call, the element SHALL have
     * valid identifierLocation.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void jsFunctionCallMustHaveValidIdentifierLocation(
            @ForAll("jsFunctionCallElement") JSElement element
    ) {
        assert element.type() == JSElementType.FUNCTION_CALL || element.type() == JSElementType.METHOD_CALL 
            : "Element must be a FUNCTION_CALL or METHOD_CALL";
        
        // identifierLocation must be valid
        assert element.identifierLocation() != null : "identifierLocation must not be null for function/method call";
        assert element.identifierLocation().startLine() >= 1 : "identifierLocation startLine must be >= 1";
        assert element.identifierLocation().startColumn() >= 1 : "identifierLocation startColumn must be >= 1";
    }

    /**
     * Property 24: JS Expression Location Tracking - Arguments
     * 
     * For any JSElement representing a function or method call, each JSArgument SHALL have
     * a valid location.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void jsArgumentsMustHaveValidLocations(
            @ForAll("jsFunctionCallWithArguments") JSElement element
    ) {
        assert element.type() == JSElementType.FUNCTION_CALL || element.type() == JSElementType.METHOD_CALL 
            : "Element must be a FUNCTION_CALL or METHOD_CALL";
        
        for (JSArgument arg : element.arguments()) {
            assert arg.location() != null : "JSArgument location must not be null";
            assert arg.location().startLine() >= 1 : "JSArgument location startLine must be >= 1";
            assert arg.location().startColumn() >= 1 : "JSArgument location startColumn must be >= 1";
        }
    }

    /**
     * Property 24: JS Expression Location Tracking - Full Location
     * 
     * For any JSElement representing a function or method call, the full expression location
     * SHALL be valid.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void jsFunctionCallMustHaveValidFullLocation(
            @ForAll("jsFunctionCallElement") JSElement element
    ) {
        assert element.type() == JSElementType.FUNCTION_CALL || element.type() == JSElementType.METHOD_CALL 
            : "Element must be a FUNCTION_CALL or METHOD_CALL";
        
        // Full location must be valid
        assert element.location() != null : "location must not be null";
        assert element.location().startLine() >= 1 : "location startLine must be >= 1";
        assert element.location().startColumn() >= 1 : "location startColumn must be >= 1";
    }

    /**
     * Property 24: JS Expression Location Tracking - Identifier Content
     * 
     * For any JSElement representing a function or method call, identifier SHALL be non-null.
     * 
     * Feature: web-legacy-scan, Property 24: JS Expression Location Tracking
     * Validates: Requirements 13.4
     */
    @Property(tries = 100)
    void jsFunctionCallMustHaveIdentifier(
            @ForAll("jsFunctionCallElement") JSElement element
    ) {
        assert element.type() == JSElementType.FUNCTION_CALL || element.type() == JSElementType.METHOD_CALL 
            : "Element must be a FUNCTION_CALL or METHOD_CALL";
        assert element.identifier() != null : "identifier must not be null for function/method call";
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Location> validLocation() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 1000),
            Arbitraries.integers().between(1, 200),
            Arbitraries.integers().between(0, 50),
            Arbitraries.integers().between(0, 100)
        ).as((startLine, startColumn, lineOffset, colOffset) -> {
            int endLine = startLine + lineOffset;
            int endColumn = (lineOffset == 0) ? startColumn + colOffset : 1 + colOffset;
            return Location.of(TEST_FILE, startLine, startColumn, endLine, endColumn);
        });
    }

    @Provide
    Arbitrary<AttributeInfo> validAttributeInfo() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            validLocation(),
            Arbitraries.strings().ofMinLength(0).ofMaxLength(50),
            validLocation(),
            validLocation()
        ).as((name, nameLoc, value, valueLoc, fullLoc) -> 
            AttributeInfo.of(name, nameLoc, value.isEmpty() ? null : value, 
                           value.isEmpty() ? null : valueLoc, fullLoc));
    }

    @Provide
    Arbitrary<AttributeInfo> attributeInfoWithValue() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            validLocation(),
            Arbitraries.strings().ofMinLength(1).ofMaxLength(50),
            validLocation(),
            validLocation()
        ).as((name, nameLoc, value, valueLoc, fullLoc) -> 
            AttributeInfo.of(name, nameLoc, value, valueLoc, fullLoc));
    }

    @Provide
    Arbitrary<AttributeInfo> booleanAttributeInfo() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            validLocation()
        ).as((name, loc) -> AttributeInfo.booleanAttribute(name, loc));
    }

    @Provide
    Arbitrary<HTMLElement> htmlElementWithAttributes() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
            validLocation(),
            Arbitraries.maps(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                validAttributeInfo()
            ).ofMinSize(1).ofMaxSize(5),
            validLocation()
        ).as((tagName, tagNameLoc, attrs, loc) -> 
            HTMLElement.builder()
                .tagName(tagName)
                .tagNameLocation(tagNameLoc)
                .attributes(attrs)
                .location(loc)
                .elementType(HTMLElementType.OPEN_TAG)
                .build());
    }

    @Provide
    Arbitrary<CSSElement> cssDeclarationElement() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            validLocation(),
            Arbitraries.strings().ofMinLength(1).ofMaxLength(50),
            validLocation(),
            validLocation()
        ).as((property, propLoc, value, valueLoc, fullLoc) ->
            CSSElement.builder()
                .type(CSSElementType.DECLARATION)
                .property(property)
                .propertyLocation(propLoc)
                .value(value)
                .valueLocation(valueLoc)
                .location(fullLoc)
                .build());
    }

    @Provide
    Arbitrary<JSElement> jsFunctionCallElement() {
        return Combinators.combine(
            Arbitraries.of(JSElementType.FUNCTION_CALL, JSElementType.METHOD_CALL),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            validLocation(),
            validLocation()
        ).as((type, identifier, identLoc, fullLoc) ->
            JSElement.builder()
                .type(type)
                .identifier(identifier)
                .identifierLocation(identLoc)
                .location(fullLoc)
                .build());
    }

    @Provide
    Arbitrary<JSArgument> validJsArgument() {
        return Combinators.combine(
            Arbitraries.strings().ofMinLength(1).ofMaxLength(30),
            validLocation(),
            Arbitraries.of(JSArgumentType.values())
        ).as((value, loc, type) -> JSArgument.of(value, loc, type));
    }

    @Provide
    Arbitrary<JSElement> jsFunctionCallWithArguments() {
        return Combinators.combine(
            Arbitraries.of(JSElementType.FUNCTION_CALL, JSElementType.METHOD_CALL),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            validLocation(),
            validJsArgument().list().ofMinSize(1).ofMaxSize(5),
            validLocation()
        ).as((type, identifier, identLoc, args, fullLoc) ->
            JSElement.builder()
                .type(type)
                .identifier(identifier)
                .identifierLocation(identLoc)
                .arguments(args)
                .location(fullLoc)
                .build());
    }
}
