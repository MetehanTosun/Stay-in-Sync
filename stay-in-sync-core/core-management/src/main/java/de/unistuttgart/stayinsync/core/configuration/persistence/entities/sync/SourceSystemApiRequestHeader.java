package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class SourceSystemApiRequestHeader extends ApiRequestHeader {

    @ManyToOne
    public SourceSystemEndpoint sourceSystemEndpoint;

}
