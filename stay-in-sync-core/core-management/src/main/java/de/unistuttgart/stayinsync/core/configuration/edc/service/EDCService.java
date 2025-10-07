package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.EntityUpdateFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.EDCInstanceConnector;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.ConnectionToEdcFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.EntityCreationFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCInstanceMapper;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service für die Verwaltung von EDC-Instanzen.
 * Stellt Methoden zum Abrufen, Erstellen, Aktualisieren und Löschen von EDC-Instanzen bereit.
 */
@ApplicationScoped
public class EDCService {

    @Inject
    EDCInstanceMapper mapper;

    @Inject
    EDCInstanceConnector edcConnector;

    /**
     * Findet eine EDC-Instanz anhand ihrer ID.
     * 
     * @param id Die ID der zu findenden EDC-Instanz
     * @return Die gefundene EDC-Instanz als DTO
     * @throws CustomException wenn keine EDC-Instanz mit der angegebenen ID gefunden wurde
     */
    public EDCInstanceDto findById(final UUID id) throws CustomException {
        Log.info("Suche nach EDC-Instanz mit ID: " + id);
        
        final EDCInstance instance = EDCInstance.findById(id);

        if (instance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        Log.info("EDC-Instanz gefunden: " + instance.getName());
        return mapper.entityToDto(instance);
    }

    /**
     * Gibt eine Liste aller EDC-Instanzen zurück.
     * 
     * @return Liste aller EDC-Instanzen als DTOs
     */
    public List<EDCInstanceDto> listAll() {
        Log.info("Abrufen aller EDC-Instanzen");
        
        List<EDCInstance> instances = EDCInstance.listAll();
        List<EDCInstanceDto> dtos = instances.stream()
                .map(mapper::entityToDto)
                .toList();
        
        Log.info(instances.size() + " EDC-Instanzen gefunden");
        return dtos;
    }

    /**
     * Persists EdcInstance in database if connection to referenced edc is possible.
     * 
     * @param edcInstanceDto contains data for the created EdcInstance
     * @return persisted edcInstance as Dto
     */
    @Transactional
    public EDCInstanceDto create(final EDCInstanceDto edcInstanceDto) throws EntityCreationFailedException{
        Log.debugf("Creation of EdcInstance started.", edcInstanceDto.name());
        final EDCInstance edcInstance = EDCInstanceMapper.mapper.dtoToEntity(edcInstanceDto);
        try{
            edcConnector.tryConnectingInstanceToExistingEdc(edcInstance);
            Log.debugf("Connection tested. EDCInstance is persisted in database");
            edcInstance.persist();
            return EDCInstanceMapper.mapper.entityToDto(edcInstance);
        } catch(ConnectionToEdcFailedException e){
            final String exceptionMessage = "EdcInstance creation failed because of connectionError:" + e.getMessage();
            Log.errorf(exceptionMessage);
            throw new EntityCreationFailedException(exceptionMessage);
        }
    }

    /**
     * Aktualisiert eine bestehende EDC-Instanz.
     * 
     * @param id Die ID der zu aktualisierenden EDC-Instanz
     * @param updatedDto Die EDC-Instanz mit den aktualisierten Daten als DTO
     * @return Die aktualisierte EDC-Instanz als DTO
     * @throws CustomException wenn keine EDC-Instanz mit der angegebenen ID gefunden wurde
     */
    @Transactional
    public EDCInstanceDto update(final UUID id, final EDCInstanceDto updatedDto) throws EntityUpdateFailedException {
        Log.debug("Aktualisieren der EDC-Instanz mit ID: " + id);
        final EDCInstance persistedInstance = EDCInstance.findById(id);
        final EDCInstance updatedInstance = mapper.dtoToEntity(updatedDto);
        if (persistedInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + id + " für Update gefunden";
            Log.error(exceptionMessage);
            throw new EntityUpdateFailedException(exceptionMessage);
        }

        try{
            edcConnector.tryConnectingInstanceToExistingEdc(updatedInstance);
            persistedInstance.setName(updatedInstance.getName());
            persistedInstance.setControlPlaneManagementUrl(updatedInstance.getControlPlaneManagementUrl());
            persistedInstance.setApiKey(updatedInstance.getApiKey());
            persistedInstance.setProtocolVersion(updatedInstance.getProtocolVersion());
            persistedInstance.setDescription(updatedInstance.getDescription());
            persistedInstance.setBpn(updatedInstance.getBpn());
            persistedInstance.setEdcAssetEndpoint(updatedInstance.getEdcAssetEndpoint());
            persistedInstance.setEdcPolicyEndpoint(updatedInstance.getEdcPolicyEndpoint());
            persistedInstance.setEdcContractDefinitionEndpoint(updatedInstance.getEdcContractDefinitionEndpoint());

            Log.info("EDC-Instanz mit ID " + id + " erfolgreich aktualisiert");
            return mapper.entityToDto(persistedInstance);
        } catch(ConnectionToEdcFailedException e) {
            final String exceptionMessage = "EdcInstance update failed because of connectionError:" + e.getMessage();
            Log.errorf(exceptionMessage);
            throw new EntityUpdateFailedException(exceptionMessage);
        }


    }

    /**
     * Löscht eine EDC-Instanz.
     * 
     * @param id Die ID der zu löschenden EDC-Instanz
     * @return true, wenn die EDC-Instanz erfolgreich gelöscht wurde, sonst false
     */
    @Transactional
    public boolean delete(final UUID id) {
        Log.info("Löschen der EDC-Instanz mit ID: " + id);
        
        boolean deleted = EDCInstance.deleteById(id);
        
        if (deleted) {
            Log.info("EDC-Instanz mit ID " + id + " erfolgreich gelöscht");
        } else {
            Log.warn("EDC-Instanz mit ID " + id + " konnte nicht gelöscht werden");
        }
        
        return deleted;
    }
    

}