package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
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
        return mapper.toDto(instance);
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
                .map(mapper::toDto)
                .toList();
        
        Log.info(instances.size() + " EDC-Instanzen gefunden");
        return dtos;
    }

    /**
     * Erstellt eine neue EDC-Instanz in der Datenbank.
     * 
     * @param dto Die zu erstellende EDC-Instanz als DTO
     * @return Die erstellte EDC-Instanz als DTO
     */
    @Transactional
    public EDCInstanceDto create(final EDCInstanceDto dto) {
        Log.info("Erstellen einer neuen EDC-Instanz: " + dto.getName());
        
        EDCInstance instance = mapper.fromDto(dto);
        instance.persist();
        
        Log.info("EDC-Instanz erfolgreich erstellt mit ID: " + instance.id);
        return mapper.toDto(instance);
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
    public EDCInstanceDto update(final UUID id, final EDCInstanceDto updatedDto) throws CustomException {
        Log.info("Aktualisieren der EDC-Instanz mit ID: " + id);
        
        final EDCInstance persistedInstance = EDCInstance.findById(id);
        final EDCInstance updatedInstance = mapper.fromDto(updatedDto);
        
        if (persistedInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + id + " für Update gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        persistedInstance.setName(updatedInstance.getName());
        persistedInstance.setUrl(updatedInstance.getUrl());
        persistedInstance.setApiKey(updatedInstance.getApiKey());
        persistedInstance.setProtocolVersion(updatedInstance.getProtocolVersion());
        persistedInstance.setDescription(updatedInstance.getDescription());
        persistedInstance.setBpn(updatedInstance.getBpn());
        
        Log.info("EDC-Instanz mit ID " + id + " erfolgreich aktualisiert");
        return mapper.toDto(persistedInstance);
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
    
    /**
     * Hilfsmethode zum Abrufen einer EDC-Instanz ohne DTO-Konvertierung.
     * 
     * @param id Die ID der zu findenden EDC-Instanz
     * @return Die gefundene EDC-Instanz
     * @throws CustomException wenn keine EDC-Instanz mit der angegebenen ID gefunden wurde
     */
    public EDCInstance getEntityById(final UUID id) throws CustomException {
        Log.info("Abrufen der EDC-Instanz-Entität mit ID: " + id);
        
        final EDCInstance instance = EDCInstance.findById(id);
        if (instance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        return instance;
    }
    
    /**
     * Erstellt eine EDC-Instanz direkt aus einer Entity.
     * Wird intern verwendet.
     *
     * @param entity Die zu erstellende EDC-Instanz-Entity
     * @return Die erstellte EDC-Instanz-Entity
     */
    @Transactional
    public EDCInstance createEntity(final EDCInstance entity) {
        Log.info("Erstellen einer neuen EDC-Instanz-Entity: " + entity.getName());
        
        entity.persist();
        
        Log.info("EDC-Instanz-Entity erfolgreich erstellt mit ID: " + entity.id);
        return entity;
    }
}
