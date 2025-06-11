package de.unistuttgart.stayinsync.core.configuration.persistence.entities.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class EDCProperty extends PanacheEntity {

    public String description;


}
