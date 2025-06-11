package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class SystemAuthDetails extends PanacheEntity {

    public String authType;


}
