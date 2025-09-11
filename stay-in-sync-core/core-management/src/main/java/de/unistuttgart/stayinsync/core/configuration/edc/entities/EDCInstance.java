package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Entity-Klasse für EDC-Instanzen (Eclipse Dataspace Connector).
 * Repräsentiert eine Instanz eines EDC-Connectors, mit dem die Anwendung kommunizieren kann.
 */
@Entity
@Table(name = "edc_instance")
public class EDCInstance extends UuidEntity {

    /**
     * Der Name der EDC-Instanz.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * Die URL, unter der die EDC-Instanz erreichbar ist.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String url;

    /**
     * Die Protokollversion, die von dieser EDC-Instanz verwendet wird.
     */
    @Getter
    @Setter
    @Column
    private String protocolVersion;

    /**
     * Eine optionale Beschreibung der EDC-Instanz.
     */
    @Getter
    @Setter
    @Column
    private String description;

    /**
     * Die Business Partner Number (BPN) der EDC-Instanz.
     */
    @Getter
    @Setter
    @Column
    private String bpn;

    /**
     * Der API-Schlüssel für die Authentifizierung bei der EDC-Instanz.
     */
    @Getter
    @Setter
    @Column
    private String apiKey;

    /**
     * Die Assets, die dieser EDC-Instanz zugeordnet sind.
     * Beim Löschen der Instanz werden alle zugehörigen Assets automatisch mit gelöscht.
     */
    @Getter
    @Setter
    @OneToMany(mappedBy = "targetEDC", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EDCAsset> edcAssets;
}
