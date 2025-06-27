package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class SourceSystemApiQueryParam extends ApiQueryParam {

    @ManyToOne
    public SourceSystemEndpoint sourceSystemEndpoint;

}
