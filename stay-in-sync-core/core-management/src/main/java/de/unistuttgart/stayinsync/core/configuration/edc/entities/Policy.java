package de.unistuttgart.stayinsync.core.configuration.edc.entities;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "edc_policy")
public class Policy extends PanacheEntity {

    private String contents;

    @OneToOne
    private PolicyDefinition policyDefinition;
}
