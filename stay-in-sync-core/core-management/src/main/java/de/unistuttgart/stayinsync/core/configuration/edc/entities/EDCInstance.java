package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor // für JPA & MapStruct
@Entity
@Table(name = "edc_instance")
public class EDCInstance extends UuidEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "control_plane_management_url")
    private String controlPlaneManagementUrl;

    private String protocolVersion;

    private String description;

    private String bpn;

    private String apiKey;

    @OneToMany(mappedBy = "targetEdc", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PolicyDefinition> policies = new ArrayList<>();

    @OneToMany(mappedBy = "targetEdc", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asset> assets = new ArrayList<>();

    @OneToMany(mappedBy = "targetEdc", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractDefinition> contractDefinitions = new ArrayList<>();



}
