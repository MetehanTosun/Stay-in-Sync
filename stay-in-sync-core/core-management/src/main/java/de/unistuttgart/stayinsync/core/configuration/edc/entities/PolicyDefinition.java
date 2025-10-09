package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entitätsklasse für EDC-Policies in der Datenbank.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System
 * und speichert alle relevanten Informationen persistent in der Datenbank.
 * Eine Policy definiert Zugriffs- und Nutzungsbedingungen für Assets im EDC-System.
 * Die vollständige Policy-Definition wird als JSON-String gespeichert.
 */
@Setter
@Getter
@Entity
@Table(name = "edc_policydefinition")
public class PolicyDefinition extends PanacheEntity {

    @JsonProperty("@id")
    @Column(nullable = false, unique = true)
    private String policyId;

    @OneToOne
    private Policy policy;

    @Column
    private String displayName;

    @ManyToOne
    @JoinColumn(name = "edc_instance")
    private EDCInstance edcInstance;

}
