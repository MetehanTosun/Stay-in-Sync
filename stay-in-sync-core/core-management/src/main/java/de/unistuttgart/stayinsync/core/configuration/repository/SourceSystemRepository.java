package de.unistuttgart.stayinsync.core.configuration.repository;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SourceSystemRepository implements PanacheRepository<SourceSystem> {
}