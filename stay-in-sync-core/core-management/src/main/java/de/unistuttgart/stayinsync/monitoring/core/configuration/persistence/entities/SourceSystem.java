package de.unistuttgart.stayinsync.monitoring.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

//TODO Implementierung
@Entity
public class SourceSystem extends PanacheEntity {

    public String name;

    public String address;

    public String apiKey;
}
