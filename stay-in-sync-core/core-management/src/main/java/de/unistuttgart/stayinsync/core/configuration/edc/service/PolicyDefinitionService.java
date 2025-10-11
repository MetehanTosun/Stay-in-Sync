package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.client.PolicyDefinitionEdcClient;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.PolicyDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;

import de.unistuttgart.stayinsync.core.configuration.edc.mapping.PolicyDefinitionMapper;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.transaction.Transactional;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@ApplicationScoped
public class PolicyDefinitionService extends EdcEntityService<PolicyDefinitionDto> {

    @Transactional
    @Override
    public PolicyDefinitionDto getEntityWithSyncCheck(final Long id) throws EntityNotFoundException, EntityFetchingException{
        final PolicyDefinition persistedPolicyDefinition = this.getPolicyDefinitionFromDatabase(id);
        final EDCInstance edcOfPolicyDefinition = persistedPolicyDefinition.getTargetEDC();
        final PolicyDefinitionEdcClient client = PolicyDefinitionEdcClient.createClient(edcOfPolicyDefinition.getControlPlaneManagementUrl());
        try {

            final PolicyDefinitionDto fetchedPolicyDefinitionFromEdc = this.extractPolicyDefinitionDtosFromResponse(client.getPolicyDefinitionById(edcOfPolicyDefinition.getApiKey(), persistedPolicyDefinition.getPolicyDefinitionId())).getFirst();
            final PolicyDefinitionDto persistedPolicyDefinitionAsDto = PolicyDefinitionMapper.mapper.entityToDto(persistedPolicyDefinition);
            persistedPolicyDefinition.setEntityOutOfSync(!persistedPolicyDefinitionAsDto.equals(fetchedPolicyDefinitionFromEdc));
            return persistedPolicyDefinitionAsDto;

        } catch (DatabaseEntityOutOfSyncException e) {
            Log.warnf(e.getMessage(), persistedPolicyDefinition.getPolicyDefinitionId());
            persistedPolicyDefinition.setEntityOutOfSync(true);
            return PolicyDefinitionMapper.mapper.entityToDto(persistedPolicyDefinition);

        } catch (AuthorizationFailedException | ResponseInvalidFormatException | ConnectionToEdcFailedException e) {
            Log.errorf(e.getMessage(), persistedPolicyDefinition.getPolicyDefinitionId());
            throw new EntityFetchingException(e.getMessage());

        }
    }

    @Transactional
    @Override
    public List<PolicyDefinitionDto> getEntitiesAsListWithSyncCheck(final Long edcId) throws EntityNotFoundException, EntityFetchingException {
        final EDCInstance edcInstance = getEdcInstanceFromDatabase(edcId);
        final List<PolicyDefinition> persistedPolicyDefinitionsForEdcInstance = getAllPolicyDefinitionsForEdcInstanceFromDatabase(edcInstance);
        try {
            this.checkForAllPolicyDefinitionsForThirdPartyChanges(persistedPolicyDefinitionsForEdcInstance, edcInstance);
            return persistedPolicyDefinitionsForEdcInstance.stream().map(PolicyDefinitionMapper.mapper::entityToDto).toList();

        } catch (AuthorizationFailedException | ConnectionToEdcFailedException | ResponseInvalidFormatException | DatabaseEntityOutOfSyncException e) {
            Log.errorf("PolicyDefinitionList fetching from Edc failed with message: " + e.getMessage());
            throw new EntityFetchingException("PolicyDefinitionList fetching from Edc failed with message: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public PolicyDefinitionDto createEntityInDatabaseAndEdc(final Long edcId, final PolicyDefinitionDto policyDefinitionDto) throws EntityNotFoundException, EntityCreationFailedException {
        final EDCInstance policyDefinitionsEdc = getEdcInstanceFromDatabase(edcId);
        final PolicyDefinition policyDefinitionToPersist = PolicyDefinitionMapper.mapper.dtoToEntity(policyDefinitionDto);
        PolicyDefinitionEdcClient client = PolicyDefinitionEdcClient.createClient(policyDefinitionToPersist.getTargetEDC().getControlPlaneManagementUrl());
        try {
            final PolicyDefinitionDto uploadedPolicyDefinition = this.extractPolicyDefinitionDtosFromResponse(client.createPolicyDefinition(policyDefinitionToPersist.getTargetEDC().getApiKey(), policyDefinitionDto)).getFirst();
            if (uploadedPolicyDefinition.equals(PolicyDefinitionMapper.mapper.entityToDto(policyDefinitionToPersist))) {
                policyDefinitionToPersist.setTargetEDC(policyDefinitionsEdc);
                PolicyDefinition.persist(policyDefinitionToPersist);
            } else {
                Log.warnf("The PolicyDefinition was created on the edc, but its information on the edc did not match the information " +
                        "in the database during a later check. Database asset entry is now updated based on the information for it on the edc.");
                policyDefinitionToPersist.updateValuesWithPolicyDefinitionDto(uploadedPolicyDefinition);
                Asset.persist(policyDefinitionToPersist);
            }
            return uploadedPolicyDefinition;
        } catch (ResponseInvalidFormatException | DatabaseEntityOutOfSyncException | AuthorizationFailedException | ConnectionToEdcFailedException e) {
            Log.errorf("PolicyDefinition creation from Edc failed with message: " + e.getMessage());
            throw new EntityCreationFailedException("PolicyDefinition Creation failed with message: " + e.getMessage(), e);
        }
    }


    @Transactional
    @Override
    public PolicyDefinitionDto updateEntityInDatabaseAndEdc(final Long  policyDefinitionId, final PolicyDefinitionDto updatedPolicyDefinitionDto)  throws EntityNotFoundException, EntityUpdateFailedException {
        final PolicyDefinition persistedPolicyDefinition = this.getPolicyDefinitionFromDatabase(policyDefinitionId);
        final PolicyDefinitionEdcClient client = PolicyDefinitionEdcClient.createClient(persistedPolicyDefinition.getTargetEDC().getControlPlaneManagementUrl());
        try {
            final PolicyDefinitionDto returnedPolicyDefinitionAfterUpdateOnEdc = extractPolicyDefinitionDtosFromResponse(client.updatePolicyDefinition(persistedPolicyDefinition.getTargetEDC().getApiKey(), persistedPolicyDefinition.getPolicyDefinitionId(), updatedPolicyDefinitionDto)).getFirst();
            persistedPolicyDefinition.updateValuesWithPolicyDefinitionDto(returnedPolicyDefinitionAfterUpdateOnEdc);
            return PolicyDefinitionMapper.mapper.entityToDto(persistedPolicyDefinition);
        } catch (ResponseInvalidFormatException | DatabaseEntityOutOfSyncException | AuthorizationFailedException |
                 ConnectionToEdcFailedException e) {
            throw new EntityUpdateFailedException("Update of PolicyDefinition failed because it could not be uploaded to the EDC " + e.getMessage());
        }
    }


    @Transactional
    @Override
    public void deleteEntityFromDatabaseAndEdc(final Long policyDefinitionId) throws EntityNotFoundException, EntityDeletionFailedException {
        PolicyDefinition policyDefinitionToDelete = this.getPolicyDefinitionFromDatabase(policyDefinitionId);
        PolicyDefinitionEdcClient client = PolicyDefinitionEdcClient.createClient(policyDefinitionToDelete.getTargetEDC().getControlPlaneManagementUrl());
        try (RestResponse<Void> response = client.deletePolicyDefinition(
                policyDefinitionToDelete.getTargetEDC().getApiKey(),
                policyDefinitionToDelete.getPolicyDefinitionId()
        )) {
            if (response.getStatus() == 200) {
                PolicyDefinition.deleteById(policyDefinitionId);
                Log.infof("PolicyDefinition successfully deleted from Edc and Database.", policyDefinitionId);
            } else {
                final String exceptionMessage = "PolicyDefinition could not be deleted from edc.";
                Log.errorf(exceptionMessage, policyDefinitionId);
                throw new EntityDeletionFailedException(exceptionMessage);
            }
        }
    }

    /**
     * Returns policy definition from database if it´s found with id. In other case EntityNotFoundException is thrown.
     *
     * @param id used to find the policy definition
     * @return the policy definition if it´s found.
     * @throws EntityNotFoundException if no policy definition was found.
     */
    private PolicyDefinition getPolicyDefinitionFromDatabase(final Long id) throws EntityNotFoundException {
        final PolicyDefinition policyDefinition = PolicyDefinition.findById(id);
        if (policyDefinition == null) {
            final String exceptionMessage = "PolicyDefinition could not be found with the given id.";
            Log.errorf(exceptionMessage, id);
            throw new EntityNotFoundException(exceptionMessage);
        }
        return policyDefinition;
    }

    /**
     * Returns all policy definitions from database that are part of the given edcInstance.
     *
     * @param edcInstance used to filter policy definitions for specifically this edc.
     * @return all policy definitions for the given edc.
     */
    private List<PolicyDefinition> getAllPolicyDefinitionsForEdcInstanceFromDatabase(final EDCInstance edcInstance) {
        final List<PolicyDefinition> allPolicyDefinitions = PolicyDefinition.listAll();
        return allPolicyDefinitions.stream()
                .filter(policyDefinition -> policyDefinition.getTargetEDC().equals(edcInstance))
                .toList();
    }

    /**
     * Returns edcInstance from database if it´s found with id. In other case EntityNotFoundException is thrown.
     *
     * @param id used to find the edcInstance
     * @return the edcInstance if it´s found.
     * @throws EntityNotFoundException if no edcInstance was found.
     */
    private EDCInstance getEdcInstanceFromDatabase(final Long id) throws EntityNotFoundException {
        final EDCInstance edcInstance = EDCInstance.findById(id);
        if (edcInstance == null) {
            final String exceptionMessage = "EdcInstance could not be found with the given id.";
            Log.errorf(exceptionMessage, id);
            throw new EntityNotFoundException(exceptionMessage);
        }
        return edcInstance;
    }

    private void checkForAllPolicyDefinitionsForThirdPartyChanges(final List<PolicyDefinition> policyDefinitionsToCheck, final EDCInstance edcInstance) throws AuthorizationFailedException, DatabaseEntityOutOfSyncException, ConnectionToEdcFailedException, ResponseInvalidFormatException {
        final Map<String, PolicyDefinitionDto> edcPolicyDefinitionDtosMappedToOwnPolicyDefinitionIds = extractPolicyDefinitionDtosFromResponse(PolicyDefinitionEdcClient.createClient(edcInstance.getControlPlaneManagementUrl()).getAllPolicyDefinitions(edcInstance.getApiKey()))
                .stream()
                .collect(Collectors.toMap(PolicyDefinitionDto::policyDefinitionId, policyDefinition -> policyDefinition));

        for (PolicyDefinition persistedPolicyDefinition : policyDefinitionsToCheck) {
            final PolicyDefinitionDto persistedPolicyDefinitionAsDto = PolicyDefinitionMapper.mapper.entityToDto(persistedPolicyDefinition);
            final PolicyDefinitionDto edcPolicyDefinitionDto = edcPolicyDefinitionDtosMappedToOwnPolicyDefinitionIds.get(persistedPolicyDefinitionAsDto.policyDefinitionId());
            persistedPolicyDefinition.setEntityOutOfSync(!persistedPolicyDefinitionAsDto.equals(edcPolicyDefinitionDto));
        }
    }

    /**
     * Extracts PolicyDefinitionDtos from EDC response.
     * Always returns a List (empty for DELETE, single element for GET/POST/PUT, multiple for Post query).
     *
     * @param response the REST response from EDC
     * @return list of PolicyDefinitionDto
     * @throws ResponseInvalidFormatException,DatabaseEntityOutOfSyncException,AuthorizationFailedException,ConnectionToEdcFailedException if response code indicates error or body is invalid
     */
    private List<PolicyDefinitionDto> extractPolicyDefinitionDtosFromResponse(final RestResponse<Object> response) throws
            ResponseInvalidFormatException, DatabaseEntityOutOfSyncException, AuthorizationFailedException, ConnectionToEdcFailedException {
        final int status = response.getStatus();
        this.handleNegativeResponseCodes(status);

        final boolean acceptableResponse = status >= 200 && status < 300;
        final boolean deletionStatusOrEmptyResponse = status == 204 || response.getEntity() == null;

        if (acceptableResponse) {
            if (deletionStatusOrEmptyResponse) {
                return List.of();
            }
            return parseJsonToPolicyDefinitionDtoList(response.getEntity());
        }

        throw new ResponseInvalidFormatException("Edc policy definition request failed with status " + status);
    }

    /**
     * Parses JsonObject into list of PolicyDefinitionDtos.
     * Handles two formats:
     * 1. Direct Array: [{PolicyDefinition}, {PolicyDefinition}, ...] (from query/getAll)
     * 2. Single Object: {PolicyDefinition} (from get/create/update)
     *
     * @param json the JSON response
     * @return list of parsed PolicyDefinitionDtos
     * @throws ResponseInvalidFormatException if format is invalid
     */
    private List<PolicyDefinitionDto> parseJsonToPolicyDefinitionDtoList(final Object json) throws ResponseInvalidFormatException {
        try {
            if (json instanceof JsonArray jsonArray) {
                return jsonArray.stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(jsonObj -> {
                            try {
                                return parseJsonToPolicyDefinitionDto(jsonObj);
                            } catch (ResponseInvalidFormatException e) {
                                Log.error("Failed to parse one policy definition in JSON array", e);
                                return null;
                            }
                        })
                        .toList();
            }

            if (json instanceof JsonObject jsonObject) {
                if (jsonObject.containsKey("@type") || jsonObject.containsKey("@id")) {
                    return List.of(parseJsonToPolicyDefinitionDto(jsonObject));
                }
            }

            throw new ResponseInvalidFormatException("Unexpected EDC response format - neither PolicyDefinition object nor PolicyDefinition array");

        } catch (ClassCastException e) {
            throw new ResponseInvalidFormatException("Failed to parse EDC response structure", e);
        }
    }

    /**
     * Parses a single JsonObject into a PolicyDefinitionDto.
     */
    private PolicyDefinitionDto parseJsonToPolicyDefinitionDto(final JsonObject jsonObject)
            throws ResponseInvalidFormatException {
        try {
            return jsonObject.mapTo(PolicyDefinitionDto.class);
        } catch (IllegalArgumentException e) {
            Log.error("Failed to parse JSON to PolicyDefinitionDto: " + jsonObject.encode(), e);
            throw new ResponseInvalidFormatException("Failed to parse JSON to PolicyDefinitionDto", e);
        }
    }


}
