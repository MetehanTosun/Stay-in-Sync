package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiRequestConfiguration;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class ApiRequestConfigurationService {

    @Transactional(SUPPORTS)
    public Optional<ApiRequestConfiguration> findApiRequestConfigurationById(Long id) {
        Log.infof("Finding request-configurations by id = %d", id);
        return ApiRequestConfiguration.findByIdOptional(id);
    }


}
