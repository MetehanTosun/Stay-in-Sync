package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;

import java.util.List;
import java.util.Set;

@Entity
public class SourceSystemEndpoint extends PanacheEntity {

    public String endpointPath;

    public String httpRequestType;

    public boolean pollingActive;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemApiQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemApiRequestHeader> apiRequestHeaders;

    @ManyToMany(mappedBy = "sourceSystemEndpoints")
    public Set<Transformation> transformations;

    /**
     * JSON-Schema für diesen Endpoint, automatisch per Extractor generiert.
     */
    @Column(columnDefinition = "TEXT")
    public String jsonSchema;

    /**
     * Modus der Schema-Erstellung: "auto" (Extractor) oder "manual" (Editor).
     */
    @Column(length = 20)
    public String schemaMode;

    public int pollingRateInMs;

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
}
