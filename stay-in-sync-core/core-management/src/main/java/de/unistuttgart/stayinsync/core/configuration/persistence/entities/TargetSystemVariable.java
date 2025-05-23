package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

//TODO Implementierung
@Entity
public class TargetSystemVariable extends PanacheEntity {

    public String name;

}
