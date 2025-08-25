package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "edc_instance")
public class EDCInstance extends UuidEntity {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String url;

    @Column
    public String protocolVersion;

    @Column
    public String description;

    @Column
    public String bpn;

    @Column
    public String apiKey;

    @OneToMany(mappedBy = "targetEDC", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<EDCAsset> edcAssets;
}
