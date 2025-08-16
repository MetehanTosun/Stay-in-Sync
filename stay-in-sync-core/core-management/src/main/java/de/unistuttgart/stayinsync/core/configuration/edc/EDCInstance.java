package de.unistuttgart.stayinsync.core.configuration.edc;

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

    @Column(nullable = false)
    public String apiKey;

    @Column(name="protocol_version", length=20) public String protocolVersion; // NEU

    @Column(length=1024) public String description;                             // NEU
    
    @Column(length=36) public String bpn; 

    @OneToMany(mappedBy = "targetEDC", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<EDCAsset> edcAssets;
}
