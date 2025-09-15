package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCPropertyMapper;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import io.quarkus.logging.Log;

/**
 * Service-Klasse für die Verwaltung von EDC-Properties.
 * 
 * Diese Klasse bietet Methoden zum Erstellen, Lesen, Aktualisieren und Löschen
 * von EDC-Properties. Properties sind zusätzliche Metadaten für Assets im
 * Eclipse Dataspace Connector (EDC), wie Name, Version, Content-Type und
 * Beschreibung.
 */
@ApplicationScoped
public class EDCPropertyService {

    /**
     * Findet ein EDCProperty anhand seiner ID.
     * 
     * @param id Die ID des zu findenden EDCProperty
     * @return Das gefundene EDCProperty als DTO
     * @throws CustomException Wenn kein EDCProperty mit der angegebenen ID gefunden wird
     */
    public EDCPropertyDto findById(final UUID id) throws CustomException {
        final EDCProperty property = EDCProperty.findById(id);
        if (property == null) {
            final String exceptionMessage = "Keine Property mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        return EDCPropertyMapper.toDto(property);
    }

    /**
     * Listet alle EDCProperties auf, die in der Datenbank vorhanden sind.
     * 
     * @return Eine Liste aller EDCProperties als DTOs
     */
    public List<EDCPropertyDto> listAll() {
        List<EDCPropertyDto> properties = new ArrayList<>();
        List<EDCProperty> propertyList = EDCProperty.<EDCProperty>listAll();
        for (EDCProperty property : propertyList) {
            properties.add(EDCPropertyMapper.toDto(property));
        }
        return properties;
    }

    /**
     * Erstellt ein neues EDCProperty in der Datenbank.
     * 
     * @param propertyDto Das zu erstellende EDCProperty als DTO
     * @return Das erstellte EDCProperty als DTO
     */
    @Transactional
    public EDCPropertyDto create(final EDCPropertyDto propertyDto) {
        EDCProperty property = EDCPropertyMapper.fromDto(propertyDto);
        property.persist();
        Log.info("EDCProperty mit ID " + property.id + " erstellt");
        return EDCPropertyMapper.toDto(property);
    }

    /**
     * Aktualisiert ein bestehendes EDCProperty in der Datenbank.
     * 
     * @param id Die ID des zu aktualisierenden EDCProperty
     * @param updatedPropertyDto Das aktualisierte EDCProperty als DTO
     * @return Das aktualisierte EDCProperty als DTO
     * @throws CustomException Wenn kein EDCProperty mit der angegebenen ID gefunden wird
     */
    @Transactional
    public EDCPropertyDto update(final UUID id, final EDCPropertyDto updatedPropertyDto) throws CustomException {
        final EDCProperty persistedProperty = EDCProperty.findById(id);
        if (persistedProperty == null) {
            final String exceptionMessage = "Keine Property mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        final EDCProperty updatedProperty = EDCPropertyMapper.fromDto(updatedPropertyDto);
        
        // Aktualisiere alle Felder der Property
        persistedProperty.setName(updatedProperty.getName());
        persistedProperty.setVersion(updatedProperty.getVersion());
        persistedProperty.setContentType(updatedProperty.getContentType());
        persistedProperty.setDescription(updatedProperty.getDescription());
        
        Log.info("EDCProperty mit ID " + id + " aktualisiert");
        return EDCPropertyMapper.toDto(persistedProperty);
    }

    /**
     * Löscht ein EDCProperty aus der Datenbank.
     * 
     * @param id Die ID des zu löschenden EDCProperty
     * @return true, wenn das EDCProperty erfolgreich gelöscht wurde, false sonst
     * @throws CustomException Wenn kein EDCProperty mit der angegebenen ID gefunden wird
     */
    @Transactional
    public boolean delete(final UUID id) throws CustomException {
        final EDCProperty property = EDCProperty.findById(id);
        if (property == null) {
            final String exceptionMessage = "Keine Property mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        boolean deleted = EDCProperty.deleteById(id);
        if (deleted) {
            Log.info("EDCProperty mit ID " + id + " gelöscht");
        } else {
            Log.warn("EDCProperty mit ID " + id + " konnte nicht gelöscht werden");
        }
        return deleted;
    }
}
