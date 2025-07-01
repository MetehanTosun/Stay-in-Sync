package de.unistuttgart.stayinsync.syncnode.logik_engine.database.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;

/**
 * Data Transfer Object (DTO) for an {@link InputNode}.
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