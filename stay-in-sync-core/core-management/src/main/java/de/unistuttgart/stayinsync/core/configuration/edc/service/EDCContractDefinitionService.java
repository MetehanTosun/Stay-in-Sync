package de.unistuttgart.stayinsync.core.configuration.edc.service;


import de.unistuttgart.stayinsync.core.configuration.edc.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCContractDefinitionMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCContractDefinitionService {

    public List<EDCContractDefinition> listAll() {
        return EDCContractDefinition.listAll();
    }

    public Optional<EDCContractDefinition> findById(Long id) {
        return Optional.ofNullable(EDCContractDefinition.findById(id));
    }

    @Transactional
    public EDCContractDefinition createFromDto(EDCContractDefinitionDto dto) {
        var entity = EDCContractDefinitionMapper.fromDto(dto);
        entity.persist();
        return entity;
    }

    @Transactional
    public Optional<EDCContractDefinition> update(Long id, EDCContractDefinitionDto dto) {
        return findById(id).map(existing -> {
            dto.setId(id);
            // Mapper wird das bestehende entity befüllen
            EDCContractDefinitionMapper.fromDto(dto);
            return existing;
        });
    }

    @Transactional
    public boolean delete(Long id) {
        return EDCContractDefinition.deleteById(id);
    }
}
