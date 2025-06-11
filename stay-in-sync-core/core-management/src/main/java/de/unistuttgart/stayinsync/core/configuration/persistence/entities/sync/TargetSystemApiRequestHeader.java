package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class TargetSystemApiRequestHeader extends ApiRequestHeader {

    @ManyToOne
    public TargetSystemEndpoint targetSystemEndpoint;

}
