/*<<<<<<< HEAD:stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/monitoring/core/configuration/persistence/entities/SourceSystem.java
package de.unistuttgart.stayinsync.monitoring.core.configuration.persistence.entities;
=======
package de.unistuttgart.stayinsync.core.configuration.persistence.entities.edc;
>>>>>>> main:stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/persistence/entities/edc/EDC.java

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class EDC extends PanacheEntity {

    public String name;

    public String url;

    public String apiKey;

    //@OneToMany(mappedBy = "targetEDC")
    //public Set<EDCAsset> edcAssets;
}
*/