package de.unistuttgart.stayinsync.core.configuration.repository;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SourceSystemEndpointRepository implements PanacheRepository<SourceSystemEndpoint> {

    /**
     * Liefert alle Endpunkte für ein gegebenes Quellsystem.
     */
    public List<SourceSystemEndpoint> listBySourceSystemId(Long sourceSystemId) {
        return list("sourceSystem.id", sourceSystemId);
    }

    /**
     * Finds a single endpoint by its ID.
     */
    public Optional<SourceSystemEndpoint> findByEndpointId(Long id) {
        return find("id", id).firstResultOptional();
    }
}