package de.unistuttgart.stayinsync.core.configuration.service.serviceedc;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
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

    public EDCAsset createFromDto(EDCAssetDto dto) {
        EDCAsset asset = EDCAssetMapper.fromDto(dto);
        asset.persist();
        return asset;
    }

    public Optional<EDCAsset> update(Long id, EDCAssetDto dto) {
        return findById(id).map(existing -> {
            dto.setId(id);
            EDCAssetMapper.fromDto(dto);
            return existing;
        });
    }

    public boolean delete(Long id) {
        return EDCAsset.deleteById(id);
    }
}
