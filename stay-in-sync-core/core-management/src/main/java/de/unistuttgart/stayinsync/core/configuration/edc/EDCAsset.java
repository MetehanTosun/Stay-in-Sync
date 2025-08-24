package de.unistuttgart.stayinsync.core.configuration.edc;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

@Entity
@Table(name = "edc_asset")
public class EDCAsset extends UuidEntity {

    @NotBlank
    @Column(nullable = false)
    public String assetId;

    @NotBlank
    @Column(nullable = false)
    public String url;

    @NotBlank
    @Column(nullable = false)
    public String type;

    @NotBlank
    @Column(nullable = false)
    public String contentType;

    @Column(length = 1024)
    public String description;

    // Relation zur DataAddress (UUID-PK) – mit Cascade
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "data_address_id", columnDefinition = "CHAR(36)", nullable = false)
    public EDCDataAddress dataAddress;

    // Relation zu Properties (UUID-PK, optional) – mit Cascade
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "properties_id", columnDefinition = "CHAR(36)")
    public EDCProperty properties;

    // Relation zur EDC-Instanz (UUID-PK)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_edc_id", columnDefinition = "CHAR(36)", nullable = false)
    public EDCInstance targetEDC;

    // Optional: Access Policies
    @OneToMany(mappedBy = "edcAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<EDCAccessPolicy> edcAccessPolicies;

    // Optional: Contract Definitions
    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<EDCContractDefinition> contractDefinitions;

    // Relation zum TargetSystemEndpoint (Long-PK) — hier NICHT mehr CHAR(36)!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_system_endpoint_id", nullable = true)
    public TargetSystemEndpoint targetSystemEndpoint;
}
