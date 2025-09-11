package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.client.EDCClient;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service-Klasse für die Verwaltung von EDC-Assets.
 * 
 * Bietet Methoden zum Erstellen, Lesen, Aktualisieren und Löschen von Assets,
 * sowie zur Konvertierung zwischen verschiedenen Asset-Formaten.
 */
@ApplicationScoped
public class EDCAssetService {

    @PersistenceContext
    EntityManager entityManager;
    
    @Inject
    EDCClient edcClient;

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
            final String exceptionMessage = "No Asset found with id";
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
        return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
    }
    
    /**
     * Erstellt ein neues Asset in der Datenbank und im EDC.
     * 
     * @param assetDto Das zu erstellende Asset als DTO
     * @return Das erstellte Asset als DTO
     * @throws CustomException Wenn das Asset nicht im EDC erstellt werden konnte
     */
    @Transactional
    public EDCAssetDto createInEdcAndDatabase(EDCAssetDto assetDto) throws CustomException {
        // Zuerst das Asset in der Datenbank erstellen
        EDCAsset asset = EDCAssetMapper.assetMapper.assetDtoToAsset(assetDto);
        asset.persist();
        
        // Die EDC-Instanz abrufen
        if (asset.getTargetEDC() == null) {
            Log.error("No target EDC specified for asset");
            throw new CustomException("No target EDC specified for asset");
        }
        
        try {
            // Asset im EDC erstellen
            JsonObject assetJson = createEdcAssetJson(asset);
            String edcUrl = asset.getTargetEDC().getUrl();
            String apiKey = asset.getTargetEDC().getApiKey();
            
            // API-Aufruf zum Erstellen des Assets im EDC
            edcClient.createAsset(edcUrl, apiKey, assetJson);
            
            // Optional: Standardrichtlinie und Vertragsdefinition erstellen, 
            // wenn das Asset erfolgreich erstellt wurde
            createDefaultPolicyAndContractDefinition(asset);
            
            return EDCAssetMapper.assetMapper.assetToAssetDto(asset);
        } catch (Exception e) {
            // Bei Fehler rollback in der Datenbank
            asset.delete();
            Log.errorf("Failed to create asset in EDC: %s", e.getMessage());
            throw new CustomException("Failed to create asset in EDC: " + e.getMessage());
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
        final EDCAsset updatedAsset = EDCAssetMapper.assetMapper.assetDtoToAsset(updatedAssetDto);

        if (persistedAsset == null) {
            final String exceptionMessage = "No Asset found with id";
            Log.errorf(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

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

        return EDCAssetMapper.assetMapper.assetToAssetDto(persistedAsset);
    }
    
    /**
     * Aktualisiert ein bestehendes Asset in der Datenbank und im EDC.
     *
     * @param id Die ID des zu aktualisierenden Assets
     * @param updatedAssetDto Das aktualisierte Asset als DTO
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn kein Asset mit der angegebenen ID gefunden wird oder die Aktualisierung im EDC fehlschlägt
     */
    @Transactional
    public EDCAssetDto updateInEdcAndDatabase(UUID id, EDCAssetDto updatedAssetDto) throws CustomException {
        // Zuerst in der Datenbank aktualisieren
        final EDCAsset persistedAsset = EDCAsset.findById(id);
        final EDCAsset updatedAsset = EDCAssetMapper.assetMapper.assetDtoToAsset(updatedAssetDto);

        if (persistedAsset == null) {
            final String exceptionMessage = "No Asset found with id";
            Log.errorf(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

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
        
        try {
            // Dann im EDC aktualisieren
            // Hinweis: EDC unterstützt kein direktes Update, daher löschen und neu erstellen
            if (persistedAsset.getTargetEDC() != null) {
                String edcUrl = persistedAsset.getTargetEDC().getUrl();
                String apiKey = persistedAsset.getTargetEDC().getApiKey();
                
                // Versuche das Asset zu löschen, falls es bereits existiert
                try {
                    edcClient.deleteAsset(edcUrl, apiKey, persistedAsset.getAssetId());
                } catch (Exception e) {
                    // Ignoriere Fehler beim Löschen, falls das Asset nicht existiert
                    Log.warnf("Could not delete asset in EDC: %s", e.getMessage());
                }
                
                // Asset neu erstellen
                JsonObject assetJson = createEdcAssetJson(persistedAsset);
                edcClient.createAsset(edcUrl, apiKey, assetJson);
                
                // Richtlinie und Vertragsdefinition aktualisieren
                updateDefaultPolicyAndContractDefinition(persistedAsset);
            }
            
            return EDCAssetMapper.assetMapper.assetToAssetDto(persistedAsset);
        } catch (Exception e) {
            Log.errorf("Failed to update asset in EDC: %s", e.getMessage());
            throw new CustomException("Failed to update asset in EDC: " + e.getMessage());
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
        return EDCAsset.deleteById(id);
    }
    
    /**
     * Löscht ein Asset aus der Datenbank und aus dem EDC.
     *
     * @param id Die ID des zu löschenden Assets
     * @return true, wenn das Asset erfolgreich gelöscht wurde, false sonst
     * @throws CustomException Wenn das Löschen im EDC fehlschlägt
     */
    @Transactional
    public boolean deleteFromEdcAndDatabase(final UUID id) throws CustomException {
        EDCAsset asset = EDCAsset.findById(id);
        
        if (asset == null) {
            return false;
        }
        
        if (asset.getTargetEDC() != null) {
            try {
                String edcUrl = asset.getTargetEDC().getUrl();
                String apiKey = asset.getTargetEDC().getApiKey();
                String assetId = asset.getAssetId();
                
                // Löschen des Assets im EDC
                edcClient.deleteAsset(edcUrl, apiKey, assetId);
                
                // Optional: Löschen der zugehörigen Vertragsdefinitionen
                deleteAssociatedContractDefinitions(asset);
            } catch (Exception e) {
                Log.warnf("Failed to delete asset from EDC: %s", e.getMessage());
                // Entscheidung: Trotz Fehler im EDC in der Datenbank löschen oder Exception werfen?
                throw new CustomException("Failed to delete asset from EDC: " + e.getMessage());
            }
        }
        
        // Löschen des Assets aus der Datenbank
        return EDCAsset.deleteById(id);
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
            final String exceptionMessage = "No EDC instance found with id " + edcId;
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
            final String exceptionMessage = "No Asset found with id " + assetId + " for EDC instance " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        return EDCAssetMapper.assetMapper.assetToAssetDto(results.get(0));
    }
    
    /**
     * Erstellt ein JSON-Objekt für ein Asset im EDC-Format.
     * 
     * @param asset Das Asset, das in ein JSON-Objekt konvertiert werden soll
     * @return Das Asset als JSON-Objekt im EDC-Format
     */
    private JsonObject createEdcAssetJson(EDCAsset asset) {
        JsonObjectBuilder assetBuilder = Json.createObjectBuilder()
            .add("@id", asset.getAssetId())
            .add("@type", "Asset");
        
        // Properties hinzufügen
        JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
        
        // Pflichtfelder hinzufügen
        propertiesBuilder.add("asset:prop:id", asset.getAssetId());
        propertiesBuilder.add("asset:prop:contenttype", 
            asset.getContentType() != null ? asset.getContentType() : "application/json");
        
        // Optionale Felder hinzufügen
        if (asset.getDescription() != null) {
            propertiesBuilder.add("asset:prop:description", asset.getDescription());
        }
        
        if (asset.getType() != null) {
            propertiesBuilder.add("asset:prop:type", asset.getType());
        }
        
        // Benutzerdefinierte Properties hinzufügen
        if (asset.getProperties() != null) {
            // Da EDCProperty in diesem Fall ein einzelnes Objekt ist und keine Collection,
            // fügen wir es direkt als Beschreibung hinzu, falls vorhanden
            propertiesBuilder.add("asset:prop:customDescription", asset.getProperties().getDescription());
        }
        
        assetBuilder.add("properties", propertiesBuilder);
        
        // DataAddress hinzufügen
        JsonObjectBuilder dataAddressBuilder = Json.createObjectBuilder();
        
        if (asset.getDataAddress() != null) {
            // Type ist ein Pflichtfeld für DataAddress
            dataAddressBuilder.add("type", asset.getDataAddress().getType() != null 
                ? asset.getDataAddress().getType() : "HttpData");
            
            // URL hinzufügen, falls vorhanden
            if (asset.getUrl() != null) {
                dataAddressBuilder.add("baseUrl", asset.getUrl());
            }
            
            // Weitere wichtige Daten-Adress-Eigenschaften
            dataAddressBuilder.add("proxyPath", asset.getDataAddress().isProxyPath());
            dataAddressBuilder.add("proxyQueryParams", asset.getDataAddress().isProxyQueryParams());
        } else {
            // Minimale DataAddress, falls keine definiert wurde
            dataAddressBuilder.add("type", "HttpData");
            if (asset.getUrl() != null) {
                dataAddressBuilder.add("baseUrl", asset.getUrl());
            }
        }
        
        assetBuilder.add("dataAddress", dataAddressBuilder);
        
        return assetBuilder.build();
    }
    
    /**
     * Erstellt eine Standard-Policy und eine Contract-Definition für ein Asset im EDC.
     * 
     * @param asset Das Asset, für das die Policy erstellt werden soll
     */
    private void createDefaultPolicyAndContractDefinition(EDCAsset asset) {
        if (asset.getTargetEDC() == null) {
            Log.warn("No target EDC specified for asset, skipping policy creation");
            return;
        }
        
        String edcUrl = asset.getTargetEDC().getUrl();
        String apiKey = asset.getTargetEDC().getApiKey();
        String assetId = asset.getAssetId();
        
        try {
            // Erstelle eine Policy mit einer USE-Berechtigung
            String policyId = "policy-" + assetId;
            JsonObjectBuilder policyBuilder = Json.createObjectBuilder();
            
            // Policy-Definition erstellen
            policyBuilder.add("@context", Json.createObjectBuilder()
                .add("odrl", "http://www.w3.org/ns/odrl/2/"));
            
            policyBuilder.add("@id", policyId);
            policyBuilder.add("@type", "PolicyDefinition");
            
            // Policy als JSON definieren
            JsonObjectBuilder policyDefBuilder = Json.createObjectBuilder();
            policyDefBuilder.add("@type", "Policy");
            policyDefBuilder.add("odrl:permission", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("odrl:action", "USE")
                    .add("odrl:constraint", Json.createArrayBuilder())));
            
            policyBuilder.add("policy", policyDefBuilder);
            
            // Policy im EDC erstellen
            edcClient.createPolicy(edcUrl, apiKey, policyBuilder.build());
            
            // Contract-Definition erstellen
            String contractDefId = "contract-" + assetId;
            JsonObjectBuilder contractDefBuilder = Json.createObjectBuilder();
            
            contractDefBuilder.add("@id", contractDefId);
            contractDefBuilder.add("@type", "ContractDefinition");
            contractDefBuilder.add("accessPolicyId", policyId);
            contractDefBuilder.add("contractPolicyId", policyId);
            contractDefBuilder.add("assetsSelector", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("@type", "CriterionDto")
                    .add("operandLeft", "https://w3id.org/edc/v0.0.1/ns/id")
                    .add("operator", "=")
                    .add("operandRight", assetId)));
            
            // Contract-Definition im EDC erstellen
            edcClient.createContractDefinition(edcUrl, apiKey, contractDefBuilder.build());
            
            Log.infof("Created policy and contract definition for asset %s", assetId);
        } catch (Exception e) {
            Log.warnf("Failed to create policy and contract definition: %s", e.getMessage());
            // Fehler hier nicht weiterleiten, da die Asset-Erstellung sonst abgebrochen würde
        }
    }
    
    /**
     * Aktualisiert die Standard-Policy und Contract-Definition für ein Asset im EDC.
     * Da EDC keine direkte Update-Funktion anbietet, wird zuerst gelöscht und dann neu erstellt.
     * 
     * @param asset Das Asset, für das die Policy aktualisiert werden soll
     */
    private void updateDefaultPolicyAndContractDefinition(EDCAsset asset) {
        if (asset.getTargetEDC() == null) {
            Log.warn("No target EDC specified for asset, skipping policy update");
            return;
        }
        
        try {
            // Zuerst löschen wir die alten Definitionen
            deleteAssociatedContractDefinitions(asset);
            
            // Dann erstellen wir neue Definitionen
            createDefaultPolicyAndContractDefinition(asset);
        } catch (Exception e) {
            Log.warnf("Failed to update policy and contract definition: %s", e.getMessage());
            // Fehler hier nicht weiterleiten, da die Asset-Aktualisierung sonst abgebrochen würde
        }
    }
    
    /**
     * Löscht die mit einem Asset verbundenen Contract-Definitionen und Policies im EDC.
     * 
     * @param asset Das Asset, dessen Contract-Definitionen gelöscht werden sollen
     */
    private void deleteAssociatedContractDefinitions(EDCAsset asset) {
        if (asset.getTargetEDC() == null) {
            return;
        }
        
        String edcUrl = asset.getTargetEDC().getUrl();
        String apiKey = asset.getTargetEDC().getApiKey();
        String assetId = asset.getAssetId();
        String contractDefId = "contract-" + assetId;
        String policyId = "policy-" + assetId;
        
        try {
            // Contract-Definition löschen
            // Hinweis: Hier müssen wir entsprechende Methoden im EDCClient implementieren
            // Diese Implementierung ist beispielhaft und muss evtl. angepasst werden
            deleteContractDefinition(edcUrl, apiKey, contractDefId);
            
            // Policy löschen
            deletePolicy(edcUrl, apiKey, policyId);
        } catch (Exception e) {
            Log.warnf("Failed to delete contract definitions and policies: %s", e.getMessage());
        }
    }
    
    /**
     * Löscht eine Contract-Definition im EDC.
     * 
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param contractDefId Die ID der zu löschenden Contract-Definition
     */
    private void deleteContractDefinition(String edcUrl, String apiKey, String contractDefId) {
        try {
            // Da der EDCClient noch keine Methode zum Löschen von Contract-Definitionen hat,
            // loggen wir nur, dass wir sie löschen würden
            // TODO: Implementiere Methode zum Löschen von Contract-Definitionen im EDCClient
            Log.infof("Would delete contract definition %s (not yet implemented)", contractDefId);
        } catch (Exception e) {
            Log.warnf("Failed to delete contract definition %s: %s", contractDefId, e.getMessage());
        }
    }
    
    /**
     * Löscht eine Policy im EDC.
     * 
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param policyId Die ID der zu löschenden Policy
     */
    private void deletePolicy(String edcUrl, String apiKey, String policyId) {
        try {
            // Da der EDCClient noch keine Methode zum Löschen von Policies hat,
            // loggen wir nur, dass wir sie löschen würden
            // TODO: Implementiere Methode zum Löschen von Policies im EDCClient
            Log.infof("Would delete policy %s (not yet implemented)", policyId);
        } catch (Exception e) {
            Log.warnf("Failed to delete policy %s: %s", policyId, e.getMessage());
        }
    }
    
    /**
     * Listet alle Assets auf, die zu einer bestimmten EDC-Instanz gehören.
     *
     * @param edcId Die ID der EDC-Instanz
     * @return Eine Liste aller Assets für diese EDC-Instanz als DTOs
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    public List<EDCAssetDto> listAllByEdcId(final UUID edcId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
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
            throw new CustomException("Asset data is missing");
        }
        
        // Prüfe, ob die EDC-Instanz existiert
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
        
        // DataAddress extrahieren und konfigurieren
        EDCDataAddressDto dataAddressDto = new EDCDataAddressDto();
        if (frontendJson.containsKey("dataAddress") && frontendJson.get("dataAddress") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> daMap = (Map<String, Object>) frontendJson.get("dataAddress");
            
            // Typ aus dataAddress extrahieren oder Standardwert verwenden
            if (daMap.containsKey("type")) {
                dataAddressDto.setType(daMap.get("type").toString());
            } else {
                dataAddressDto.setType("HttpData"); // Standardwert
            }
            
            // Base URL aus verschiedenen möglichen Quellen extrahieren
            String baseUrl = null;
            if (daMap.containsKey("base_url")) {
                baseUrl = daMap.get("base_url").toString();
            } else if (daMap.containsKey("baseURL")) {
                baseUrl = daMap.get("baseURL").toString();
            } else if (daMap.containsKey("baseUrl")) {
                baseUrl = daMap.get("baseUrl").toString();
            }
            
            // Fallback-Logik für BaseURL
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
            
            // Proxy-Einstellungen konfigurieren
            configureProxySettings(dataAddressDto, daMap);
            
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
            } else {
                dataAddressDto.setBaseURL("https://example.com/api");
            }
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
     * Hilfsmethode zum Konfigurieren der Proxy-Einstellungen für eine DataAddress.
     * 
     * @param dataAddressDto Das zu konfigurierende DataAddressDto
     * @param daMap Die Map mit den Proxy-Einstellungen aus dem Frontend
     */
    private void configureProxySettings(EDCDataAddressDto dataAddressDto, Map<String, Object> daMap) {
        // ProxyPath-Einstellung
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
        
        // ProxyQueryParams-Einstellung
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
        // Alternativ direkt aus dem Frontend-JSON
        else if (frontendJson.containsKey("description")) {
            description = frontendJson.get("description").toString();
            // Auch in Properties hinzufügen
            propertiesMap.put("asset:prop:description", description);
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
        
        // Erstelle ein neues DTO mit der richtigen EDC-ID
        EDCAssetDto newAssetDto = new EDCAssetDto(
            assetId,
            assetDto.assetId(),
            assetDto.url(),
            assetDto.type(),
            assetDto.contentType(),
            assetDto.description(),
            targetEDCId,  // Setze die targetEDCId auf die angegebene EDC-Instanz
            assetDto.dataAddress(),
            assetDto.properties()
        );
        
        // Debug-Ausgabe für die Fehlersuche
        Log.info("Creating asset with ID: " + assetId + " for EDC: " + targetEDCId);
        
        try {
            // Konvertiere DTO in Entity und persistiere es
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
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn die EDC-Instanz oder das Asset nicht existieren
     */
    @Transactional
    public EDCAssetDto updateForEdc(UUID edcId, UUID assetId, EDCAssetDto updatedAssetDto) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
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
        
        // Konvertiere DTO in Entity
        final EDCAsset updatedAsset = EDCAssetMapper.assetMapper.assetDtoToAsset(newUpdatedAssetDto);
        
        // Aktualisiere alle Felder des persistierten Assets
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
        // Prüfe, ob die EDC-Instanz existiert
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            final String exceptionMessage = "No EDC instance found with id " + edcId;
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
            Log.warn("No asset found with assetId " + assetId + " for EDC " + edcId);
            return false;
        }
        
        // Lösche das gefundene Asset
        EDCAsset asset = results.get(0);
        asset.delete();
        Log.info("Successfully deleted asset with assetId " + assetId + " for EDC " + edcId);
        return true;
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
            final String exceptionMessage = "No EDC instance found with id " + edcId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Suche nach dem Asset mit der angegebenen UUID in der angegebenen EDC-Instanz
        TypedQuery<EDCAsset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId", 
                EDCAsset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<EDCAsset> results = query.getResultList();
        if (results.isEmpty()) {
            return false;
        }
        
        // Lösche das gefundene Asset
        EDCAsset asset = results.get(0);
        asset.delete();
        return true;
    }
    
    /**
     * Initiiert einen Datentransfer für ein Asset.
     * Diese Methode startet einen Prozess zum Übertragen der Daten des angegebenen Assets
     * zu einer Ziel-URL.
     *
     * @param assetId Die ID des zu übertragenden Assets in der Datenbank
     * @param destinationUrl Die URL, an die die Daten übertragen werden sollen
     * @return Die Transfer-Prozess-ID als String
     * @throws CustomException Wenn der Datentransfer nicht initiiert werden kann
     */
    @Transactional
    public String initiateDataTransfer(UUID assetId, String destinationUrl) throws CustomException {
        // Asset aus der Datenbank abrufen
        final EDCAsset asset = EDCAsset.findById(assetId);
        if (asset == null) {
            final String exceptionMessage = "No Asset found with id " + assetId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Prüfen, ob das Asset mit einem EDC verknüpft ist
        if (asset.getTargetEDC() == null) {
            final String exceptionMessage = "Asset is not associated with an EDC instance";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // EDC-Verbindungsinformationen
        String edcUrl = asset.getTargetEDC().getUrl();
        String apiKey = asset.getTargetEDC().getApiKey();
        
        try {
            // Zuerst prüfen, ob für dieses Asset bereits eine Contract Definition existiert
            String contractId = "contract-" + asset.getAssetId(); // Wir nehmen an, dass die Contract-Definition diese ID hat
            
            // Datentransfer initiieren
            String transferResult = edcClient.initiateDataTransfer(
                edcUrl, 
                apiKey, 
                asset.getAssetId(), 
                contractId, 
                destinationUrl, 
                asset.getContentType()
            );
            
            Log.infof("Data transfer initiated for asset %s: %s", asset.getAssetId(), transferResult);
            
            // Transfer-Prozess-ID extrahieren und zurückgeben
            return extractTransferProcessIdFromResponse(transferResult);
        } catch (Exception e) {
            Log.errorf("Failed to initiate data transfer for asset %s: %s", asset.getAssetId(), e.getMessage());
            throw new CustomException("Failed to initiate data transfer: " + e.getMessage());
        }
    }
    
    /**
     * Überprüft den Status eines Datentransfer-Prozesses.
     *
     * @param assetId Die ID des Assets in der Datenbank
     * @param transferProcessId Die ID des zu überprüfenden Transfer-Prozesses
     * @return Der aktuelle Status des Transfer-Prozesses als String
     * @throws CustomException Wenn der Status nicht abgerufen werden kann
     */
    public String checkTransferStatus(UUID assetId, String transferProcessId) throws CustomException {
        // Asset aus der Datenbank abrufen
        final EDCAsset asset = EDCAsset.findById(assetId);
        if (asset == null) {
            final String exceptionMessage = "No Asset found with id " + assetId;
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // Prüfen, ob das Asset mit einem EDC verknüpft ist
        if (asset.getTargetEDC() == null) {
            final String exceptionMessage = "Asset is not associated with an EDC instance";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        
        // EDC-Verbindungsinformationen
        String edcUrl = asset.getTargetEDC().getUrl();
        String apiKey = asset.getTargetEDC().getApiKey();
        
        try {
            // Status des Transfer-Prozesses abrufen
            String statusResult = edcClient.getTransferProcessStatus(edcUrl, apiKey, transferProcessId);
            Log.infof("Transfer status for process %s: %s", transferProcessId, statusResult);
            
            return statusResult;
        } catch (Exception e) {
            Log.errorf("Failed to check transfer status for process %s: %s", transferProcessId, e.getMessage());
            throw new CustomException("Failed to check transfer status: " + e.getMessage());
        }
    }
    
    /**
     * Extrahiert die Transfer-Prozess-ID aus der EDC-Antwort.
     *
     * @param edcResponse Die EDC-Antwort als JSON-String
     * @return Die Transfer-Prozess-ID
     */
    private String extractTransferProcessIdFromResponse(String edcResponse) {
        try (jakarta.json.JsonReader reader = Json.createReader(new java.io.StringReader(edcResponse))) {
            jakarta.json.JsonObject responseJson = reader.readObject();
            return responseJson.getString("@id");
        } catch (Exception e) {
            Log.errorf("Failed to extract transfer process ID from response: %s", e.getMessage());
            throw new RuntimeException("Failed to extract transfer process ID from response: " + e.getMessage());
        }
    }
}
