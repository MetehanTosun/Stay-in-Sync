package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCDataAddress;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCDataAddressMapper;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import io.quarkus.logging.Log;

/**
 * Service-Klasse für die Verwaltung von EDC-DataAddresses.
 * 
 * Diese Klasse bietet Methoden zum Erstellen, Lesen, Aktualisieren und Löschen
 * von EDC-DataAddresses. DataAddresses definieren, wie auf Assets im
 * Eclipse Dataspace Connector (EDC) zugegriffen werden kann und enthalten
 * Informationen wie die Basis-URL und Proxy-Einstellungen.
 */
@ApplicationScoped
public class EDCDataAddressService {

    /**
     * Findet eine EDCDataAddress anhand ihrer ID.
     * 
     * @param id Die ID der zu findenden EDCDataAddress
     * @return Die gefundene EDCDataAddress als DTO
     * @throws CustomException Wenn keine EDCDataAddress mit der angegebenen ID gefunden wird
     */
    public EDCDataAddressDto findById(final UUID id) throws CustomException {
        final EDCDataAddress dataAddress = EDCDataAddress.findById(id);
        if (dataAddress == null) {
            final String exceptionMessage = "Keine DataAddress mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        return EDCDataAddressMapper.toDto(dataAddress);
    }

    /**
     * Listet alle EDCDataAddresses auf, die in der Datenbank vorhanden sind.
     * 
     * @return Eine Liste aller EDCDataAddresses als DTOs
     */
    public List<EDCDataAddressDto> listAll() {
        List<EDCDataAddressDto> dataAddresses = new ArrayList<>();
        List<EDCDataAddress> dataAddressList = EDCDataAddress.<EDCDataAddress>listAll();
        for (EDCDataAddress dataAddress : dataAddressList) {
            dataAddresses.add(EDCDataAddressMapper.toDto(dataAddress));
        }
        return dataAddresses;
    }

    /**
     * Erstellt eine neue EDCDataAddress in der Datenbank.
     * 
     * @param dataAddressDto Die zu erstellende EDCDataAddress als DTO
     * @return Die erstellte EDCDataAddress als DTO
     */
    @Transactional
    public EDCDataAddressDto create(final EDCDataAddressDto dataAddressDto) {
        EDCDataAddress dataAddress = EDCDataAddressMapper.fromDto(dataAddressDto);
        dataAddress.persist();
        Log.info("EDCDataAddress mit ID " + dataAddress.id + " erstellt");
        return EDCDataAddressMapper.toDto(dataAddress);
    }

    /**
     * Aktualisiert eine bestehende EDCDataAddress in der Datenbank.
     * 
     * @param id Die ID der zu aktualisierenden EDCDataAddress
     * @param updatedDataAddressDto Die aktualisierte EDCDataAddress als DTO
     * @return Die aktualisierte EDCDataAddress als DTO
     * @throws CustomException Wenn keine EDCDataAddress mit der angegebenen ID gefunden wird
     */
    @Transactional
    public EDCDataAddressDto update(final UUID id, final EDCDataAddressDto updatedDataAddressDto) throws CustomException {
        final EDCDataAddress persistedDataAddress = EDCDataAddress.findById(id);
        if (persistedDataAddress == null) {
            final String exceptionMessage = "Keine DataAddress mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        final EDCDataAddress updatedDataAddress = EDCDataAddressMapper.fromDto(updatedDataAddressDto);
        
        // Aktualisiere alle Felder der DataAddress
        persistedDataAddress.setJsonLDType(updatedDataAddress.getJsonLDType());
        persistedDataAddress.setType(updatedDataAddress.getType());
        persistedDataAddress.setBaseUrl(updatedDataAddress.getBaseUrl());
        persistedDataAddress.setProxyPath(updatedDataAddress.isProxyPath());
        persistedDataAddress.setProxyQueryParams(updatedDataAddress.isProxyQueryParams());
        
        Log.info("EDCDataAddress mit ID " + id + " aktualisiert");
        return EDCDataAddressMapper.toDto(persistedDataAddress);
    }

    /**
     * Löscht eine EDCDataAddress aus der Datenbank.
     * 
     * @param id Die ID der zu löschenden EDCDataAddress
     * @return true, wenn die EDCDataAddress erfolgreich gelöscht wurde, false sonst
     * @throws CustomException Wenn keine EDCDataAddress mit der angegebenen ID gefunden wird
     */
    @Transactional
    public boolean delete(final UUID id) throws CustomException {
        final EDCDataAddress dataAddress = EDCDataAddress.findById(id);
        if (dataAddress == null) {
            final String exceptionMessage = "Keine DataAddress mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        boolean deleted = EDCDataAddress.deleteById(id);
        if (deleted) {
            Log.info("EDCDataAddress mit ID " + id + " gelöscht");
        } else {
            Log.warn("EDCDataAddress mit ID " + id + " konnte nicht gelöscht werden");
        }
        return deleted;
    }
}
