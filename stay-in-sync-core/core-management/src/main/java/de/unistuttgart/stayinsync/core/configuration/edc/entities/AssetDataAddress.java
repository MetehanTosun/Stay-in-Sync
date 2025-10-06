package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity-Klasse für EDC-DataAddress.
 * Repräsentiert die Daten-Adresse eines Assets im EDC-System.
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "edc_data_address")
public class AssetDataAddress extends PanacheEntity {

    @Column(name = "type")
    private String type;

    @Column(name = "baseUrl")
    private String baseUrl;

    @Column(name = "path")
    private String path;

    private String queryParams;

    private  String headers;

    @Column(name = "proxyPath")
    private Boolean proxyPath;

    @Column(name = "proxyQueryParams")
    private Boolean proxyQueryParams;

    @Column(name = "proxyMethod")
    private Boolean proxyMethod;

    @Column(name = "proxyBody")
    private Boolean proxyBody;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "asset_id")
    private Asset asset;


}
