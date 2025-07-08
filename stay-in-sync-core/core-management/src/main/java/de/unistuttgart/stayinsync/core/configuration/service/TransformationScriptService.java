package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationScript;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class TransformationScriptService {

    @Inject
    TransformationScriptMapper mapper;

    @Transactional(SUPPORTS)
    public Optional<TransformationScript> findById(Long id) {
        Log.infof("Finding transformation script by id: %s", id);
        return TransformationScript.findByIdOptional(id);
    }

    public Optional<TransformationScript> update(Long id, TransformationScript scriptFromDTO) {
        Log.infof("Updating transformation script with id: %s", id);
        return TransformationScript.<TransformationScript>findByIdOptional(id)
                .map(targetScript -> {
                    mapper.mapFullUpdate(scriptFromDTO, targetScript);
                    return targetScript;
                });
    }
}
