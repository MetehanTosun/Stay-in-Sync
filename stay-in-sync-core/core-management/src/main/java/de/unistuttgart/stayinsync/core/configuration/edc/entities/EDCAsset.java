package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "edc_asset")
public class EDCAsset extends UuidEntity {

    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String assetId;

    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String url;

    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String type;

    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String contentType;

    @Getter
    @Setter
    @Column(length = 1024)
    private String description;

    //TODO argumentieren warum hier auf EDCInstanz gemappt wird.
    @Getter
    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_edc_id", columnDefinition = "CHAR(36)", nullable = false)
    private EDCInstance targetEDC;

    @Getter
    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "data_address_id", columnDefinition = "CHAR(36)", nullable = false)
    private EDCDataAddress dataAddress;

    // Relation zu Properties (UUID-PK, optional) – mit Cascade
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "properties_id", columnDefinition = "CHAR(36)")
    private EDCProperty properties;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_system_endpoint_id", nullable = true)
    private TargetSystemEndpoint targetSystemEndpoint;
}
