package de.unistuttgart.stayinsync.core.configuration.edc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.DataAddress;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EdcInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Property;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.CreateEDCAssetDTO;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.EDCClient;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.CustomException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service-Klasse für die Verwaltung von EDC-Assets.
 * <p>
 * Diese Klasse bietet Methoden zum Erstellen, Lesen, Aktualisieren und Löschen
 * von EDC-Assets (Eclipse Dataspace Connector) mit Panache Entities und Record DTOs.
 */
@ApplicationScoped
public class EDCAssetService {
    
    private static final Logger LOG = Logger.getLogger(EDCAssetService.class.getName());

    @Inject
    EntityManager entityManager;
    
    @Inject
    HttpDataFetcherService httpDataFetcherService;

    /**
     * Erstellt einen EDC-Client für die angegebene Basis-URL.
     *
     * @param baseUrl Die Basis-URL für den Client
     * @return Ein neuer EDCClient
     */
    public EDCClient createClient(String baseUrl) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(EDCClient.class);
    }
    
    /**
     * Erzeugt den Standard-Kontext für EDC-Assets.
     * 
     * @return Eine Map mit dem Standard-EDC-Kontext
     */
    private Map<String, String> getDefaultContext() {
        return new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
    }

    /**
     * Findet ein Asset anhand seiner ID.
     *
     * @param id Die ID des zu findenden Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn kein Asset mit der angegebenen ID gefunden wird
     */
    public EDCAssetDto findById(final Long id) throws CustomException {
        final Asset asset = Asset.findById(id);
        if (asset == null) {
            final String exceptionMessage = "Kein Asset mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }
        return EDCAssetMapper.INSTANCE.assetToAssetDto(asset);
    }

    /**
     * Listet alle Assets auf, die in der Datenbank vorhanden sind.
     *
     * @return Eine Liste aller Assets als DTOs
     */
    public List<EDCAssetDto> listAll() {
        List<Asset> assetList = Asset.listAll();
        return assetList.stream()
                .map(EDCAssetMapper.INSTANCE::assetToAssetDto)
                .collect(Collectors.toList());
    }

    /**
     * Erstellt ein neues Asset in der Datenbank und im EDC.
     *
     * @param assetDto Das zu erstellende Asset als DTO
     * @return Das erstellte Asset als DTO
     * @throws CustomException Wenn das Asset nicht erstellt werden konnte
     */
    @Transactional
    public EDCAssetDto create(EDCAssetDto assetDto) throws CustomException {
        // Validiere die EDC-Instanz, falls eine angegeben ist
        if (assetDto.targetEDCId() != null) {
            EdcInstance edcInstance = EdcInstance.findById(assetDto.targetEDCId());
            if (edcInstance == null) {
                throw new CustomException("Keine EDC-Instanz mit ID " + assetDto.targetEDCId() + " gefunden");
            }
        }
        
        // Asset in der Datenbank erstellen
        Asset asset = EDCAssetMapper.INSTANCE.assetDtoToAsset(assetDto);
        asset.persist();
        Log.info("Asset mit ID " + asset.id + " erstellt");
        
        // Simuliere EDC-Integration, wenn eine EDC-Instanz verknüpft ist
        if (asset.targetEDC != null) {
            try {
                EDCClient client = createClient("http://dataprovider-controlplane.tx.test/management/v3");
                
                // Bestimme die zu verwendende URL
                String urlToUse = null;
                
                // Priorität: 1. DataAddress.baseUrl, 2. Asset.url, 3. Fallback-URL
                if (asset.dataAddress != null && asset.dataAddress.baseUrl != null && !asset.dataAddress.baseUrl.isEmpty()) {
                    urlToUse = asset.dataAddress.baseUrl;
                    Log.info("Verwende DataAddress.baseUrl: " + urlToUse);
                } else if (asset.url != null && !asset.url.isEmpty()) {
                    urlToUse = asset.url;
                    Log.info("Verwende Asset.url: " + urlToUse);
                } else {
                    urlToUse = "http://dataprovider-submodelserver.tx.test";
                    Log.info("Verwende Fallback-URL: " + urlToUse);
                }
                
                // Erstelle ein neues CreateEDCAssetDTO mit der Asset-ID und der URL
                CreateEDCAssetDTO edcAssetDTO = new CreateEDCAssetDTO(urlToUse);
                edcAssetDTO.setId(asset.assetId != null ? asset.assetId : "asset-" + UUID.randomUUID());
                
                // Übertrage die zusätzlichen Parameter auf das DTO
                if (asset.dataAddress != null) {
                    // Path übertragen
                    if (asset.dataAddress.path != null && !asset.dataAddress.path.isEmpty()) {
                        edcAssetDTO.getDataAddress().setPath(asset.dataAddress.path);
                        LOG.info("Übertrage path an EDC: " + asset.dataAddress.path);
                    }
                    
                    // QueryParams übertragen
                    if (asset.dataAddress.queryParams != null && !asset.dataAddress.queryParams.isEmpty()) {
                        edcAssetDTO.getDataAddress().setQueryParams(asset.dataAddress.queryParams);
                        LOG.info("Übertrage queryParams an EDC: " + asset.dataAddress.queryParams);
                    }
                    
                    // HeaderParams direkt als String übertragen
                    if (asset.dataAddress.headerParams != null && !asset.dataAddress.headerParams.isEmpty()) {
                        edcAssetDTO.getDataAddress().setHeaderParams(asset.dataAddress.headerParams);
                        LOG.info("Übertrage headerParams an EDC als String");
                    }
                }
                
                if (asset.description != null) {
                    edcAssetDTO.getProperties().setDescription(asset.description);
                }
                
                // API-Aufruf an EDC durchführen
                RestResponse<JsonObject> response = client.createAsset("TEST2", edcAssetDTO);
                Log.infof("EDC-Anfrage Status: %d", response.getStatus());
                
                if (response.getStatus() >= 400) {
                    Log.errorf("EDC-Fehler %d: %s", response.getStatus(), response.getEntity());
                    // Asset aus der Datenbank entfernen, wenn EDC-Integration fehlschlägt
                    Asset.deleteById(asset.id);
                    throw new CustomException("Fehler bei der EDC-Integration: " + response.getStatus());
                }
                
                Log.info("Asset im EDC mit ID: " + asset.assetId + " erstellt");
            } catch (Exception e) {
                // Bei Fehler das Asset aus der Datenbank löschen und Exception werfen
                Asset.deleteById(asset.id);
                throw new CustomException("Fehler beim Erstellen des Assets im EDC: " + e.getMessage());
            }
        }
        
        return EDCAssetMapper.INSTANCE.assetToAssetDto(asset);
    }

    /**
     * Aktualisiert ein bestehendes Asset in der Datenbank und im EDC.
     *
     * @param id              Die ID des zu aktualisierenden Assets
     * @param updatedAssetDto Das aktualisierte Asset als DTO
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn kein Asset mit der angegebenen ID gefunden wird
     */
    @Transactional
    public EDCAssetDto update(Long id, EDCAssetDto updatedAssetDto) throws CustomException {
        final Asset persistedAsset = Asset.findById(id);
        if (persistedAsset == null) {
            final String exceptionMessage = "Kein Asset mit ID " + id + " gefunden";
            Log.error(exceptionMessage);
            throw new CustomException(exceptionMessage);
        }

        final Asset updatedAsset = EDCAssetMapper.INSTANCE.assetDtoToAsset(updatedAssetDto);

        // Aktualisiere alle Felder des Assets
        persistedAsset.assetId = updatedAsset.assetId;
        persistedAsset.url = updatedAsset.url;
        persistedAsset.type = updatedAsset.type;
        persistedAsset.contentType = updatedAsset.contentType;
        persistedAsset.description = updatedAsset.description;
        persistedAsset.dataAddress = updatedAsset.dataAddress;
        persistedAsset.properties = updatedAsset.properties;
        persistedAsset.targetSystemEndpoint = updatedAsset.targetSystemEndpoint;
        persistedAsset.targetEDC = updatedAsset.targetEDC;

        // Simuliere EDC-Integration, wenn eine EDC-Instanz verknüpft ist
        if (persistedAsset.targetEDC != null) {
            try {
                Log.info("Simuliere Aktualisierung von Asset im EDC mit ID: " + persistedAsset.assetId);
                // Hier würde die tatsächliche EDC-Integration stattfinden
            } catch (Exception e) {
                throw new CustomException("Fehler beim Aktualisieren des Assets im EDC: " + e.getMessage());
            }
        }

        Log.info("Asset mit ID " + id + " aktualisiert");
        return EDCAssetMapper.INSTANCE.assetToAssetDto(persistedAsset);
    }

    /**
     * Löscht ein Asset aus der Datenbank und dem EDC.
     *
     * @param id Die ID des zu löschenden Assets
     * @return true, wenn das Asset erfolgreich gelöscht wurde, false sonst
     * @throws CustomException Wenn das Löschen fehlschlägt
     */
    @Transactional
    public boolean delete(final Long id) throws CustomException {
        Asset asset = Asset.findById(id);
        
        if (asset == null) {
            Log.warn("Kein Asset mit ID " + id + " gefunden zum Löschen");
            return false;
        }

        // Tatsächliche EDC-Integration, wenn eine EDC-Instanz verknüpft ist
        if (asset.targetEDC != null) {
            try {
                Log.info("Lösche Asset im EDC mit ID: " + asset.assetId);
                
                // EDC Client für die Kommunikation mit dem EDC erstellen
                EDCClient client = createClient(asset.targetEDC.edcAssetEndpoint);
                
                // API-Key für die Authentifizierung holen
                String apiKey = asset.targetEDC.apiKey;
                
                // Asset im EDC löschen
                RestResponse<Void> response = client.deleteAsset(apiKey, asset.assetId);
                
                // Prüfen, ob das Löschen im EDC erfolgreich war
                if (response.getStatus() >= 300) {
                    throw new CustomException("EDC returned error status: " + response.getStatus());
                }
                
                Log.info("Asset im EDC erfolgreich gelöscht: " + asset.assetId);
            } catch (Exception e) {
                // Fehler protokollieren, aber fortfahren mit dem Löschen aus der Datenbank
                Log.error("Fehler beim Löschen des Assets im EDC: " + e.getMessage(), e);
                // Fehlermeldung werfen, um den Client zu informieren
                throw new CustomException("Fehler beim Löschen des Assets im EDC: " + e.getMessage());
            }
        }

        // Löschen des Assets aus der Datenbank
        boolean deleted = Asset.deleteById(id);
        if (deleted) {
            Log.info("Asset mit ID " + id + " gelöscht");
        } else {
            Log.warn("Asset mit ID " + id + " konnte nicht gelöscht werden");
        }
        
        return deleted;
    }
    
    /**
     * Löscht ein Asset anhand seiner ID und der ID der zugehörigen EDC-Instanz.
     *
     * @param edcId Die ID der EDC-Instanz
     * @param assetId Die ID des zu löschenden Assets
     * @return true, wenn das Asset erfolgreich gelöscht wurde, sonst false
     * @throws CustomException Wenn beim Löschen ein Fehler auftritt oder die EDC-Instanz nicht existiert
     */
    @Transactional
    public boolean deleteByIdAndEdcId(final Long edcId, final Long assetId) throws CustomException {
        Log.info("Löschvorgang gestartet für Asset mit ID " + assetId + " und EDC-ID " + edcId);
        
        // Prüfe, ob die EDC-Instanz existiert
        EdcInstance edcInstance = EdcInstance.findById(edcId);
        if (edcInstance == null) {
            Log.error("Keine EDC-Instanz mit ID " + edcId + " gefunden");
            throw new CustomException("Keine EDC-Instanz mit ID " + edcId + " gefunden");
        }
        
        // Suche nach dem Asset mit der angegebenen ID in der angegebenen EDC-Instanz
        TypedQuery<Asset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId",
                Asset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);
        
        List<Asset> assets = query.getResultList();
        if (assets.isEmpty()) {
            Log.warn("Kein Asset mit ID " + assetId + " und EDC-ID " + edcId + " gefunden zum Löschen");
            return false;
        }
        
        Asset asset = assets.get(0);
        Log.info("Asset gefunden: ID " + assetId + ", EDC-Asset-ID " + asset.assetId);
        
        // Versuche die EDC-Integration, wenn möglich, aber lass es nicht den gesamten Löschvorgang blockieren
        boolean edcDeletionSuccessful = false;
        try {
            if (edcInstance.edcAssetEndpoint != null && !edcInstance.edcAssetEndpoint.isBlank()) {
                Log.info("Versuche Asset im EDC zu löschen: " + asset.assetId + " mit Endpunkt: " + edcInstance.edcAssetEndpoint);
                
                EDCClient client = createClient(edcInstance.edcAssetEndpoint);
                
                // API-Key für die Authentifizierung holen
                String apiKey = edcInstance.apiKey != null ? edcInstance.apiKey : "default-key";
                
                // Asset im EDC löschen
                RestResponse<Void> response = client.deleteAsset(apiKey, asset.assetId);
                
                // Prüfen, ob das Löschen im EDC erfolgreich war
                if (response.getStatus() >= 300) {
                    Log.warn("EDC returned error status: " + response.getStatus() + " but continuing with database deletion");
                } else {
                    Log.info("Asset im EDC erfolgreich gelöscht: " + asset.assetId);
                    edcDeletionSuccessful = true;
                }
            } else {
                Log.warn("EDC Asset Endpoint ist nicht konfiguriert, überspringe EDC-Löschung");
            }
        } catch (Exception e) {
            // Fehler protokollieren, aber trotzdem mit dem Löschen aus der Datenbank fortfahren
            Log.error("Fehler beim Löschen des Assets im EDC: " + e.getMessage(), e);
            Log.info("Fahre fort mit dem Löschen aus der Datenbank, unabhängig vom EDC-Fehler");
        }
        
        // Statt zu versuchen, einzelne Teile zu entfernen, führen wir native SQL-Queries aus
        try {
            Log.info("Beginne mit dem Löschen des Assets aus der Datenbank: ID=" + assetId);
            
            // Zuerst die Asset-ID und die IDs der abhängigen Objekte speichern
            Long propertiesId = asset.properties != null ? asset.properties.id : null;
            Long dataAddressId = asset.dataAddress != null ? asset.dataAddress.id : null;
            
            Log.info("Asset-ID: " + assetId + ", Properties-ID: " + propertiesId + ", DataAddress-ID: " + dataAddressId);
            
            // Zuerst das Asset selbst löschen, weil es Fremdschlüsselbeziehungen zu den anderen Entitäten hat
            Log.info("Lösche das Asset mit ID: " + assetId);
            int deletedAssets = entityManager.createNativeQuery("DELETE FROM edc_asset WHERE id = :assetId")
                    .setParameter("assetId", assetId)
                    .executeUpdate();
            
            Log.info("Gelöschte Assets: " + deletedAssets);
            
            // Dann die abhängigen Objekte löschen
            if (propertiesId != null) {
                Log.info("Lösche Properties mit ID: " + propertiesId);
                int deletedProps = entityManager.createNativeQuery("DELETE FROM edc_property WHERE id = :propId")
                        .setParameter("propId", propertiesId)
                        .executeUpdate();
                Log.info("Gelöschte Properties: " + deletedProps);
            }
            
            if (dataAddressId != null) {
                Log.info("Lösche DataAddress mit ID: " + dataAddressId);
                int deletedAddrs = entityManager.createNativeQuery("DELETE FROM edc_data_address WHERE id = :addrId")
                        .setParameter("addrId", dataAddressId)
                        .executeUpdate();
                Log.info("Gelöschte DataAddresses: " + deletedAddrs);
            }
            
            Log.info("Asset mit ID " + assetId + " und EDC-ID " + edcId + 
                    " erfolgreich gelöscht. EDC-Löschung " + (edcDeletionSuccessful ? "erfolgreich" : "übersprungen oder fehlgeschlagen"));
            return true;
        } catch (Exception e) {
            Log.error("Fehler beim Löschen des Assets aus der Datenbank: " + e.getMessage(), e);
            // Zweiter Versuch mit direktem Query
            try {
                Log.info("Zweiter Löschversuch mit direktem SQL-Query");
                int deleted = entityManager.createNativeQuery("DELETE FROM edc_asset WHERE id = :assetId")
                        .setParameter("assetId", assetId)
                        .executeUpdate();
                
                if (deleted > 0) {
                    Log.info("Asset mit ID " + assetId + " im zweiten Versuch erfolgreich gelöscht");
                    return true;
                } else {
                    Log.warn("Asset mit ID " + assetId + " konnte auch im zweiten Versuch nicht gelöscht werden");
                    throw new CustomException("Asset konnte nicht gelöscht werden");
                }
            } catch (Exception e2) {
                Log.error("Auch zweiter Löschversuch fehlgeschlagen: " + e2.getMessage(), e2);
                throw new CustomException("Fehler beim Löschen des Assets aus der Datenbank: " + e2.getMessage());
            }
        }
    }

    /**
     * Löscht ein Asset anhand seiner EDC-Asset-ID (assetId) und der ID der zugehörigen EDC-Instanz.
     *
     * @param edcId     Die ID der EDC-Instanz
     * @param edcAssetId Die EDC-Asset-ID des Assets
     * @return true, wenn das Asset erfolgreich gelöscht wurde, sonst false
     * @throws CustomException Wenn die EDC-Instanz nicht existiert oder ein Fehler auftritt
     */
    @Transactional
    public boolean deleteByEdcAssetId(final Long edcId, final String edcAssetId) throws CustomException {
        Log.info("Löschvorgang gestartet für Asset mit EDC-Asset-ID " + edcAssetId + " und EDC-ID " + edcId);
        
        // Prüfe, ob die EDC-Instanz existiert
        EdcInstance edcInstance = EdcInstance.findById(edcId);
        if (edcInstance == null) {
            Log.error("Keine EDC-Instanz mit ID " + edcId + " gefunden");
            throw new CustomException("Keine EDC-Instanz mit ID " + edcId + " gefunden");
        }
        
        // Verbesserte Logging vor der Suche
        Log.info("Suche nach Asset mit folgenden Kriterien - EDC-Asset-ID: '" + edcAssetId + "', EDC-ID: " + edcId);
        
        // Alle Assets für diese EDC-Instanz zählen für Debugging-Zwecke
        Long totalCount = (Long) entityManager.createQuery(
                "SELECT COUNT(a) FROM EDCAsset a WHERE a.targetEDC.id = :edcId")
                .setParameter("edcId", edcId)
                .getSingleResult();
                
        Log.info("Gesamtanzahl der Assets für EDC-ID " + edcId + ": " + totalCount);
        
        // Direkte Suche mit nativen SQL zur Umgehung möglicher Hibernate-Cache-Probleme
        List<Asset> assets = entityManager.createNativeQuery(
                "SELECT * FROM edc_asset WHERE asset_id = :assetId AND target_edc_id = :edcId", 
                Asset.class)
                .setParameter("assetId", edcAssetId)
                .setParameter("edcId", edcId)
                .getResultList();
                
        // Falls keine Ergebnisse, auch nach allen Assets mit dieser Asset-ID suchen (unabhängig von EDC-ID)
        if (assets.isEmpty()) {
            List<Object[]> allMatchingAssets = entityManager.createNativeQuery(
                    "SELECT id, asset_id, target_edc_id FROM edc_asset WHERE asset_id = :assetId")
                    .setParameter("assetId", edcAssetId)
                    .getResultList();
                    
            Log.info("Gesamtzahl der Assets mit Asset-ID '" + edcAssetId + "' unabhängig von EDC-ID: " + allMatchingAssets.size());
            
            for (Object[] result : allMatchingAssets) {
                Log.info("Gefundenes Asset: ID=" + result[0] + ", Asset-ID='" + result[1] + "', EDC-ID=" + result[2]);
            }
        }
        
        if (assets.isEmpty()) {
            Log.warn("Kein Asset mit EDC-Asset-ID '" + edcAssetId + "' und EDC-ID " + edcId + " gefunden zum Löschen");
            return false;
        }
        
        Asset asset = assets.get(0);
        Long assetId = asset.id;
        
        Log.info("Asset gefunden: ID " + assetId + ", EDC-Asset-ID " + edcAssetId);
        
        // Versuche die EDC-Integration, wenn möglich, aber lass es nicht den gesamten Löschvorgang blockieren
        boolean edcDeletionSuccessful = false;
        try {
            if (edcInstance.edcAssetEndpoint != null && !edcInstance.edcAssetEndpoint.isBlank()) {
                Log.info("Versuche Asset im EDC zu löschen: " + asset.assetId + " mit Endpunkt: " + edcInstance.edcAssetEndpoint);
                
                EDCClient client = createClient(edcInstance.edcAssetEndpoint);
                
                // API-Key für die Authentifizierung holen
                String apiKey = edcInstance.apiKey != null ? edcInstance.apiKey : "default-key";
                
                // Asset im EDC löschen
                RestResponse<Void> response = client.deleteAsset(apiKey, asset.assetId);
                
                // Prüfen, ob das Löschen im EDC erfolgreich war
                if (response.getStatus() >= 300) {
                    Log.warn("EDC returned error status: " + response.getStatus() + " but continuing with database deletion");
                } else {
                    Log.info("Asset im EDC erfolgreich gelöscht: " + asset.assetId);
                    edcDeletionSuccessful = true;
                }
            } else {
                Log.warn("EDC Asset Endpoint ist nicht konfiguriert, überspringe EDC-Löschung");
            }
        } catch (Exception e) {
            // Fehler protokollieren, aber trotzdem mit dem Löschen aus der Datenbank fortfahren
            Log.error("Fehler beim Löschen des Assets im EDC: " + e.getMessage(), e);
            Log.info("Fahre fort mit dem Löschen aus der Datenbank, unabhängig vom EDC-Fehler");
        }

        // Direktes Löschen mit JPA-Entity Management, um sicherzustellen, dass abhängige Objekte korrekt gelöscht werden
        try {
            Log.info("Beginne mit dem Löschen des Assets aus der Datenbank: ID=" + assetId);
            
            // Statt zu versuchen, einzelne Teile zu entfernen, führen wir native SQL-Queries aus,
            // um die Daten direkt zu löschen, und umgehen so die Hibernate-Probleme
            
            // Zuerst die Asset-ID und die IDs der abhängigen Objekte speichern
            Long propertiesId = asset.properties != null ? asset.properties.id : null;
            Long dataAddressId = asset.dataAddress != null ? asset.dataAddress.id : null;
            
            Log.info("Asset-ID: " + assetId + ", Properties-ID: " + propertiesId + ", DataAddress-ID: " + dataAddressId);
            
            // Zuerst das Asset selbst löschen, weil es Fremdschlüsselbeziehungen zu den anderen Entitäten hat
            Log.info("Lösche das Asset mit ID: " + assetId);
            int deletedAssets = entityManager.createNativeQuery("DELETE FROM edc_asset WHERE id = :assetId")
                    .setParameter("assetId", assetId)
                    .executeUpdate();
            
            Log.info("Gelöschte Assets: " + deletedAssets);
            
            // Dann die abhängigen Objekte löschen
            if (propertiesId != null) {
                Log.info("Lösche Properties mit ID: " + propertiesId);
                int deletedProps = entityManager.createNativeQuery("DELETE FROM edc_property WHERE id = :propId")
                        .setParameter("propId", propertiesId)
                        .executeUpdate();
                Log.info("Gelöschte Properties: " + deletedProps);
            }
            
            if (dataAddressId != null) {
                Log.info("Lösche DataAddress mit ID: " + dataAddressId);
                int deletedAddrs = entityManager.createNativeQuery("DELETE FROM edc_data_address WHERE id = :addrId")
                        .setParameter("addrId", dataAddressId)
                        .executeUpdate();
                Log.info("Gelöschte DataAddresses: " + deletedAddrs);
            }
            
            Log.info("Asset mit ID " + assetId + " und EDC-Asset-ID " + edcAssetId + " erfolgreich gelöscht. EDC-Löschung " + 
                    (edcDeletionSuccessful ? "erfolgreich" : "übersprungen oder fehlgeschlagen"));
            return true;
            
            // Dieser Block wurde bereits in der vorherigen Bearbeitung entfernt
        } catch (Exception e) {
            Log.error("Fehler beim Löschen des Assets aus der Datenbank: " + e.getMessage(), e);
            throw new CustomException("Fehler beim Löschen des Assets aus der Datenbank: " + e.getMessage());
        }
    }

    /**
     * Findet ein Asset anhand seiner ID und der ID der zugehörigen EDC-Instanz.
     *
     * @param edcId   Die ID der EDC-Instanz
     * @param assetId Die ID des Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn kein Asset gefunden wird oder die EDC-Instanz nicht existiert
     */
    public EDCAssetDto findByIdAndEdcId(final Long edcId, final Long assetId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EdcInstance edcInstance = EdcInstance.findById(edcId);
        if (edcInstance == null) {
            throw new CustomException("Keine EDC-Instanz mit ID " + edcId + " gefunden");
        }

        // Suche nach dem Asset mit der angegebenen ID in der angegebenen EDC-Instanz
        TypedQuery<Asset> query = entityManager.createQuery(
                "SELECT a FROM EDCAsset a WHERE a.id = :assetId AND a.targetEDC.id = :edcId",
                Asset.class);
        query.setParameter("assetId", assetId);
        query.setParameter("edcId", edcId);

        List<Asset> results = query.getResultList();
        if (results.isEmpty()) {
            throw new CustomException("Kein Asset mit ID " + assetId + 
                    " in EDC-Instanz mit ID " + edcId + " gefunden");
        }

        return EDCAssetMapper.INSTANCE.assetToAssetDto(results.get(0));
    }

    /**
     * Listet alle Assets einer EDC-Instanz auf.
     *
     * @param edcId Die ID der EDC-Instanz
     * @return Liste aller Assets der EDC-Instanz als DTOs
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    public List<EDCAssetDto> listAllByEdcId(final Long edcId) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EdcInstance edcInstance = EdcInstance.findById(edcId);
        if (edcInstance == null) {
            throw new CustomException("Keine EDC-Instanz mit ID " + edcId + " gefunden");
        }

        // Suche nach allen Assets in der angegebenen EDC-Instanz
        TypedQuery<Asset> query = entityManager.createQuery(
                "SELECT a FROM Asset a WHERE a.targetEDC.id = :edcId",
                Asset.class);
        query.setParameter("edcId", edcId);

        List<Asset> assetList = query.getResultList();
        
        return assetList.stream()
                .map(EDCAssetMapper.INSTANCE::assetToAssetDto)
                .collect(Collectors.toList());
    }

    /**
     * Verarbeitet ein Asset im Frontend-Format und konvertiert es in das interne DTO-Format.
     * Diese Methode extrahiert die relevanten Daten aus dem vom Frontend gelieferten JSON-Format
     * und erstellt ein EDCAssetDto daraus.
     *
     * @param frontendJson Die JSON-Daten aus dem Frontend als Map
     * @param edcId        Die ID der EDC-Instanz
     * @return Ein EDCAssetDto mit den Daten aus dem Frontend
     * @throws CustomException Wenn die Daten fehlen oder die EDC-Instanz nicht existiert
     */
    public EDCAssetDto processFrontendAsset(Map<String, Object> frontendJson, Long edcId) throws CustomException {
        // Prüfe, ob Daten vorhanden sind
        if (frontendJson == null) {
            throw new CustomException("Keine Daten vom Frontend erhalten");
        }

        // Prüfe, ob die EDC-Instanz existiert
        EdcInstance edcInstance = EdcInstance.findById(edcId);
        if (edcInstance == null) {
            throw new CustomException("Keine EDC-Instanz mit ID " + edcId + " gefunden");
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
        Map<String, Object> propertiesMap = new HashMap<>();
        if (frontendJson.containsKey("properties") && frontendJson.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> propMap = (Map<String, Object>) frontendJson.get("properties");
            propertiesMap.putAll(propMap);
        }

        // DataAddress extrahieren und konfigurieren
        String dataAddressType = "HttpData";
        String baseUrl = "";
        Boolean proxyPath = true;
        Boolean proxyQueryParams = true;

        if (frontendJson.containsKey("dataAddress") && frontendJson.get("dataAddress") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> daMap = (Map<String, Object>) frontendJson.get("dataAddress");

            // Typ und URL extrahieren
            if (daMap.containsKey("type") && daMap.get("type") instanceof String) {
                dataAddressType = (String) daMap.get("type");
            }

            // Handle different naming conventions for baseUrl
            if (daMap.containsKey("baseUrl") && daMap.get("baseUrl") instanceof String) {
                baseUrl = (String) daMap.get("baseUrl");
            } else if (daMap.containsKey("base_url") && daMap.get("base_url") instanceof String) {
                baseUrl = (String) daMap.get("base_url");
            } else if (daMap.containsKey("url") && daMap.get("url") instanceof String) {
                baseUrl = (String) daMap.get("url");
            }

            // Proxy-Einstellungen konfigurieren
            proxyPath = extractProxyPath(daMap);
            proxyQueryParams = extractProxyQueryParams(daMap);
        }
        
        // Extrahiere path, queryParams und headerParams aus daMap
        String dataAddressPath = null;
        Map<String, String> dataAddressQueryParams = null;
        String queryParamsString = null; // Für die direkte Weitergabe an EDC
        Map<String, String> dataAddressHeaderParams = new HashMap<>();
        
        if (frontendJson.containsKey("dataAddress") && frontendJson.get("dataAddress") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> daMap = (Map<String, Object>) frontendJson.get("dataAddress");
            
            // Pfad extrahieren
            if (daMap.containsKey("path") && daMap.get("path") instanceof String) {
                dataAddressPath = (String) daMap.get("path");
                LOG.info("Extracted path from dataAddress: " + dataAddressPath);
            }
            
            // Query-Parameter extrahieren - als String beibehalten (wie vom UI gesendet)
            if (daMap.containsKey("queryParams")) {
                Object queryParamsObj = daMap.get("queryParams");
                String rawQueryParamsStr = null;
                
                if (queryParamsObj instanceof String) {
                    // Direkt den String-Wert beibehalten
                    rawQueryParamsStr = (String) queryParamsObj;
                    // Für die Entität müssen wir es parsen
                    dataAddressQueryParams = parseQueryParamsString(rawQueryParamsStr);
                    LOG.info("Got queryParams string from frontend: " + rawQueryParamsStr);
                } else if (queryParamsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> paramsMap = (Map<String, String>) queryParamsObj;
                    dataAddressQueryParams = paramsMap;
                    // Auch als String speichern für das DTO
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        rawQueryParamsStr = objectMapper.writeValueAsString(paramsMap);
                    } catch (Exception e) {
                        LOG.warning("Fehler beim Konvertieren der Query-Parameter: " + e.getMessage());
                    }
                    LOG.info("Extracted queryParams from map");
                }
                
                // Den Raw-String für die Weitergabe an EDC speichern
                if (rawQueryParamsStr != null) {
                    // Variable für das aktuelle Objekt speichern
                    queryParamsString = rawQueryParamsStr;
                }
            }
            
            // Header-Parameter extrahieren - als einzelne Properties mit header: Präfix erwartet
            // Suche nach header: Präfix-Feldern (wie im UI gesetzt)
            for (Map.Entry<String, Object> entry : daMap.entrySet()) {
                if (entry.getKey().toLowerCase().startsWith("header:") && entry.getValue() instanceof String) {
                    String headerKey = entry.getKey().substring("header:".length());
                    dataAddressHeaderParams.put(headerKey, (String) entry.getValue());
                    LOG.info("Found header parameter: " + headerKey + "=" + entry.getValue());
                }
            }
            
            // Falls headerParams als Map vorhanden sind (für Abwärtskompatibilität)
            if (daMap.containsKey("headerParams")) {
                Object headerParamsObj = daMap.get("headerParams");
                if (headerParamsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> headersMap = (Map<String, String>) headerParamsObj;
                    dataAddressHeaderParams.putAll(headersMap);
                    LOG.info("Added headerParams from map");
                }
            }
        }

        // DataAddressDto erstellen
        EDCDataAddressDto dataAddressDto = new EDCDataAddressDto(
            null,                    // id
            "DataAddress",           // jsonLDType
            dataAddressType,         // type
            baseUrl,                 // baseUrl
            dataAddressPath,         // path
            queryParamsString,       // queryParams als String für EDC-Integration
            dataAddressHeaderParams.isEmpty() ? null : dataAddressHeaderParams, // headerParams
            proxyPath,               // proxyPath
            proxyQueryParams         // proxyQueryParams
        );

        // Sicherstellen, dass wir eine description haben
        String description = extractDescription(frontendJson, propertiesMap);
        
        // Standardwerte für Properties setzen, falls nicht vorhanden
        if (!propertiesMap.containsKey("asset:prop:description") && description != null && !description.isEmpty()) {
            propertiesMap.put("asset:prop:description", description);
        }
        
        // Name aus Properties oder direkt aus Frontend-JSON
        String name = null;
        if (propertiesMap.containsKey("asset:prop:name")) {
            Object nameObj = propertiesMap.get("asset:prop:name");
            if (nameObj instanceof String) {
                name = (String) nameObj;
            } else if (nameObj != null) {
                name = nameObj.toString();
            }
        } else if (frontendJson.containsKey("name") && frontendJson.get("name") instanceof String) {
            name = (String) frontendJson.get("name");
            // Auch in Properties speichern
            propertiesMap.put("asset:prop:name", name);
        }

        // Content-Type aus Properties extrahieren oder Standardwert verwenden
        String contentType = "application/json";
        if (propertiesMap.containsKey("asset:prop:contenttype")) {
            Object contentTypeObj = propertiesMap.get("asset:prop:contenttype");
            if (contentTypeObj instanceof String) {
                contentType = (String) contentTypeObj;
            } else if (contentTypeObj != null) {
                contentType = contentTypeObj.toString();
            }
        } else {
            // Standardwert in Properties speichern
            propertiesMap.put("asset:prop:contenttype", contentType);
        }
        
        // Typ und URL aus dataAddress extrahieren
        String url = baseUrl;
        String type = dataAddressType;

        // Erstellen des DTOs mit dem Record-Konstruktor
        return new EDCAssetDto(
            null,                   // id
            assetIdStr,             // assetId
            "Asset",                // jsonLDType
            null,                   // name wird aus properties-Map extrahiert
            url,                    // url
            type,                   // type
            contentType,            // contentType
            description,            // description
            edcId,                  // targetEDCId
            dataAddressDto,         // dataAddress
            propertiesMap,          // properties
            getDefaultContext()     // context
        );
    }

    /**
     * Hilfsmethode zum Extrahieren der ProxyPath-Einstellung aus einer Map.
     *
     * @param daMap Die Map mit den Proxy-Einstellungen
     * @return Der extrahierte Boolean-Wert oder true als Standard
     */
    private Boolean extractProxyPath(Map<String, Object> daMap) {
        if (daMap.containsKey("proxyPath")) {
            if (daMap.get("proxyPath") instanceof Boolean) {
                return (Boolean) daMap.get("proxyPath");
            } else if (daMap.get("proxyPath") instanceof String) {
                return Boolean.parseBoolean((String) daMap.get("proxyPath"));
            }
        }
        return true;
    }

    /**
     * Hilfsmethode zum Extrahieren der ProxyQueryParams-Einstellung aus einer Map.
     *
     * @param daMap Die Map mit den Proxy-Einstellungen
     * @return Der extrahierte Boolean-Wert oder true als Standard
     */
    private Boolean extractProxyQueryParams(Map<String, Object> daMap) {
        if (daMap.containsKey("proxyQueryParams")) {
            if (daMap.get("proxyQueryParams") instanceof Boolean) {
                return (Boolean) daMap.get("proxyQueryParams");
            } else if (daMap.get("proxyQueryParams") instanceof String) {
                return Boolean.parseBoolean((String) daMap.get("proxyQueryParams"));
            }
        }
        return true;
    }

    /**
     * Hilfsmethode zum Parsen eines URL-Query-Parameter-Strings in eine Map.
     * 
     * @param queryParamsStr Ein String im Format "key1=value1&key2=value2"
     * @return Eine Map mit den geparsten Query-Parametern
     */
    private Map<String, String> parseQueryParamsString(String queryParamsStr) {
        if (queryParamsStr == null || queryParamsStr.isEmpty()) {
            return null;
        }
        
        Map<String, String> paramsMap = new HashMap<>();
        String[] pairs = queryParamsStr.split("&");
        
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                
                try {
                    // URL-Decodierung der Parameter
                    key = URLDecoder.decode(key, StandardCharsets.UTF_8);
                    value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                    paramsMap.put(key, value);
                } catch (IllegalArgumentException e) {
                    // Bei Fehlern beim Decodieren, Original-Werte verwenden
                    paramsMap.put(key, value);
                    LOG.warning("Fehler beim Decodieren von URL-Parameter: " + e.getMessage());
                }
            }
        }
        
        return paramsMap;
    }

    /**
     * Hilfsmethode zum Extrahieren der Beschreibung aus verschiedenen Quellen.
     *
     * @param frontendJson  Die JSON-Daten aus dem Frontend
     * @param propertiesMap Die Properties-Map aus dem Frontend
     * @return Die extrahierte Beschreibung oder einen leeren String
     */
    private String extractDescription(Map<String, Object> frontendJson, Map<String, Object> propertiesMap) {
        String description = "";

        // Zuerst aus den Properties holen
        if (propertiesMap.containsKey("asset:prop:description")) {
            Object descObj = propertiesMap.get("asset:prop:description");
            if (descObj instanceof String) {
                description = (String) descObj;
            } else if (descObj != null) {
                description = descObj.toString();
            }
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
     * @param edcId    Die ID der EDC-Instanz
     * @param assetDto Das zu erstellende Asset
     * @return Das erstellte Asset als DTO
     * @throws CustomException Wenn die EDC-Instanz nicht existiert
     */
    @Transactional
    public EDCAssetDto createForEdc(Long edcId, EDCAssetDto assetDto) throws CustomException {
        // Prüfe, ob die EDC-Instanz existiert
        EdcInstance edcInstance = EdcInstance.findById(edcId);
        if (edcInstance == null) {
            throw new CustomException("Keine EDC-Instanz mit ID " + edcId + " gefunden");
        }

        // Erstelle ein neues DTO mit der EDC-ID aus dem Pfad
        EDCAssetDto newAssetDto = new EDCAssetDto(
            assetDto.id(),                // id 
            assetDto.assetId(),           // assetId
            assetDto.jsonLDType(),        // jsonLDType
            assetDto.name(),              // name
            assetDto.url(),               // url
            assetDto.type(),              // type
            assetDto.contentType(),       // contentType
            assetDto.description(),       // description
            edcId,                        // targetEDCId - immer die aus dem Pfad verwenden
            assetDto.dataAddress(),       // dataAddress
            assetDto.properties(),        // properties
            assetDto.context()            // context
        );

        // Asset über die Standardmethode erstellen
        return create(newAssetDto);
    }

    /**
     * Simuliert einen Datentransfer für ein Asset.
     *
     * @param assetId        Die ID des zu übertragenden Assets in der Datenbank
     * @param destinationUrl Die URL, an die die Daten übertragen werden sollen
     * @return Die Transfer-Prozess-ID als String
     * @throws CustomException Wenn der Datentransfer nicht initiiert werden kann
     */
    @Transactional
    public String initiateDataTransfer(Long assetId, String destinationUrl) throws CustomException {
        Asset asset = Asset.findById(assetId);

        if (asset == null) {
            throw new CustomException("Kein Asset mit ID " + assetId + " gefunden");
        }

        if (asset.targetEDC == null) {
            throw new CustomException("Keine Ziel-EDC-Instanz für Asset mit ID " + assetId + " angegeben");
        }

        try {
            // Simuliere Datentransfer-Initiierung
            String transferProcessId = "transfer-" + UUID.randomUUID();
            Log.info("Simuliere Datentransfer für Asset: " + asset.assetId + " zu URL: " + destinationUrl);
            Log.info("Simulierter Datentransfer initiiert mit Transfer-Prozess-ID: " + transferProcessId);
            return transferProcessId;
        } catch (Exception e) {
            throw new CustomException("Fehler beim Initiieren des Datentransfers: " + e.getMessage());
        }
    }

    /**
     * Simuliert die Überprüfung des Status eines Datentransfer-Prozesses.
     *
     * @param assetId           Die ID des Assets in der Datenbank
     * @param transferProcessId Die ID des zu überprüfenden Transfer-Prozesses
     * @return Der aktuelle Status des Transfer-Prozesses als String
     * @throws CustomException Wenn der Status nicht abgerufen werden kann
     */
    public String checkTransferStatus(Long assetId, String transferProcessId) throws CustomException {
        Asset asset = Asset.findById(assetId);

        if (asset == null) {
            throw new CustomException("Kein Asset mit ID " + assetId + " gefunden");
        }

        if (asset.targetEDC == null) {
            throw new CustomException("Keine Ziel-EDC-Instanz für Asset mit ID " + assetId + " angegeben");
        }

        try {
            // Simuliere Status-Überprüfung
            Log.info("Simuliere Statusüberprüfung für Transfer-Prozess: " + transferProcessId);
            return "COMPLETED";  // Für Testzwecke immer COMPLETED zurückgeben
        } catch (Exception e) {
            throw new CustomException("Fehler beim Überprüfen des Transfer-Status: " + e.getMessage());
        }
    }
    
    /**
     * Ruft Daten basierend auf einem Asset ab.
     * Unterstützt jetzt auch erweiterte Parameter wie Pfad, Query-Parameter und Header-Parameter.
     *
     * @param assetId Die ID des Assets
     * @return Die abgerufenen Daten als String
     * @throws CustomException Wenn Fehler beim Abrufen auftreten
     */
    public String fetchAssetData(Long assetId) throws CustomException {
        Asset asset = Asset.findById(assetId);
        if (asset == null) {
            throw new CustomException("Kein Asset mit ID " + assetId + " gefunden");
        }
        
        if (asset.dataAddress == null) {
            throw new CustomException("Asset hat keine DataAddress");
        }
        
        try {
            return httpDataFetcherService.fetchData(asset.dataAddress);
        } catch (Exception e) {
            throw new CustomException("Fehler beim Abrufen der Daten: " + e.getMessage(), e);
        }
    }
}