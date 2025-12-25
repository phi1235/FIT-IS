package com.example.keycloak.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Custom deserializer để trim string khi deserialize JSON
 */
public class TrimStringDeserializer extends JsonDeserializer<String> {
    
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) 
            throws IOException {
        String value = jsonParser.getValueAsString();
        return value != null ? value.trim() : null;
    }
}

