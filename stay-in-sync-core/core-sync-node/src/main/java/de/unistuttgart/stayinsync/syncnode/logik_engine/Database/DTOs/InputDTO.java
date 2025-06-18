package de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object (DTO) for an {@link de.unistuttgart.stayinsync.syncnode.logik_engine.InputNode}.
 * This class uses a polymorphic structure to represent different types of inputs.
 * The 'type' field acts as a discriminator.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputDTO {

    /**
     * The discriminator field, indicating the type of input.
     * Expected values: "CONSTANT", "JSON", "PARENT".
     */
    public String type;

    // --- Fields for CONSTANT type ---
    public String elementName;
    public Object value;

    // --- Fields for JSON type ---
    public String sourceName;
    public String path;

    // --- Fields for PARENT type ---
    public String parentNodeName;
}