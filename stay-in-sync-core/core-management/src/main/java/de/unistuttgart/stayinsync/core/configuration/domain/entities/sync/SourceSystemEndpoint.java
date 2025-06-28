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

    public static List<SourceSystemEndpoint> listBySourceSystemId(Long sourceSystemId) {
        return list("sourceSystem.id", sourceSystemId);
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }
    public String getSchemaMode() {
        return schemaMode;
    }
    public void setSchemaMode(String schemaMode) {
        this.schemaMode = schemaMode;
    }
    public String getEndpointPath() {
        return endpointPath;
    }
    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }
    public String getHttpRequestType() {
        return httpRequestType;
    }
    public void setHttpRequestType(String httpRequestType) {
        this.httpRequestType = httpRequestType;
    }
    public boolean isPollingActive() {
        return pollingActive;
    }
    public void setPollingActive(boolean pollingActive) {
        this.pollingActive = pollingActive;
    }
    public int getPollingRateInMs() {
        return pollingRateInMs;
    }
    public void setPollingRateInMs(int pollingRateInMs) {
        this.pollingRateInMs = pollingRateInMs;
    }
    public Set<SourceSystemApiQueryParam> getApiQueryParams() {
        return apiQueryParams;
    }
    public void setApiQueryParams(Set<SourceSystemApiQueryParam> apiQueryParams) {
        this.apiQueryParams = apiQueryParams;
    }
    public Set<SourceSystemApiRequestHeader> getApiRequestHeaders() {
        return apiRequestHeaders;
    }
    public void setApiRequestHeaders(Set<SourceSystemApiRequestHeader> apiRequestHeaders) {
        this.apiRequestHeaders = apiRequestHeaders;
    }
    public Set<SourceSystemVariable> getSourceSystemVariable() {
        return sourceSystemVariable;
    }
    public void setSourceSystemVariable(Set<SourceSystemVariable> sourceSystemVariable) {
        this.sourceSystemVariable = sourceSystemVariable;
    }
    public SourceSystem getSourceSystem() {
        return sourceSystem;
    }
    public void setSourceSystem(SourceSystem sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
}
