package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfigurationAction;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.targetsystem.RequestConfigurationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.CreateArcDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.ActionDefinitionDTO;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class TargetSystemApiRequestConfigurationService {

    @Inject
    RequestConfigurationMapper mapper;

    public TargetSystemApiRequestConfiguration create(CreateArcDTO dto){
        Log.debugf("Attempting to create Target ARC with alias '%s'", dto.alias());

        validateArcDefinition(dto);

        TargetSystem targetSystem = TargetSystem.<TargetSystem>findByIdOptional(dto.targetSystemId())
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "TargetSystem not found.", "TargetSystem was not found with id: " + dto.targetSystemId()));

        TargetSystemApiRequestConfiguration newArc = mapper.mapToEntity(dto);
        newArc.targetSystem = targetSystem;
        newArc.persist();

        List<TargetSystemApiRequestConfigurationAction> actions = createAndPersistActions(dto.actions(), newArc, targetSystem);
        newArc.actions = actions;

        Log.infof("Successfully created Target ARC '%s' with ID %d", newArc.alias, newArc.id);
        return newArc;
    }

    @Transactional(SUPPORTS)
    public List<TargetSystemApiRequestConfiguration> findAllByTargetSystemId(Long targetSystemId) {
        return TargetSystemApiRequestConfiguration.list("targetSystem.id", targetSystemId);
    }

    @Transactional(SUPPORTS)
    public Optional<TargetSystemApiRequestConfiguration> findById(Long id) {
        return TargetSystemApiRequestConfiguration.findByIdOptional(id);
    }

    public boolean deleteById(Long id) {
        return TargetSystemApiRequestConfiguration.deleteById(id);
    }

    private List<TargetSystemApiRequestConfigurationAction> createAndPersistActions(List<ActionDefinitionDTO> actionDtos, TargetSystemApiRequestConfiguration parentArc, TargetSystem targetSystem) {
        List<TargetSystemApiRequestConfigurationAction> persistedActions = new ArrayList<>();
        for (ActionDefinitionDTO actionDto : actionDtos) {
            TargetSystemEndpoint endpoint = TargetSystemEndpoint.<TargetSystemEndpoint>findByIdOptional(actionDto.endpointId())
                    .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "TargetSystemEndpoint not found.", "TargetSystemEndpoint was not found with id: " + actionDto.endpointId()));

            if (!endpoint.syncSystem.id.equals(targetSystem.id)) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "Endpoint Mismatch", "Endpoint with id %d does not belong to TargetSystem with id %d.", endpoint.id, targetSystem.id);
            }

            TargetSystemApiRequestConfigurationAction action = new TargetSystemApiRequestConfigurationAction();
            action.targetSystemApiRequestConfiguration = parentArc;
            action.endpoint = endpoint;
            action.actionRole = actionDto.actionRole();
            action.executionOrder = actionDto.executionOrder();
            action.persist();
            persistedActions.add(action);
        }
        return persistedActions;
    }

    private void validateArcDefinition(CreateArcDTO dto) {
        final int REQUIRED_BASIC_API_ACTION_COUNT = 1;
        switch(dto.arcPatternType()){
            case LIST_UPSERT:
            case OBJECT_UPSERT:
                validateUpsertPattern(dto);
                break;
            case BASIC_API:
                if (dto.actions().size() != REQUIRED_BASIC_API_ACTION_COUNT){
                    throw new CoreManagementException("Invalid BASIC_API definition", "Exactly one action is required to describe a BASIC_API definition.");
                }
                break;
            case CUSTOM_WORKFLOW:
                throw new CoreManagementException("Custom Workflows not Supported", "Custom Workflows are not yet supported and thus will not function properly.");
        }
    }

    private void validateUpsertPattern(CreateArcDTO dto) {
        final int REQUIRED_UPSERT_ACTION_COUNT = 3;
        if (dto.actions().size() != REQUIRED_UPSERT_ACTION_COUNT) {
            throw new CoreManagementException("Invalid Upsert definition.", "Exactly three actions (CHECK, CREATE, UPDATE) are required.");
        }
        Set<TargetApiRequestConfigurationActionRole> roles = dto.actions().stream()
                .map(ActionDefinitionDTO::actionRole)
                .collect(Collectors.toSet());

        if (!roles.containsAll(Set.of(TargetApiRequestConfigurationActionRole.CHECK, TargetApiRequestConfigurationActionRole.CREATE, TargetApiRequestConfigurationActionRole.UPDATE))) {
            throw new CoreManagementException("Invalid Upsert definition.", "Actions must have the roles CHECK, CREATE, and UPDATE.");
        }
    }
}
