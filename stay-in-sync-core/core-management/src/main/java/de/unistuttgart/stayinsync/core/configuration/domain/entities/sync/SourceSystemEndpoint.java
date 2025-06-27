package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystemEndpoint extends SyncSystemEndpoint {

    public String endpointPath;

    public String httpRequestType;

    @ManyToMany(mappedBy = "sourceSystemEndpoints")
    public Set<Transformation> transformations;

    public String jsonSchema;

    @ManyToOne
    public SourceSystem sourceSystem;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemVariable> sourceSystemVariable;

    public static List<SourceSystemEndpoint> listAllWherePollingIsActiveAndUnused() {
        String query = "SELECT sse FROM SourceSystemEndpoint sse " +
                "WHERE sse.pollingActive = true " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM Transformation t " +
                "    WHERE sse MEMBER OF t.sourceSystemEndpoints " +
                "    AND t.syncJob.deployed = true" +
                ")";

        return list(query);
    }

    public static List<SourceSystemEndpoint> findBySourceSystemId(Long sourceSystemId) {
        return find("sourceSystem.id", sourceSystemId).list();
    }
}
