package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCAssetService {

    public List<EDCAsset> listAll() {
        return EDCAsset.listAll();
    }

    public Optional<EDCAsset> findById(UUID id) {
        return EDCAsset.findByIdOptional(id);
    }

    @Transactional
    public EDCAsset create(EDCAsset entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCAsset> update(UUID id, EDCAsset newState) {
        EDCAsset existing = EDCAsset.findById(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.assetId               = newState.assetId;
        existing.url                   = newState.url;
        existing.type                  = newState.type;
        existing.contentType           = newState.contentType;
        existing.description           = newState.description;
        existing.dataAddress           = newState.dataAddress;
        existing.properties            = newState.properties;
        existing.edcAccessPolicies     = newState.edcAccessPolicies;
        existing.targetSystemEndpoint  = newState.targetSystemEndpoint;
        existing.targetEDC             = newState.targetEDC;
        
        return Optional.of(existing);
    }

    @Transactional
    public boolean delete(UUID id) {
        return EDCAsset.deleteById(id);
    }
}
