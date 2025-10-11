package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.client.ContractDefinitionEdcClient;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.ContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.ContractDefinitionMapper;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service-Class to manage ContractDefinition objects.
 * Makes sure that if the state of a contract definition is changed in the database these changes are represented in its corresponding edc too.
 * If contract definitions are fetched from the database then they are compared to their one self from the edc. If the states of both versions are inconsistent their boolean thirdPartyChanges is changed to true.
 */
@ApplicationScoped
public class ContractDefinitionService extends EdcEntityService<ContractDefinitionDto> {

    @Transactional
    @Override
    public ContractDefinitionDto getEntityWithSyncCheck(final Long id) throws EntityNotFoundException, EntityFetchingException {
        final ContractDefinition persistedContractDefinition = this.getContractDefinitionFromDatabase(id);
        final EDCInstance edcOfContractDefinition = persistedContractDefinition.getTargetEdc();
        final ContractDefinitionEdcClient client = ContractDefinitionEdcClient.createClient(edcOfContractDefinition.getControlPlaneManagementUrl(), edcOfContractDefinition.getProtocolVersion());
        try {

            final ContractDefinitionDto fetchedContractDefinitionFromEdc = this.extractContractDefinitionDtosFromResponse(client.getContractDefinitionById(edcOfContractDefinition.getApiKey(), persistedContractDefinition.getContractDefinitionId())).getFirst();
            final ContractDefinitionDto persistedContractDefinitionAsDto = ContractDefinitionMapper.mapper.entityToDto(persistedContractDefinition);
            persistedContractDefinition.setEntityOutOfSync(!persistedContractDefinitionAsDto.equals(fetchedContractDefinitionFromEdc));
            return persistedContractDefinitionAsDto;

        } catch (DatabaseEntityOutOfSyncException e) {
            Log.warnf(e.getMessage(), persistedContractDefinition.getContractDefinitionId());
            persistedContractDefinition.setEntityOutOfSync(true);
            return ContractDefinitionMapper.mapper.entityToDto(persistedContractDefinition);

        } catch (AuthorizationFailedException | ResponseInvalidFormatException | ConnectionToEdcFailedException e) {
            Log.errorf(e.getMessage(), persistedContractDefinition.getContractDefinitionId());
            throw new EntityFetchingException(e.getMessage());

        }
    }

    @Transactional
    @Override
    public List<ContractDefinitionDto> getEntitiesAsListWithSyncCheck(final Long edcId) throws EntityNotFoundException, EntityFetchingException {
        final EDCInstance edcInstance = getEdcInstanceFromDatabase(edcId);
        final List<ContractDefinition> persistedContractDefinitionsForEdcInstance = getAllContractDefinitionsForEdcInstanceFromDatabase(edcInstance);
        try {
            this.checkForAllContractDefinitionsForThirdPartyChanges(persistedContractDefinitionsForEdcInstance, edcInstance);
            return persistedContractDefinitionsForEdcInstance.stream().map(ContractDefinitionMapper.mapper::entityToDto).toList();

        } catch (AuthorizationFailedException | ConnectionToEdcFailedException | ResponseInvalidFormatException | DatabaseEntityOutOfSyncException e) {
            Log.errorf("ContractDefinitionList fetching from Edc failed with message: " + e.getMessage());
            throw new EntityFetchingException("ContractDefinitionList fetching from Edc failed with message: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public ContractDefinitionDto createEntityInDatabaseAndEdc(final Long edcId, final ContractDefinitionDto contractDefinitionDto) throws EntityNotFoundException, EntityCreationFailedException {
        final EDCInstance contractDefinitionsEdc = getEdcInstanceFromDatabase(edcId);
        final ContractDefinition contractDefinitionToPersist = ContractDefinitionMapper.mapper.dtoToEntity(contractDefinitionDto);
        ContractDefinitionEdcClient client = ContractDefinitionEdcClient.createClient(contractDefinitionToPersist.getTargetEdc().getControlPlaneManagementUrl(), contractDefinitionToPersist.getTargetEdc().getProtocolVersion());
        try {
            final ContractDefinitionDto uploadedContractDefinition = this.extractContractDefinitionDtosFromResponse(client.createContractDefinition(contractDefinitionToPersist.getTargetEdc().getApiKey(), contractDefinitionDto)).getFirst();
            if (uploadedContractDefinition.equals(ContractDefinitionMapper.mapper.entityToDto(contractDefinitionToPersist))) {
                contractDefinitionToPersist.setTargetEdc(contractDefinitionsEdc);
                ContractDefinition.persist(contractDefinitionToPersist);
            } else {
                Log.warnf("The ContractDefinition was created on the edc, but its information on the edc did not match the information " +
                        "in the database during a later check. Database contract definition entry is now updated based on the information for it on the edc.");
                contractDefinitionToPersist.updateValuesWithContractDefinitionDto(uploadedContractDefinition);
                ContractDefinition.persist(contractDefinitionToPersist);
            }
            return uploadedContractDefinition;
        } catch (ResponseInvalidFormatException | DatabaseEntityOutOfSyncException | AuthorizationFailedException | ConnectionToEdcFailedException e) {
            Log.errorf("ContractDefinition creation from Edc failed with message: " + e.getMessage());
            throw new EntityCreationFailedException("ContractDefinition Creation failed with message: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public ContractDefinitionDto updateEntityInDatabaseAndEdc(final Long contractDefinitionId, final ContractDefinitionDto updatedContractDefinitionDto) throws EntityNotFoundException, EntityUpdateFailedException {
        final ContractDefinition persistedContractDefinition = this.getContractDefinitionFromDatabase(contractDefinitionId);
        final ContractDefinitionEdcClient client = ContractDefinitionEdcClient.createClient(persistedContractDefinition.getTargetEdc().getControlPlaneManagementUrl(), persistedContractDefinition.getTargetEdc().getProtocolVersion());
        try {
            final ContractDefinitionDto returnedContractDefinitionAfterUpdateOnEdc = extractContractDefinitionDtosFromResponse(client.updateContractDefinition(persistedContractDefinition.getTargetEdc().getApiKey(), persistedContractDefinition.getContractDefinitionId(), updatedContractDefinitionDto)).getFirst();
            persistedContractDefinition.updateValuesWithContractDefinitionDto(returnedContractDefinitionAfterUpdateOnEdc);
            return ContractDefinitionMapper.mapper.entityToDto(persistedContractDefinition);
        } catch (ResponseInvalidFormatException | DatabaseEntityOutOfSyncException | AuthorizationFailedException |
                 ConnectionToEdcFailedException e) {
            throw new EntityUpdateFailedException("Update of ContractDefinition failed because it could not be uploaded to the EDC " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void deleteEntityFromDatabaseAndEdc(final Long id) throws EntityNotFoundException, EntityDeletionFailedException {
        ContractDefinition contractDefinitionToDelete = this.getContractDefinitionFromDatabase(id);
        ContractDefinitionEdcClient client = ContractDefinitionEdcClient.createClient(contractDefinitionToDelete.getTargetEdc().getControlPlaneManagementUrl(), contractDefinitionToDelete.getTargetEdc().getProtocolVersion());
        try (RestResponse<Void> response = client.deleteContractDefinition(
                contractDefinitionToDelete.getTargetEdc().getApiKey(),
                contractDefinitionToDelete.getContractDefinitionId()
        )) {
            if (response.getStatus() == 200) {
                ContractDefinition.deleteById(id);
                Log.infof("ContractDefinition successfully deleted from Edc and Database.", id);
            } else {
                final String exceptionMessage = "ContractDefinition could not be deleted from edc.";
                Log.errorf(exceptionMessage, id);
                throw new EntityDeletionFailedException(exceptionMessage);
            }
        }
    }

    /**
     * Returns contract definition from database if it´s found with id. In other case EntityNotFoundException is thrown.
     *
     * @param id used to find the contract definition
     * @return the contract definition if it´s found.
     * @throws EntityNotFoundException if no contract definition was found.
     */
    private ContractDefinition getContractDefinitionFromDatabase(final Long id) throws EntityNotFoundException {
        final ContractDefinition contractDefinition = ContractDefinition.findById(id);
        if (contractDefinition == null) {
            final String exceptionMessage = "ContractDefinition could not be found with the given id.";
            Log.errorf(exceptionMessage, id);
            throw new EntityNotFoundException(exceptionMessage);
        }
        return contractDefinition;
    }

    /**
     * Returns all contract definitions from database that are part of the given edcInstance.
     *
     * @param edcInstance used to filter contract definitions for specifically this edc.
     * @return all contract definitions for the given edc.
     */
    private List<ContractDefinition> getAllContractDefinitionsForEdcInstanceFromDatabase(final EDCInstance edcInstance) {
        final List<ContractDefinition> allContractDefinitions = ContractDefinition.listAll();
        return allContractDefinitions.stream()
                .filter(contractDefinition -> contractDefinition.getTargetEdc().equals(edcInstance))
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

    private void checkForAllContractDefinitionsForThirdPartyChanges(final List<ContractDefinition> contractDefinitionsToCheck, final EDCInstance edcInstance) throws AuthorizationFailedException, DatabaseEntityOutOfSyncException, ConnectionToEdcFailedException, ResponseInvalidFormatException {
        final Map<String, ContractDefinitionDto> edcContractDefinitionDtosMappedToOwnContractDefinitionIds = extractContractDefinitionDtosFromResponse(ContractDefinitionEdcClient.createClient(edcInstance.getControlPlaneManagementUrl(), edcInstance.getProtocolVersion()).getAllContractDefinitions(edcInstance.getApiKey()))
                .stream()
                .collect(Collectors.toMap(ContractDefinitionDto::contractDefinitionId, contractDefinition -> contractDefinition));

        for (ContractDefinition persistedContractDefinition : contractDefinitionsToCheck) {
            final ContractDefinitionDto persistedContractDefinitionAsDto = ContractDefinitionMapper.mapper.entityToDto(persistedContractDefinition);
            final ContractDefinitionDto edcContractDefinitionDto = edcContractDefinitionDtosMappedToOwnContractDefinitionIds.get(persistedContractDefinitionAsDto.contractDefinitionId());
            persistedContractDefinition.setEntityOutOfSync(!persistedContractDefinitionAsDto.equals(edcContractDefinitionDto));
        }
    }

    /**
     * Extracts ContractDefinitionDtos from EDC response.
     * Always returns a List (empty for DELETE, single element for GET/POST/PUT, multiple for Post query).
     *
     * @param response the REST response from EDC
     * @return list of ContractDefinitionDto
     * @throws ResponseInvalidFormatException,DatabaseEntityOutOfSyncException,AuthorizationFailedException,ConnectionToEdcFailedException if response code indicates error or body is invalid
     */
    private List<ContractDefinitionDto> extractContractDefinitionDtosFromResponse(final RestResponse<Object> response) throws
            ResponseInvalidFormatException, DatabaseEntityOutOfSyncException, AuthorizationFailedException, ConnectionToEdcFailedException {
        final int status = response.getStatus();
        this.handleNegativeResponseCodes(status);

        final boolean acceptableResponse = status >= 200 && status < 300;
        final boolean deletionStatusOrEmptyResponse = status == 204 || response.getEntity() == null;

        if (acceptableResponse) {
            if (deletionStatusOrEmptyResponse) {
                return List.of();
            }
            return parseJsonToContractDefinitionDtoList(response.getEntity());
        }

        throw new ResponseInvalidFormatException(
                "Edc contract definition request failed with status " + status);
    }

    /**
     * Parses JsonObject into list of ContractDefinitionDtos.
     * Handles two formats:
     * 1. Direct Array: [{ContractDefinition}, {ContractDefinition}, ...] (from query/getAll)
     * 2. Single Object: {ContractDefinition} (from get/create/update)
     *
     * @param json the JSON response
     * @return list of parsed ContractDefinitionDtos
     * @throws ResponseInvalidFormatException if format is invalid
     */
    private List<ContractDefinitionDto> parseJsonToContractDefinitionDtoList(final Object json) throws ResponseInvalidFormatException {
        try {
            if (json instanceof JsonArray jsonArray) {
                return jsonArray.stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(jsonObj -> {
                            try {
                                return parseJsonToContractDefinitionDto(jsonObj);
                            } catch (ResponseInvalidFormatException e) {
                                Log.error("Failed to parse one contract definition in JSON array", e);
                                return null;
                            }
                        })
                        .toList();
            }

            if (json instanceof JsonObject jsonObject) {
                if (jsonObject.containsKey("@type") || jsonObject.containsKey("@id")) {
                    return List.of(parseJsonToContractDefinitionDto(jsonObject));
                }
            }

            throw new ResponseInvalidFormatException(
                    "Unexpected EDC response format - neither ContractDefinition object nor ContractDefinition array"
            );

        } catch (ClassCastException e) {
            throw new ResponseInvalidFormatException(
                    "Failed to parse EDC response structure", e
            );
        }
    }

    /**
     * Parses a single JsonObject into a ContractDefinitionDto.
     */
    private ContractDefinitionDto parseJsonToContractDefinitionDto(final JsonObject jsonObject)
            throws ResponseInvalidFormatException {
        try {
            return jsonObject.mapTo(ContractDefinitionDto.class);
        } catch (IllegalArgumentException e) {
            Log.error("Failed to parse JSON to ContractDefinitionDto: " + jsonObject.encode(), e);
            throw new ResponseInvalidFormatException(
                    "Failed to parse JSON to ContractDefinitionDto", e
            );
        }
    }

}