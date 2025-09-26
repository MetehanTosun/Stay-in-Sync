package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.transport.domain.ApiRequestHeaderType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class ApiHeader extends PanacheEntity {


    public String headerName;

    @ElementCollection
    @CollectionTable(name = "header_values", joinColumns = @JoinColumn(name = "api_request_header_id"))
    @Column(name = "value")
    public Set<String> values = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "header_type")
    public ApiRequestHeaderType headerType;
    @ManyToOne(fetch = FetchType.LAZY)
    public SyncSystem syncSystem;

    public static List<ApiHeader> findBySyncSystemId(Long endpointId) {
        return find("syncSystem.id", endpointId).list();
    }
}
