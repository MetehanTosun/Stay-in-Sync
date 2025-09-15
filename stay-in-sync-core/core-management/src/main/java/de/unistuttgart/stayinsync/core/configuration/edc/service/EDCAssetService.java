package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service-Klasse für die Verwaltung von EDC-Assets.
 * 
 * Diese Klasse bietet Methoden zum Erstellen, Lesen, Aktualisieren und Löschen
 * von EDC-Assets (Eclipse Dataspace Connector).
 */
@ApplicationScoped
public class EDCAssetService {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Findet ein Asset anhand seiner ID.
     *
     * @param id Die ID des zu findenden Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn kein Asset mit der angegebenen ID gefunden wird
     */
    public EDCAssetDto findById(final UUID id) throws CustomException {
        final EDCAsset asset = EDCAsset.findById(id);
        if (asset == null) {
            final String exceptionMessage = "Kein Asset mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
    }

    /**
     * Listet alle Assets auf, die in der Datenbank vorhanden sind.
     *
     * @return Eine Liste aller Assets als DTOs
     */
    public List<EDCAssetDto> listAll() {
        List<EDCAssetDto> assets = new ArrayList<>();
        List<EDCAsset> assetList = EDCAsset.<EDCAsset>listAll();
        for (EDCAsset asset : assetList) {
            assets.add(EDCAssetMapper.assetMapper.assetToAssetDto(asset));
        }
        return assets;
    }

    /**
     * Erstellt ein neues Asset in der Datenbank.
     *
     * @param assetDto Das zu erstellende Asset als DTO
     * @return Das erstellte Asset als DTO
     */
    @Transactional
    public EDCAssetDto create(EDCAssetDto assetDto) {
        EDCAsset asset = EDCAssetMapper.assetMapper.assetDtoToAsset(assetDto);
        asset.persist();
        Log.info("Asset mit ID " + asset.id + " erstellt");
        return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
    }
    
    /**
     * Erstellt ein neues Asset in der Datenbank.
     * Diese Version simuliert die EDC-Integration ohne tatsächlichen EDC-Client.
     * 
     * @param assetDto Das zu erstellende Asset als DTO
     * @return Das erstellte Asset als DTO
     * @throws CustomException Wenn das Asset nicht erstellt werden konnte
     */
    @Transactional
    public EDCAssetDto createInEdcAndDatabase(EDCAssetDto assetDto) throws CustomException {
        // Zuerst das Asset in der Datenbank erstellen
        EDCAsset asset = EDCAssetMapper.assetMapper.assetDtoToAsset(assetDto);
        asset.persist();
        
        // Die EDC-Instanz abrufen
        if (asset.getTargetEDC() == null) {
            final String exceptionMessage = "Keine Ziel-EDC-Instanz für Asset mit ID " + asset.id + " angegeben";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        try {
            // Asset im EDC erstellen (simuliert)
            Log.info("Simuliere Erstellung von Asset im EDC mit ID: " + asset.getAssetId());
            Log.info("Asset gehört zu EDC-Instanz: " + asset.getTargetEDC().id);
            Log.info("Simuliere Erstellung von Policy und Contract-Definition für Asset: " + asset.getAssetId());
            
            return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
        } catch (Exception e) {
            // Bei Fehler das Asset aus der Datenbank löschen und Exception werfen
            EDCAsset.deleteById(asset.id);
            
            final String exceptionMessage = "Fehler beim Erstellen des Assets: " + e.getMessage();
            Log.error(exceptionMessage, e);
            throw new CustomException(exceptionMessage);
        }
    }

    /**
     * Aktualisiert ein bestehendes Asset in der Datenbank.
     *
     * @param id Die ID des zu aktualisierenden Assets
     * @param updatedAssetDto Das aktualisierte Asset als DTO
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn kein Asset mit der angegebenen ID gefunden wird
     */
    @Transactional
    public EDCAssetDto update(UUID id, EDCAssetDto updatedAssetDto) throws CustomException {
        final EDCAsset persistedAsset = EDCAsset.findById(id);
        if (persistedAsset == null) {
            final String exceptionMessage = "Kein Asset mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        final EDCAsset updatedAsset = EDCAssetMapper.assetMapper.assetDtoToAsset(updatedAssetDto);
        
        // Aktualisiere alle Felder des Assets
        persistedAsset.setAssetId(updatedAsset.getAssetId());
        persistedAsset.setUrl(updatedAsset.getUrl());
        persistedAsset.setType(updatedAsset.getType());
        persistedAsset.setContentType(updatedAsset.getContentType());
        persistedAsset.setDescription(updatedAsset.getDescription());
        persistedAsset.setDataAddress(updatedAsset.getDataAddress());
        persistedAsset.setProperties(updatedAsset.getProperties());
        persistedAsset.setTargetSystemEndpoint(updatedAsset.getTargetSystemEndpoint());
        persistedAsset.setTargetEDC(updatedAsset.getTargetEDC());

        Log.info("Asset mit ID " + id + " aktualisiert");
        return EDCAssetMapper.assetMapper.assetToAssetDto(persistedAsset);
    }
    
    /**
     * Aktualisiert ein bestehendes Asset in der Datenbank.
     * Diese Version simuliert die EDC-Integration ohne tatsächlichen EDC-Client.
     *
     * @param id Die ID des zu aktualisierenden Assets
     * @param updatedAssetDto Das aktualisierte Asset als DTO
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn kein Asset mit der angegebenen ID gefunden wird
     */
    @Transactional
    public EDCAssetDto updateInEdcAndDatabase(UUID id, EDCAssetDto updatedAssetDto) throws CustomException {
        // Zuerst in der Datenbank aktualisieren
        final EDCAssetDto updatedAsset = update(id, updatedAssetDto);
        final EDCAsset asset = EDCAsset.findById(id);
        
        try {
            // Asset im EDC aktualisieren (simuliert)
            if (asset.getTargetEDC() != null) {
                Log.info("Simuliere Aktualisierung von Asset im EDC mit ID: " + asset.getAssetId());
                
                // Simuliere Aktualisierung einer Standardrichtlinie und Vertragsdefinition
                Log.info("Simuliere Aktualisierung von Policy und Contract-Definition für Asset: " + asset.getAssetId());
            } else {
                Log.warn("Keine Ziel-EDC-Instanz für Asset mit ID " + asset.id + " angegeben, nur in Datenbank aktualisiert");
            }
            
            return updatedAsset;
        } catch (Exception e) {
            final String exceptionMessage = "Fehler beim Aktualisieren des Assets: " + e.getMessage();
            Log.error(exceptionMessage, e);
            throw new CustomException(exceptionMessage);
        }
    }

    /**
     * Löscht ein Asset aus der Datenbank.
     *
     * @param id Die ID des zu löschenden Assets
     * @return true, wenn das Asset erfolgreich gelöscht wurde, false sonst
     */
    @Transactional
    public boolean delete(final UUID id) {
        boolean deleted = EDCAsset.deleteById(id);
        if (deleted) {
            Log.info("Asset mit ID " + id + " gelöscht");
        } else {
            Log.warn("Asset mit ID " + id + " konnte nicht gelöscht werden");
        }
        return deleted;
    }
    
    /**
     * Löscht ein Asset aus der Datenbank.
     * Diese Version simuliert die EDC-Integration ohne tatsächlichen EDC-Client.
     *
     * @param id Die ID des zu löschenden Assets
     * @return true, wenn das Asset erfolgreich gelöscht wurde, false sonst
     * @throws CustomException Wenn das Löschen fehlschlägt
     */
    @Transactional
    public boolean deleteFromEdcAndDatabase(final UUID id) throws CustomException {
        EDCAsset asset = EDCAsset.findById(id);
        
        if (asset == null) {
            Log.warn("Kein Asset mit ID " + id + " gefunden zum Löschen");
            return false;
        }
        
        if (asset.getTargetEDC() != null) {
            try {
                // Simuliere Löschen im EDC
                Log.info("Simuliere Löschen von Asset im EDC mit ID: " + asset.getAssetId());
                
                // Simuliere Löschen von zugehörigen Policies und Contract Definitions
                Log.info("Simuliere Löschen von Policy und Contract-Definition für Asset: " + asset.getAssetId());
            } catch (Exception e) {
                final String exceptionMessage = "Fehler beim Löschen des Assets: " + e.getMessage();
                Log.error(exceptionMessage, e);
                throw new CustomException(exceptionMessage);
            }
        }
        
        // Löschen des Assets aus der Datenbank
        return delete(id);
    }
    
    /**
     * Findet ein Asset anhand seiner ID und der ID der zugehörigen EDC-Instanz.
     *
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die ID des Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn kein Asset gefunden wird oder die EDC-Instanz nicht existiert
     */
    public EDCAssetDto findByIdAndEdcId(final UUID edcId, final UUID assetId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        // Suche nach dem Asset mit der angegebenen ID in der angegebenen EDC-Instanz
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            final String exceptionMessage = "Kein Asset mit ID " + assetId + " in EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        return EDCAssetMapper.assetMapper.assetToAssetDto(results.get(0));
    }
    
    /**
     * Findet ein Asset anhand seiner ID und der ID der zugehörigen EDC-Instanz.
     *
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die ID des Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn kein Asset gefunden wird oder die EDC-Instanz nicht existiert
     */
    public List<EDCAssetDto> listAllByEdcId(final UUID edcId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Suche nach allen Assets in der angegebenen EDC-Instanz
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> assetList = query.getResultList();
        List<EDCAssetDto> assets = new ArrayList<>();
        
        // Konvertiere alle gefundenen Assets in DTOs
        for (EDCAsset asset : assetList) {
            assets.add(EDCAssetMapper.assetMapper.assetToAssetDto(asset));
        }
        
        // Simuliere den Abruf von Assets direkt vom EDC
        Log.info("Simuliere Abruf von Assets direkt vom EDC für EDC-ID: " + edcId);
        
        return assets;
    }
    
    /**
     * Verarbeitet ein Asset im Frontend-Format und konvertiert es in das interne DTO-Format.
     * Diese Methode extrahiert die relevanten Daten aus dem vom Frontend gelieferten JSON-Format
     * und erstellt ein EDCAssetDto daraus.
     * 
     * @param frontendJson Die JSON-Daten aus dem Frontend als Map
     * @param edcId Die ID der EDC-Instanz
     * @return Ein EDCAssetDto mit den Daten aus dem Frontend
     * @throws CustomException Wenn die Daten fehlen oder die EDC-Instanz nicht existiert
     */
    public EDCAssetDto processFrontendAsset(Map<String, Object> frontendJson, UUID edcId) throws CustomException {
        // Prüfe, ob Daten vorhanden sind
        if (frontendJson == null) {
            final String exceptionMessage = "Keine Daten vom Frontend erhalten";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // ID extrahieren oder generieren
        String assetIdStr = frontendJson.containsKey("@id") ? 
                            (String) frontendJson.get("@id") : 
                            "asset-" + UUID.randomUUID().toString();
        
        // Leere Asset-ID behandeln
        if (assetIdStr == null || assetIdStr.trim().isEmpty()) {
            assetIdStr = "asset-" + UUID.randomUUID().toString();
            Log.info("Leere Asset-ID erhalten, generiere neue: " + assetIdStr);
        }
        
        // Properties extrahieren
        Map<String, String> propertiesMap = new HashMap<>();
        if (frontendJson.containsKey("properties") && frontendJson.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> propMap = (Map<String, Object>) frontendJson.get("properties");
            
            for (Map.Entry<String, Object> entry : propMap.entrySet()) {
                if (entry.getValue() instanceof String) {
                    propertiesMap.put(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() != null) {
                    propertiesMap.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        
        // DataAddress extrahieren und konfigurieren
        EDCDataAddressDto dataAddressDto = new EDCDataAddressDto();
        if (frontendJson.containsKey("dataAddress") && frontendJson.get("dataAddress") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> daMap = (Map<String, Object>) frontendJson.get("dataAddress");
            
            // Typ und URL extrahieren
            if (daMap.containsKey("type") && daMap.get("type") instanceof String) {
                dataAddressDto.setType((String) daMap.get("type"));
            } else {
                dataAddressDto.setType("HttpData");
            }
            
            if (daMap.containsKey("baseUrl") && daMap.get("baseUrl") instanceof String) {
                dataAddressDto.setBaseURL((String) daMap.get("baseUrl"));
            }
            
            // Proxy-Einstellungen konfigurieren
            configureProxySettings(dataAddressDto, daMap);
            
        } else {
            // Standard-DataAddress erstellen
            dataAddressDto.setType("HttpData");
            dataAddressDto.setBaseURL("");
            dataAddressDto.setProxyPath(true);
            dataAddressDto.setProxyQueryParams(true);
        }
        
        // Properties in EDCPropertyDto umwandeln
        EDCPropertyDto propertyDto = new EDCPropertyDto();
        propertyDto.setAdditionalProperties(propertiesMap);
        
        // Sicherstellen, dass wir eine description haben
        String description = extractDescription(frontendJson, propertiesMap);
        
        // Explizit description setzen
        if (!description.isEmpty()) {
            propertyDto.setDescription(description);
        }
        
        // URL und Type aus dataAddress extrahieren
        String url = dataAddressDto.getBaseURL() != null ? dataAddressDto.getBaseURL() : "";
        String type = dataAddressDto.getType() != null ? dataAddressDto.getType() : "HttpData";
        
        // Content-Type aus Properties extrahieren oder Standardwert verwenden
        String contentType = propertiesMap.getOrDefault("asset:prop:contenttype", "application/json");
        
        // Erstellen des DTOs
        EDCAssetDto assetDto = new EDCAssetDto();
        assetDto.setAssetId(assetIdStr);
        assetDto.setUrl(url);
        assetDto.setType(type);
        assetDto.setContentType(contentType);
        assetDto.setDescription(description);
        assetDto.setTargetEDCId(edcId);
        assetDto.setDataAddress(dataAddressDto);
        // Als Liste übergeben statt als einzelnes Objekt
        assetDto.setProperties(Collections.singletonList(propertyDto));
        
        return assetDto;
    }
    
    /**
     * Hilfsmethode zum Konfigurieren der Proxy-Einstellungen für eine DataAddress.
     * 
     * @param dataAddressDto Das zu konfigurierende DataAddressDto
     * @param daMap Die Map mit den Proxy-Einstellungen aus dem Frontend
     */
    private void configureProxySettings(EDCDataAddressDto dataAddressDto, Map<String, Object> daMap) {
        // ProxyPath-Einstellung
        if (daMap.containsKey("proxyPath")) {
            if (daMap.get("proxyPath") instanceof Boolean) {
                dataAddressDto.setProxyPath((Boolean) daMap.get("proxyPath"));
            } else if (daMap.get("proxyPath") instanceof String) {
                dataAddressDto.setProxyPath(Boolean.parseBoolean((String) daMap.get("proxyPath")));
            }
        } else {
            dataAddressDto.setProxyPath(true);
        }
        
        // ProxyQueryParams-Einstellung
        if (daMap.containsKey("proxyQueryParams")) {
            if (daMap.get("proxyQueryParams") instanceof Boolean) {
                dataAddressDto.setProxyQueryParams((Boolean) daMap.get("proxyQueryParams"));
            } else if (daMap.get("proxyQueryParams") instanceof String) {
                dataAddressDto.setProxyQueryParams(Boolean.parseBoolean((String) daMap.get("proxyQueryParams")));
            }
        } else {
            dataAddressDto.setProxyQueryParams(true);
        }
    }
    
    /**
     * Hilfsmethode zum Extrahieren der Beschreibung aus verschiedenen Quellen.
     * 
     * @param frontendJson Die JSON-Daten aus dem Frontend
     * @param propertiesMap Die Properties-Map aus dem Frontend
     * @return Die extrahierte Beschreibung oder einen leeren String
     */
    private String extractDescription(Map<String, Object> frontendJson, Map<String, String> propertiesMap) {
        String description = "";
        
        // Zuerst aus den Properties holen
        if (propertiesMap.containsKey("asset:prop:description")) {
            description = propertiesMap.get("asset:prop:description");
        } 
        // Falls nicht gefunden, direkt aus dem JSON versuchen
        else if (frontendJson.containsKey("description") && frontendJson.get("description") instanceof String) {
            description = (String) frontendJson.get("description");
        }
        
        return description;
    }
    
    /**
     * Erstellt ein Asset für eine bestimmte EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetDto Das zu erstellende Asset
     * @return Das erstellte Asset als DTO
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    @Transactional
    public EDCAssetDto createForEdc(UUID edcId, EDCAssetDto assetDto) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Erstelle eine neue Kopie des DTOs mit dem angegebenen EDC als Ziel
        // Wenn keine ID vorhanden ist, wird eine generiert
        UUID assetId = assetDto.getId() != null ? assetDto.getId() : UUID.randomUUID();
        
        // Verwende die EDC-ID aus dem Pfad, falls targetEDCId nicht im DTO gesetzt ist
        UUID targetEDCId = assetDto.getTargetEDCId() != null ? assetDto.getTargetEDCId() : edcId;
        
        // Stelle sicher, dass die targetEDCId mit der EDC-ID aus dem Pfad übereinstimmt
        if (!targetEDCId.equals(edcId)) {
            final String warningMessage = "targetEDCId im DTO (" + targetEDCId + 
                                        ") stimmt nicht mit der EDC-ID aus dem Pfad (" + 
                                        edcId + ") überein, verwende EDC-ID aus dem Pfad";
            Log.warn(warningMessage);
            targetEDCId = edcId;
        }
        
        // Erstelle ein neues DTO mit der richtigen EDC-ID
        EDCAssetDto newAssetDto = new EDCAssetDto();
        newAssetDto.setId(assetId);
        newAssetDto.setAssetId(assetDto.getAssetId());
        newAssetDto.setUrl(assetDto.getUrl());
        newAssetDto.setType(assetDto.getType());
        newAssetDto.setContentType(assetDto.getContentType());
        newAssetDto.setDescription(assetDto.getDescription());
        newAssetDto.setTargetEDCId(targetEDCId);
        newAssetDto.setDataAddress(assetDto.getDataAddress());
        newAssetDto.setProperties(assetDto.getProperties());
        
        // Debug-Ausgabe für die Fehlersuche
        Log.info("Creating asset with ID: " + assetId + " for EDC: " + targetEDCId);
        
        try {
            // Asset in Datenbank erstellen
            return createInEdcAndDatabase(newAssetDto);
        } catch (Exception e) {
            final String exceptionMessage = "Fehler beim Erstellen des Assets für EDC " + 
                                        edcId + ": " + e.getMessage();
            Log.error(exceptionMessage, e);
            throw new CustomException(exceptionMessage);
        }
    }
    
    /**
     * Aktualisiert ein Asset für eine bestimmte EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die ID des Assets
     * @param updatedAssetDto Das aktualisierte Asset
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn die EDC-Instanz oder das Asset nicht existieren
     */
    @Transactional
    public EDCAssetDto updateForEdc(UUID edcId, UUID assetId, EDCAssetDto updatedAssetDto) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Suche nach dem Asset mit der angegebenen ID in der angegebenen EDC-Instanz
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            final String exceptionMessage = "Kein Asset mit ID " + assetId + 
                                        " in EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Erstelle ein neues DTO mit den aktualisierten Daten
        // Stelle sicher, dass die IDs und EDC-ID erhalten bleiben
        EDCAssetDto newUpdatedAssetDto = new EDCAssetDto();
        newUpdatedAssetDto.setId(assetId);
        newUpdatedAssetDto.setAssetId(updatedAssetDto.getAssetId());
        newUpdatedAssetDto.setUrl(updatedAssetDto.getUrl());
        newUpdatedAssetDto.setType(updatedAssetDto.getType());
        newUpdatedAssetDto.setContentType(updatedAssetDto.getContentType());
        newUpdatedAssetDto.setDescription(updatedAssetDto.getDescription());
        newUpdatedAssetDto.setTargetEDCId(edcId);
        newUpdatedAssetDto.setDataAddress(updatedAssetDto.getDataAddress());
        newUpdatedAssetDto.setProperties(updatedAssetDto.getProperties());
        
        // Asset in Datenbank aktualisieren
        return updateInEdcAndDatabase(assetId, newUpdatedAssetDto);
    }
    
    /**
     * Löscht ein Asset aus einer bestimmten EDC-Instanz basierend auf der Asset-String-ID.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die String-ID des Assets (ODRL @id)
     * @return true, wenn das Asset gelöscht wurde, false sonst
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    @Transactional
    public boolean deleteFromEdcByStringId(UUID edcId, String assetId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Suche nach dem Asset mit der angegebenen String-ID in der angegebenen EDC-Instanz
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.assetId = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            final String warningMessage = "Kein Asset mit String-ID " + assetId + 
                                        " in EDC-Instanz mit ID " + edcId + " gefunden";
            Log.warn(warningMessage);
            return false;
        }
        
        // Asset löschen
        return deleteFromEdcAndDatabase(results.get(0).id);
    }

    /**
     * Löscht ein Asset aus einer bestimmten EDC-Instanz basierend auf der Asset-UUID.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die UUID des Assets
     * @return true, wenn das Asset gelöscht wurde, false sonst
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    @Transactional
    public boolean deleteFromEdc(UUID edcId, UUID assetId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "Keine EDC-Instanz mit ID " + edcId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Suche nach dem Asset mit der angegebenen ID in der angegebenen EDC-Instanz
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            final String warningMessage = "Kein Asset mit ID " + assetId + 
                                        " in EDC-Instanz mit ID " + edcId + " gefunden";
            Log.warn(warningMessage);
            return false;
        }
        
        // Asset löschen
        return deleteFromEdcAndDatabase(assetId);
    }
    
    /**
     * Simuliert einen Datentransfer für ein Asset.
     * 
     * @param assetId Die ID des zu übertragenden Assets in der Datenbank
     * @param destinationUrl Die URL, an die die Daten übertragen werden sollen
     * @return Die Transfer-Prozess-ID als String
     * @throws CustomException Wenn der Datentransfer nicht initiiert werden kann
     */
    @Transactional
    public String initiateDataTransfer(UUID assetId, String destinationUrl) throws CustomException {
        EDCAsset asset = EDCAsset.findById(assetId);
        
        if (asset == null) {
            final String exceptionMessage = "Kein Asset mit ID " + assetId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        if (asset.getTargetEDC() == null) {
            final String exceptionMessage = "Keine Ziel-EDC-Instanz für Asset mit ID " + assetId + " angegeben";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        try {
            // Simuliere Datentransfer-Initiierung
            String transferProcessId = "transfer-" + UUID.randomUUID().toString();
            Log.info("Simuliere Datentransfer für Asset: " + asset.getAssetId() + " zu URL: " + destinationUrl);
            Log.info("Simulierter Datentransfer initiiert mit Transfer-Prozess-ID: " + transferProcessId);
            
            return transferProcessId;
        } catch (Exception e) {
            final String exceptionMessage = "Fehler beim Initiieren des Datentransfers: " + e.getMessage();
            Log.error(exceptionMessage, e);
            throw new CustomException(exceptionMessage);
        }
    }
    
    /**
     * Simuliert die Überprüfung des Status eines Datentransfer-Prozesses.
     *
     * @param assetId Die ID des Assets in der Datenbank
     * @param transferProcessId Die ID des zu überprüfenden Transfer-Prozesses
     * @return Der aktuelle Status des Transfer-Prozesses als String
     * @throws CustomException Wenn der Status nicht abgerufen werden kann
     */
    public String checkTransferStatus(UUID assetId, String transferProcessId) throws CustomException {
        EDCAsset asset = EDCAsset.findById(assetId);
        
        if (asset == null) {
            final String exceptionMessage = "Kein Asset mit ID " + assetId + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        if (asset.getTargetEDC() == null) {
            final String exceptionMessage = "Keine Ziel-EDC-Instanz für Asset mit ID " + assetId + " angegeben";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        try {
            // Simuliere Status-Überprüfung
            Log.info("Simuliere Statusüberprüfung für Transfer-Prozess: " + transferProcessId);
            
            // Für Testzwecke immer COMPLETED zurückgeben
            String status = "COMPLETED";
            Log.info("Simulierter Transfer-Status für Prozess " + transferProcessId + ": " + status);
            return status;
        } catch (Exception e) {
            final String exceptionMessage = "Fehler beim Überprüfen des Transfer-Status: " + e.getMessage();
            Log.error(exceptionMessage, e);
            throw new CustomException(exceptionMessage);
        }
    }
}
