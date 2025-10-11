package de.unistuttgart.graphengine.nodes;

import de.unistuttgart.graphengine.exception.NodeConfigurationException;

import java.util.Map;

/**
 * A node that holds a JSON schema definition.
 * This node is specifically designed to store and provide JSON schema strings.
 */
public class SchemaNode extends Node {

    /**
     * The JSON schema definition stored as a string.
     */
    private String schemaDefinition;

    /**
     * Constructs a new SchemaNode with a schema definition.
     *
     * @param schemaDefinition The JSON schema definition as a string. Cannot be null or empty.
     * @throws NodeConfigurationException if schemaDefinition is null or empty.
     */
    public SchemaNode(String schemaDefinition) throws NodeConfigurationException {
        if (schemaDefinition == null || schemaDefinition.trim().isEmpty()) {
            throw new NodeConfigurationException("Schema definition for SchemaNode cannot be null or empty.");
        }
        this.schemaDefinition = schemaDefinition;
    }

    /**
     * Returns the schema definition stored in this node.
     *
     * @return The JSON schema definition as a string.
     */
    public String getSchemaDefinition() {
        return schemaDefinition;
    }

    /**
     * Sets the schema definition for this node.
     *
     * @param schemaDefinition The JSON schema definition as a string.
     */
    public void setSchemaDefinition(String schemaDefinition) {
        this.schemaDefinition = schemaDefinition;
    }

    @Override
    public void calculate(Map<String, Object> dataContext) {
        // Schema nodes simply return their schema definition
        this.setCalculatedResult(this.schemaDefinition);
    }

    @Override
    public Class<?> getOutputType() {
        return String.class;
    }
}