package de.unistuttgart.graphengine.service;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SchemaCache {
    private final Map<String, JsonSchema> cache = new ConcurrentHashMap<>();
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    public JsonSchema getCompiledSchema(String schemaString) {
        return cache.computeIfAbsent(schemaString, this::compileSchema);
    }

    private JsonSchema compileSchema(String schemaString) {
        try {
            return factory.getSchema(schemaString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile schema: " + schemaString, e);
        }
    }

    // Optional: Cache-Management
    public void clearCache() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }
}
