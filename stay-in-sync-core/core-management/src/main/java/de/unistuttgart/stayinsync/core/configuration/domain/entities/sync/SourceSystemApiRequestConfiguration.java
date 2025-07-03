package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.util.List;
import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystemApiRequestConfiguration extends ApiRequestConfiguration {

    @ManyToMany(mappedBy = "sourceSystemApiRequestConfigrations")
    public Set<Transformation> transformations;

    @ManyToOne
    public SourceSystem sourceSystem;

    @ManyToOne
    public SourceSystemEndpoint sourceSystemEndpoint;

    public int pollingIntervallTimeInMs;

    public static List<SourceSystemApiRequestConfiguration> listAllWherePollingIsActiveAndUnused() {
        String query = "SELECT sse FROM SourceSystemApiRequestConfiguration sse " +
                "WHERE sse.active = true " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM Transformation t " +
                "    WHERE sse MEMBER OF t.sourceSystemEndpoints " +
                "    AND t.syncJob.deployed = true" +
                ")";

        return list(query);
    }

    public static List<SourceSystemApiRequestConfiguration> findBySourceSystemId(Long sourceSystemId) {
        return find("sourceSystem.id", sourceSystemId).list();
    }

    public static List<SourceSystemApiRequestConfiguration> findByEndpointId(Long endpointId) {
        return find("sourceSystemEndpoint.id", endpointId).list();
    }

}
