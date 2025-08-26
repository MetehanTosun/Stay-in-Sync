package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCContractDefinitionService {

    public List<EDCContractDefinition> listAll() {
        return EDCContractDefinition.listAll();
    }

    public Optional<EDCContractDefinition> findById(UUID id) {
        return EDCContractDefinition.findByIdOptional(id);
    }

    @Transactional
    public EDCContractDefinition create(EDCContractDefinition entity) {
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCContractDefinition> update(UUID id, EDCContractDefinitionDto dto) {
        return findById(id).map(existing -> {
            // nur die zu ändernden Felder übernehmen
            existing.contractDefinitionId = dto.getContractDefinitionId();
            existing.asset              = EDCAsset.findById(dto.getAssetId());
            existing.accessPolicy       = EDCPolicy.findById(dto.getAccessPolicyId());
            existing.contractPolicy     = EDCPolicy.findById(dto.getContractPolicyId());
            return existing;
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        return EDCContractDefinition.deleteById(id);
    }
}

