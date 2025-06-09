package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class TargetSystemApiQueryParam extends PanacheEntity {

    @ManyToOne
    public TargetSystemEndpoint targetSystemEndpoint;

}
