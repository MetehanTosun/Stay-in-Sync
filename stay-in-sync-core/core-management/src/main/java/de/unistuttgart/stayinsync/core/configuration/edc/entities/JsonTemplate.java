package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;
import java.util.UUID;

/**
 * Entity-Klasse für Policy-Templates.
 * Repräsentiert ein Vorlage-Template für Policies, die im Frontend verwendet werden können.
 * Diese Entität speichert sowohl Metadaten als auch den JSON-Content des Templates.
 */
@Entity
@Table(name = "json_template")
public class JsonTemplate extends UuidEntity {

    /**
     * Der Name des Templates für die Anzeige im Frontend.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * Eine optionale Beschreibung des Templates.
     */
    @Getter
    @Setter
    @Column(length = 1024)
    private String description;

    /**
     * Der JSON-Content des Templates als Map.
     * Wird als JSON in der Datenbank gespeichert und automatisch serialisiert/deserialisiert.
     * Verwendet JdbcTypeCode für die Speicherung als JSON-Spalte.
     */
    @Getter
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private Map<String, Object> content;
}
