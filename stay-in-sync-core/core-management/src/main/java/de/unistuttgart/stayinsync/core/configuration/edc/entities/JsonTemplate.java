package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

/**
 * Entity-Klasse für Policy-Templates.
 * Repräsentiert ein Vorlage-Template für Policies, die im Frontend verwendet werden können.
 * Diese Entität speichert sowohl Metadaten als auch den JSON-Content des Templates.
 */
@Entity
@Table(name = "json_template")
@NoArgsConstructor
public class JsonTemplate extends PanacheEntity {

    /**
     * Der Name des Templates für die Anzeige im Frontend.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    public String name;

    /**
     * Eine optionale Beschreibung des Templates.
     */
    @Getter
    @Setter
    @Column(length = 1024)
    public String description;

    /**
     * Der JSON-Content des Templates als Map.
     * Wird als JSON in der Datenbank gespeichert und automatisch serialisiert/deserialisiert.
     * Verwendet JdbcTypeCode für die Speicherung als JSON-Spalte.
     */
    @Getter
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    public Map<String, Object> content;
}
