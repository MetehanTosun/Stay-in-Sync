package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.transport.domain.ApiEndpointQueryParamType;
import de.unistuttgart.stayinsync.transport.dto.SchemaType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class ApiEndpointQueryParam extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    public SyncSystemEndpoint syncSystemEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_param_type")
    public ApiEndpointQueryParamType queryParamType;

    public String paramName;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_param_schema_type")
    public SchemaType schemaType;

    @ElementCollection
    @CollectionTable(name = "query_param_values", joinColumns = @JoinColumn(name = "api_endpoint_query_param_id"))
    @Column(name = "value")
    public Set<String> values = new HashSet<>();

    public static List<ApiEndpointQueryParam> findByEndpointId(Long endpointId) {
        // Use a simple query that avoids inheritance issues
        return find("syncSystemEndpoint.id", endpointId).list();
    }
}
