package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "edc_policy")
public class EDCPolicy extends UuidEntity {

    @Column(nullable = false, unique = true)
    public String policyId;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    public String policyJson;
    
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    public EDCInstance edcInstance;

}
