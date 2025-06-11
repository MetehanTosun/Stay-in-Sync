package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclass that serves the purpose of describing auth details of systems that are involved in a sync process
 */
@MappedSuperclass
public class SystemAuthDetails extends PanacheEntity {

    public String authType;


}
