package org.example.parser;

public enum ParserTokenType {
    OBJECT_START,
    OBJECT_END,
    ARRAY_START,
    ARRAY_END,
    PROPERTY_NAME,
    PROPERTY_VALUE,
    NAME_VALUE_SEPARATOR,
    PROPERTY_SEPARATOR,
    NUMBER,
    NULL,
    BOOLEAN
}
