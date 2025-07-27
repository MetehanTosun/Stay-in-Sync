package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import jakarta.enterprise.context.ApplicationScoped;

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

    public EDCAsset createFromDto(EDCAsset asset) {
        asset.persist();
        return asset;
    }

    public Optional<EDCAsset> update(Long id, EDCAsset newState) {
        return findById(id).map(existing -> {
            existing.assetId = newState.assetId;
            existing.dataAddress = newState.dataAddress;
            existing.properties = newState.properties;
            existing.targetSystemEndpoint = newState.targetSystemEndpoint;
            existing.targetEDC = newState.targetEDC;
            return existing;
        });
    }

    public boolean delete(Long id) {
        return EDCAsset.deleteById(id);
    }
}
