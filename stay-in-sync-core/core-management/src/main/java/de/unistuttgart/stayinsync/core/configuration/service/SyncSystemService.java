package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncSystem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;

@ApplicationScoped
@Transactional(REQUIRED)
public class SyncSystemService {
    public Optional<SyncSystem> findSyncSystemById(Long syncSystemId) {
        return SyncSystem.findByIdOptional(syncSystemId);
    }
}
