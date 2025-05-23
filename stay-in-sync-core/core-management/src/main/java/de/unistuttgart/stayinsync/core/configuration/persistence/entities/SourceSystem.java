package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class SourceSystem extends PanacheEntity {
    // Id hier nicht nötig wegen panache entity
    public String name;
    public String endpointUrl;
    public String description;
    public String aasIdShort;
    public String submodelPath; // falls relevant

    // könnte auch ganz anders aufgebaut sein, frage nach !!
}
