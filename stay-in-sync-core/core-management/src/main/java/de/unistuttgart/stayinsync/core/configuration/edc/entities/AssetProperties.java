package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity-Klasse für EDC-Properties.
 * Repräsentiert die Eigenschaften eines Assets im EDC-System.
 */
@Getter
@Setter
@Entity
@Table(name = "edc_property")
public class AssetProperties extends PanacheEntity {

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "asset_id")
    private Asset asset;

}
