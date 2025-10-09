package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.Map;

@Entity
@Table(name = "edc_policy")
public class Policy extends PanacheEntity {

    private String contents;

    @OneToOne
    private PolicyDefinition policyDefinition;
}
