package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EdcInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.CustomException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.EntityCreationFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.EntityUpdateFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCInstanceMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

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
    public EDCInstanceDto findById(final Long id) throws CustomException {
        Log.info("Suche nach EDC-Instanz mit ID: " + id);
        
        final EdcInstance instance = EdcInstance.findById(id);

        if (instance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        Log.info("EDC-Instanz gefunden: " + instance.name);
        return mapper.toDto(instance);
    }

    /**
     * Gibt eine Liste aller EDC-Instanzen zurück.
     *
     * @return Liste aller EDC-Instanzen als DTOs
     */
    public List<EDCInstanceDto> listAll() {
        Log.info("Abrufen aller EDC-Instanzen");

        List<EdcInstance> instances = EdcInstance.listAll();
        List<EDCInstanceDto> dtos = instances.stream()
                .map(mapper::toDto)
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
    public EDCInstanceDto create(final EDCInstanceDto edcInstanceDto) throws EntityCreationFailedException {
        Log.debugf("Creation of EdcInstance started.", edcInstanceDto.name());
        final EdcInstance edcInstance = mapper.fromDto(edcInstanceDto);
        Log.debugf("Connection tested. EDCInstance is persisted in database");
        edcInstance.persist();
        return mapper.toDto(edcInstance);

    }

    /**
     * Aktualisiert eine bestehende EDC-Instanz.
     *
     * @param id         Die ID der zu aktualisierenden EDC-Instanz
     * @param updatedDto Die EDC-Instanz mit den aktualisierten Daten als DTO
     * @return Die aktualisierte EDC-Instanz als DTO
     * @throws CustomException wenn keine EDC-Instanz mit der angegebenen ID gefunden wurde
     */
    @Transactional
    public EDCInstanceDto update(final Long id, final EDCInstanceDto updatedDto) throws EntityUpdateFailedException {
        Log.debug("Aktualisieren der EDC-Instanz mit ID: " + id);
        
        final EdcInstance persistedInstance = EdcInstance.findById(id);
        
        if (persistedInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + id + " für Update gefunden";
            Log.error(exceptionMessage);
            throw new EntityUpdateFailedException(exceptionMessage);
        }
        
        // Der Mapper wird verwendet, um die Entity direkt zu aktualisieren
        mapper.updateEntityFromDto(updatedDto, persistedInstance);

        Log.info("EDC-Instanz mit ID " + id + " erfolgreich aktualisiert");
        return mapper.toDto(persistedInstance);


    }

    /**
     * Löscht eine EDC-Instanz.
     *
     * @param id Die ID der zu löschenden EDC-Instanz
     * @return true, wenn die EDC-Instanz erfolgreich gelöscht wurde, sonst false
     * @throws CustomException wenn beim Löschen ein Fehler auftritt
     */
    @Transactional
    public boolean delete(final Long id) throws CustomException {
        Log.info("Löschen der EDC-Instanz mit ID: " + id);
        
        try {
            boolean deleted = EdcInstance.deleteById(id);

            if (deleted) {
                Log.info("EDC-Instanz mit ID " + id + " erfolgreich gelöscht");
            } else {
                Log.warn("EDC-Instanz mit ID " + id + " konnte nicht gelöscht werden");
            }

            return deleted;
        } catch (Exception e) {
            final String exceptionMessage = "Fehler beim Löschen der EDC-Instanz mit ID " + id + ": " + e.getMessage();
            Log.error(exceptionMessage, e);
            throw new CustomException(exceptionMessage);
        }
    }


}