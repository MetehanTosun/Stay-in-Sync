package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class ApiRequestHeader extends PanacheEntity {


    @ManyToOne
    public ApiRequestConfiguration apiRequestConfiguration;
    
    public String headerName;

    public String headerValue;

}
