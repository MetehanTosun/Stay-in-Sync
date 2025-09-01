package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EDCPropertyDto {
    @JsonIgnore
    private UUID id;

    @JsonIgnore
    private String description;
    
    private Map<String, String> additionalProperties = new HashMap<>();

    public UUID getId() {
        return id;
    }

    public EDCPropertyDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EDCPropertyDto setDescription(String description) {
        this.description = description;
        // Auch als zusätzliche Eigenschaft speichern
        if (description != null) {
            additionalProperties.put("asset:prop:description", description);
        }
        return this;
    }
    
    @JsonAnyGetter
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
    
    @JsonAnySetter
    public void setAdditionalProperty(String name, String value) {
        additionalProperties.put(name, value);
        // Wenn es sich um die Beschreibung handelt, setzen wir auch das description-Feld
        if ("asset:prop:description".equals(name)) {
            this.description = value;
        }
    }
    
    public void setAdditionalProperties(Map<String, String> properties) {
        this.additionalProperties = properties != null ? properties : new HashMap<>();
        // Beschreibung aus den Properties extrahieren, falls vorhanden
        if (properties != null && properties.containsKey("asset:prop:description")) {
            this.description = properties.get("asset:prop:description");
        }
    }
    
    @Override
    public String toString() {
        return "EDCPropertyDto{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
