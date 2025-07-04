package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class TargetSystemApiRequestHeader extends ApiHeader {

    @ManyToOne
    public TargetSystemEndpoint targetSystemEndpoint;

}
