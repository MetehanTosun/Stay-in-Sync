package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import java.util.List;
import java.util.Set;

@Entity
public class SourceSystemApiRequestConfiguration extends ApiRequestConfiguration {

    @ManyToMany(mappedBy = "sourceSystemApiRequestConfigrations")
    public Set<Transformation> transformations;

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

}
