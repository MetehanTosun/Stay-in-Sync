package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für Policy-Templates.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 * Folgt dem Record-Pattern für Immutabilität und automatisches Generieren von Konstruktoren,
 * equals, hashCode und toString.
 *
 * Die Template-Struktur entspricht dem Format, das vom Frontend erwartet wird.
 */
public record JsonTemplateDto(
        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        @NotBlank
        String name,

        @JsonProperty("description")
        String description,

        @JsonProperty("content")
        @NotNull
        Map<String, Object> content
) {
    /**
     * Gibt eine String-Repräsentation des DTOs zurück.
     * Überschreibt die automatisch generierte toString-Methode für bessere Lesbarkeit.
     */
    @Override
    public String toString() {
        return "TemplateDto{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", content=" + content +
                '}';
    }
}
