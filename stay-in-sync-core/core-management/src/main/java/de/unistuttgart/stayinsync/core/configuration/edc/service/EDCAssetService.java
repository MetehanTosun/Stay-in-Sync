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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class EDCAssetService {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Returns an asset found in the database with the id.
     *
     * @param id used to find the asset
     * @return found asset
     */
    public EDCAssetDto findById(final UUID id) throws CustomException {
        final EDCAsset asset = EDCAsset.findById(id);
        if (asset == null) {
            final String exceptionMessage = "No Asset found with id";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
    }

    /**
     * Returns all assets that are currently in the database.
     *
     * @return List with all assets
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
     * Moves asset into the database and returns it to the caller.
     *
     * @param assetDto to be persisted.
     * @return the created asset.
     */
    @Transactional
    public EDCAssetDto create(EDCAssetDto assetDto) {
        EDCAsset asset = EDCAssetMapper.assetMapper.assetDtoToAsset(assetDto);
        asset.persist();
        return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
     *
     * @param id              to find the database entry
     * @param updatedAssetDto contains the updated data
     * @return the updated asset
     */
    @Transactional
    public EDCAssetDto update(UUID id, EDCAssetDto updatedAssetDto) throws CustomException {
        final EDCAsset persistedAsset = EDCAsset.findById(id);
        final EDCAsset updatedAsset = EDCAssetMapper.assetMapper.assetDtoToAsset(updatedAssetDto);

        if (persistedAsset == null) {
            final String exceptionMessage = "No Asset found with id";
            Log.errorf(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        persistedAsset.setAssetId(updatedAsset.getAssetId());
        persistedAsset.setUrl(updatedAsset.getUrl());
        persistedAsset.setType(updatedAsset.getType());
        persistedAsset.setContentType(updatedAsset.getContentType());
        persistedAsset.setDescription(updatedAsset.getDescription());
        persistedAsset.setDataAddress(updatedAsset.getDataAddress());
        persistedAsset.setProperties(updatedAsset.getProperties());
        persistedAsset.setTargetSystemEndpoint(updatedAsset.getTargetSystemEndpoint());
        persistedAsset.setTargetEDC(updatedAsset.getTargetEDC());

        return EDCAssetMapper.assetMapper.assetToAssetDto(persistedAsset);
    }

    /**
     * Removes the asset from the database
     *
     * @param id used to find asset to be deleted in database
     * @return true if deletion was successful, false otherwise
     */
    @Transactional
    public boolean delete(final UUID id) {
        return EDCAsset.deleteById(id);
    }
    
    /**
     * Returns an asset found in the database with the id and belonging to the specific EDC instance.
     *
     * @param edcId used to find the EDC instance
     * @param assetId used to find the asset
     * @return found asset
     */
    public EDCAssetDto findByIdAndEdcId(final UUID edcId, final UUID assetId) throws CustomException {
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            final String exceptionMessage = "No Asset found with id " + assetId + " for EDC instance " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        return EDCAssetMapper.assetMapper.assetToAssetDto(results.get(0));
    }
    
    /**
     * Returns all assets that are associated with a specific EDC instance.
     *
     * @param edcId the UUID of the EDC instance
     * @return List with all assets for that EDC instance
     */
    public List<EDCAssetDto> listAllByEdcId(final UUID edcId) throws CustomException {
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> assetList = query.getResultList();
        List<EDCAssetDto> assets = new ArrayList<>();
        
        for (EDCAsset asset : assetList) {
            assets.add(EDCAssetMapper.assetMapper.assetToAssetDto(asset));
        }
        return assets;
    }
    
    /**
     * Hilfsmethode zur Verarbeitung eines JSON-Assets im Frontend-Format.
     * Konvertiert ein Asset im Frontend-Format in das interne DTO-Format.
     * 
     * @param frontendJson Die JSON-Daten aus dem Frontend
     * @param edcId Die ID der EDC-Instanz
     * @return Ein EDCAssetDto mit den Daten aus dem Frontend
     */
    public EDCAssetDto processFrontendAsset(Map<String, Object> frontendJson, UUID edcId) throws CustomException {
        if (frontendJson == null) {
            throw new CustomException("Asset data is missing");
        }
        
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
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
        }
        
        // Properties extrahieren
        Map<String, String> propertiesMap = new HashMap<>();
        if (frontendJson.containsKey("properties") && frontendJson.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> propMap = (Map<String, Object>) frontendJson.get("properties");
            for (Map.Entry<String, Object> entry : propMap.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().toString().trim().isEmpty()) {
                    propertiesMap.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        
        // DataAddress extrahieren
        EDCDataAddressDto dataAddressDto = new EDCDataAddressDto();
        if (frontendJson.containsKey("dataAddress") && frontendJson.get("dataAddress") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> daMap = (Map<String, Object>) frontendJson.get("dataAddress");
            
            if (daMap.containsKey("type")) {
                dataAddressDto.setType(daMap.get("type").toString());
            } else {
                dataAddressDto.setType("HttpData"); // Standardwert
            }
            
            // Unterstütze verschiedene Varianten von base_url/baseURL/baseUrl
            String baseUrl = null;
            if (daMap.containsKey("base_url")) {
                baseUrl = daMap.get("base_url").toString();
            } else if (daMap.containsKey("baseURL")) {
                baseUrl = daMap.get("baseURL").toString();
            } else if (daMap.containsKey("baseUrl")) {
                baseUrl = daMap.get("baseUrl").toString();
            }
            
            if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                dataAddressDto.setBaseURL(baseUrl);
            } else {
                // Fallback: URL aus dem Haupt-Asset verwenden
                if (frontendJson.containsKey("url")) {
                    String fallbackUrl = frontendJson.get("url").toString();
                    if (fallbackUrl != null && !fallbackUrl.trim().isEmpty()) {
                        dataAddressDto.setBaseURL(fallbackUrl);
                    } else {
                        // Letzter Fallback: Beispiel-URL
                        dataAddressDto.setBaseURL("https://example.com/api");
                    }
                } else {
                    // Letzter Fallback: Beispiel-URL
                    dataAddressDto.setBaseURL("https://example.com/api");
                }
            }
            
            // Proxy-Einstellungen
            if (daMap.containsKey("proxyPath")) {
                Object proxyPath = daMap.get("proxyPath");
                if (proxyPath instanceof Boolean) {
                    dataAddressDto.setProxyPath((Boolean) proxyPath);
                } else if (proxyPath != null) {
                    dataAddressDto.setProxyPath(Boolean.parseBoolean(proxyPath.toString()));
                }
            } else {
                dataAddressDto.setProxyPath(true); // Standardwert
            }
            
            if (daMap.containsKey("proxyQueryParams")) {
                Object proxyQueryParams = daMap.get("proxyQueryParams");
                if (proxyQueryParams instanceof Boolean) {
                    dataAddressDto.setProxyQueryParams((Boolean) proxyQueryParams);
                } else if (proxyQueryParams != null) {
                    dataAddressDto.setProxyQueryParams(Boolean.parseBoolean(proxyQueryParams.toString()));
                }
            } else {
                dataAddressDto.setProxyQueryParams(true); // Standardwert
            }
            
            dataAddressDto.setJsonLDType("DataAddress");
        } else {
            // Fallback: DataAddress mit Standardwerten erstellen
            dataAddressDto.setType("HttpData");
            dataAddressDto.setJsonLDType("DataAddress");
            dataAddressDto.setProxyPath(true);
            dataAddressDto.setProxyQueryParams(true);
            
            // URL aus dem Haupt-Asset verwenden
            if (frontendJson.containsKey("url")) {
                dataAddressDto.setBaseURL(frontendJson.get("url").toString());
            }
        }
        
        // Properties in EDCPropertyDto umwandeln
        EDCPropertyDto propertyDto = new EDCPropertyDto();
        propertyDto.setAdditionalProperties(propertiesMap);
        
        // Sicherstellen, dass wir eine description haben
        String description = "";
        if (propertiesMap.containsKey("asset:prop:description")) {
            description = propertiesMap.get("asset:prop:description");
        } else if (frontendJson.containsKey("description")) {
            description = frontendJson.get("description").toString();
            // Auch in Properties hinzufügen
            propertiesMap.put("asset:prop:description", description);
            propertyDto.setAdditionalProperties(propertiesMap);
        }
        
        // Explizit description setzen
        if (!description.isEmpty()) {
            propertyDto.setDescription(description);
        }
        
        // URL und Type aus dataAddress extrahieren
        String url = dataAddressDto.getBaseURL() != null ? dataAddressDto.getBaseURL() : "";
        String type = dataAddressDto.getType() != null ? dataAddressDto.getType() : "HttpData";
        
        // Content-Type aus Properties extrahieren oder Standardwert verwenden
        String contentType = propertiesMap.getOrDefault("asset:prop:contenttype", "application/json");
        
        // Beschreibung ist bereits oben definiert, verwende sie hier
        
        // Erstellen des DTOs
        return new EDCAssetDto(
            null, // ID wird automatisch generiert
            assetIdStr,
            url,
            type,
            contentType,
            description,
            edcId,
            dataAddressDto,
            propertyDto
        );
    }
    
    /**
     * Erstellt ein Asset für eine bestimmte EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetDto Das zu erstellende Asset
     * @return Das erstellte Asset
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    @Transactional
    public EDCAssetDto createForEdc(UUID edcId, EDCAssetDto assetDto) throws CustomException {
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Erstelle eine neue Kopie des DTOs mit dem angegebenen EDC als Ziel
        // Wenn keine ID vorhanden ist, wird eine generiert
        UUID assetId = assetDto.id() != null ? assetDto.id() : UUID.randomUUID();
        
        // Verwende die EDC-ID aus dem Pfad, falls targetEDCId nicht im DTO gesetzt ist
        UUID targetEDCId = assetDto.targetEDCId() != null ? assetDto.targetEDCId() : edcId;
        
        // Stelle sicher, dass die targetEDCId mit der EDC-ID aus dem Pfad übereinstimmt
        if (!targetEDCId.equals(edcId)) {
            Log.warn("Provided targetEDCId " + targetEDCId + " does not match path EDC ID " + edcId + ". Using path EDC ID.");
            targetEDCId = edcId;
        }
        
        // Ensure the targetEDCId matches the specified EDC instance
        EDCAssetDto newAssetDto = new EDCAssetDto(
            assetId,
            assetDto.assetId(),
            assetDto.url(),
            assetDto.type(),
            assetDto.contentType(),
            assetDto.description(),
            targetEDCId,  // Set the targetEDCId to the specified EDC instance
            assetDto.dataAddress(),
            assetDto.properties()
        );
        
        // Debug-Ausgabe für die Fehlersuche
        Log.info("Creating asset with ID: " + assetId + " for EDC: " + targetEDCId);
        
        try {
            EDCAsset asset = EDCAssetMapper.assetMapper.assetDtoToAsset(newAssetDto);
            asset.persist();
            return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
        } catch (Exception e) {
            Log.error("Error creating asset: " + e.getMessage(), e);
            throw new CustomException("Failed to create asset: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert ein Asset für eine bestimmte EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die ID des Assets
     * @param updatedAssetDto Das aktualisierte Asset
     * @return Das aktualisierte Asset
     * @throws CustomException Wenn die EDC-Instanz oder das Asset nicht existieren
     */
    @Transactional
    public EDCAssetDto updateForEdc(UUID edcId, UUID assetId, EDCAssetDto updatedAssetDto) throws CustomException {
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            final String exceptionMessage = "No Asset found with id " + assetId + " for EDC instance " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        EDCAsset persistedAsset = results.get(0);
        
        // Erstelle ein neues DTO mit den aktualisierten Daten
        // Stelle sicher, dass die IDs und EDC-ID erhalten bleiben
        EDCAssetDto newUpdatedAssetDto = new EDCAssetDto(
            assetId,
            updatedAssetDto.assetId(),
            updatedAssetDto.url(),
            updatedAssetDto.type(),
            updatedAssetDto.contentType(),
            updatedAssetDto.description(),
            edcId,  // Stelle sicher, dass die EDC-ID beibehalten wird
            updatedAssetDto.dataAddress(),
            updatedAssetDto.properties()
        );
        
        final EDCAsset updatedAsset = EDCAssetMapper.assetMapper.assetDtoToAsset(newUpdatedAssetDto);
        
        persistedAsset.setAssetId(updatedAsset.getAssetId());
        persistedAsset.setUrl(updatedAsset.getUrl());
        persistedAsset.setType(updatedAsset.getType());
        persistedAsset.setContentType(updatedAsset.getContentType());
        persistedAsset.setDescription(updatedAsset.getDescription());
        persistedAsset.setDataAddress(updatedAsset.getDataAddress());
        persistedAsset.setProperties(updatedAsset.getProperties());
        
        return EDCAssetMapper.assetMapper.assetToAssetDto(persistedAsset);
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
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.assetId = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            Log.warn("No asset found with assetId " + assetId + " for EDC " + edcId);
            return false;
        }
        
        EDCAsset asset = results.get(0);
        asset.delete();
        Log.info("Successfully deleted asset with assetId " + assetId + " for EDC " + edcId);
        return true;
    }

    /**
     * Löscht ein Asset aus einer bestimmten EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die ID des Assets
     * @return true, wenn das Asset gelöscht wurde, false sonst
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    @Transactional
    public boolean deleteFromEdc(UUID edcId, UUID assetId) throws CustomException {
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            return false;
        }
        
        EDCAsset asset = results.get(0);
        asset.delete();
        return true;
    }
}
