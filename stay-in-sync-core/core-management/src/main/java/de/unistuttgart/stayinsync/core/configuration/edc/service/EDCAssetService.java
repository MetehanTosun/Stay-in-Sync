package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCAssetService {

    public List<EDCAsset> listAll() {
        return EDCAsset.listAll();
    }

    public Optional<EDCAsset> findById(Long id) {
        return Optional.ofNullable(EDCAsset.findById(id));
    }

    @Transactional
    public EDCAsset createFromDto(EDCAsset entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCAsset> update(Long id, EDCAsset newState) {
        EDCAsset existing = EDCAsset.findById(id);
        if (existing == null) {
            return Optional.empty();
        }
        // Felder kopieren
        existing.assetId = newState.assetId;
        existing.dataAddress = newState.dataAddress;
        existing.properties = newState.properties;
        existing.edcAccessPolicies = newState.edcAccessPolicies;
        existing.targetSystemEndpoint = newState.targetSystemEndpoint;
        existing.targetEDC = newState.targetEDC;
        return Optional.of(existing);
    }

    @Transactional
    public boolean delete(Long id) {
        return EDCAsset.deleteById(id);
    }
}
