package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclass that serves the purpose of describing Target and Source system involved in a sync process
 */
@MappedSuperclass
public class SyncSystem extends PanacheEntity {

    public String name;
    public String apiUrl;
    public String description;
    public String sourceSystemType; // REST, AAS
    public String openAPI;


}
