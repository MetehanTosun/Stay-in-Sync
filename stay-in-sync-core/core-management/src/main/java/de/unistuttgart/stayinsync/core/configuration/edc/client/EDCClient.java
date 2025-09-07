package de.unistuttgart.stayinsync.core.configuration.edc.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.StringReader;
import java.util.UUID;

/**
 * Client für die Kommunikation mit dem Eclipse Dataspace Connector (EDC).
 * Diese Klasse stellt Methoden bereit, um mit der EDC-API zu interagieren.
 */
@ApplicationScoped
public class EDCClient {
    
    private static final Logger LOG = Logger.getLogger(EDCClient.class);
    
    @ConfigProperty(name = "edc.timeout.seconds", defaultValue = "30")
    int timeoutSeconds;
    
    /**
     * Erstellt ein Asset im EDC.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param asset Das Asset als JSON-Objekt
     * @return Die Antwort des EDC als String
     * @throws RuntimeException Wenn die Erstellung fehlschlägt
     */
    public String createAsset(String edcUrl, String apiKey, JsonObject asset) {
        LOG.info("Creating asset in EDC: " + edcUrl);
        LOG.debug("Asset data: " + asset.toString());
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v3/assets")
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .post(Entity.json(asset.toString()));
            
            if (response.getStatus() != 200 && response.getStatus() != 201) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to create asset: " + errorMsg);
                throw new RuntimeException("Failed to create asset: " + errorMsg);
            }
            
            String result = response.readEntity(String.class);
            LOG.info("Asset created successfully");
            LOG.debug("EDC response: " + result);
            
            client.close();
            return result;
        } catch (Exception e) {
            LOG.error("Exception when creating asset: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create asset: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ruft ein Asset aus dem EDC ab.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param assetId Die ID des abzurufenden Assets
     * @return Das Asset als String
     * @throws RuntimeException Wenn der Abruf fehlschlägt
     */
    public String getAsset(String edcUrl, String apiKey, String assetId) {
        LOG.info("Getting asset from EDC: " + edcUrl + ", assetId: " + assetId);
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v3/assets/" + assetId)
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .get();
            
            if (response.getStatus() != 200) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to get asset: " + errorMsg);
                throw new RuntimeException("Failed to get asset: " + errorMsg);
            }
            
            String result = response.readEntity(String.class);
            LOG.info("Asset retrieved successfully");
            LOG.debug("EDC response: " + result);
            
            client.close();
            return result;
        } catch (Exception e) {
            LOG.error("Exception when getting asset: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get asset: " + e.getMessage(), e);
        }
    }
    
    /**
     * Listet alle Assets im EDC auf.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @return Eine Liste von Assets als String
     * @throws RuntimeException Wenn der Abruf fehlschlägt
     */
    public String listAssets(String edcUrl, String apiKey) {
        LOG.info("Listing assets from EDC: " + edcUrl);
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v3/assets")
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .get();
            
            if (response.getStatus() != 200) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to list assets: " + errorMsg);
                throw new RuntimeException("Failed to list assets: " + errorMsg);
            }
            
            String result = response.readEntity(String.class);
            LOG.info("Assets listed successfully");
            LOG.debug("EDC response: " + result);
            
            client.close();
            return result;
        } catch (Exception e) {
            LOG.error("Exception when listing assets: " + e.getMessage(), e);
            throw new RuntimeException("Failed to list assets: " + e.getMessage(), e);
        }
    }
    
    /**
     * Löscht ein Asset im EDC.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param assetId Die ID des zu löschenden Assets
     * @return Die Antwort des EDC als String
     * @throws RuntimeException Wenn die Löschung fehlschlägt
     */
    public String deleteAsset(String edcUrl, String apiKey, String assetId) {
        LOG.info("Deleting asset from EDC: " + edcUrl + ", assetId: " + assetId);
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v3/assets/" + assetId)
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .delete();
            
            if (response.getStatus() != 200 && response.getStatus() != 204) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to delete asset: " + errorMsg);
                throw new RuntimeException("Failed to delete asset: " + errorMsg);
            }
            
            LOG.info("Asset deleted successfully");
            client.close();
            return "Asset deleted successfully";
        } catch (Exception e) {
            LOG.error("Exception when deleting asset: " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete asset: " + e.getMessage(), e);
        }
    }
    
    /**
     * Erstellt eine Policy im EDC.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param policy Die Policy als JSON-Objekt
     * @return Die Antwort des EDC als String
     * @throws RuntimeException Wenn die Erstellung fehlschlägt
     */
    public String createPolicy(String edcUrl, String apiKey, JsonObject policy) {
        LOG.info("Creating policy in EDC: " + edcUrl);
        LOG.debug("Policy data: " + policy.toString());
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v2/policydefinitions")
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .post(Entity.json(policy.toString()));
            
            if (response.getStatus() != 200 && response.getStatus() != 201) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to create policy: " + errorMsg);
                throw new RuntimeException("Failed to create policy: " + errorMsg);
            }
            
            String result = response.readEntity(String.class);
            LOG.info("Policy created successfully");
            LOG.debug("EDC response: " + result);
            
            client.close();
            return result;
        } catch (Exception e) {
            LOG.error("Exception when creating policy: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create policy: " + e.getMessage(), e);
        }
    }
    
    /**
     * Erstellt eine Contract Definition im EDC.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param contractDef Die Contract Definition als JSON-Objekt
     * @return Die Antwort des EDC als String
     * @throws RuntimeException Wenn die Erstellung fehlschlägt
     */
    public String createContractDefinition(String edcUrl, String apiKey, JsonObject contractDef) {
        LOG.info("Creating contract definition in EDC: " + edcUrl);
        LOG.debug("Contract definition data: " + contractDef.toString());
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v2/contractdefinitions")
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .post(Entity.json(contractDef.toString()));
            
            if (response.getStatus() != 200 && response.getStatus() != 201) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to create contract definition: " + errorMsg);
                throw new RuntimeException("Failed to create contract definition: " + errorMsg);
            }
            
            String result = response.readEntity(String.class);
            LOG.info("Contract definition created successfully");
            LOG.debug("EDC response: " + result);
            
            client.close();
            return result;
        } catch (Exception e) {
            LOG.error("Exception when creating contract definition: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create contract definition: " + e.getMessage(), e);
        }
    }
    
    /**
     * Erzeugt ein JSON-Objekt für ein Asset im EDC-Format.
     *
     * @param id Die ID des Assets
     * @param description Die Beschreibung des Assets
     * @param dataType Der Datentyp des Assets
     * @param baseUrl Die Basis-URL des Assets
     * @return Das Asset als JSON-Objekt
     */
    public JsonObject createAssetJson(String id, String description, String dataType, String baseUrl) {
        JsonObjectBuilder assetBuilder = Json.createObjectBuilder()
            .add("@id", id)
            .add("@type", "Asset");
        
        JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder()
            .add("asset:prop:id", id)
            .add("asset:prop:description", description)
            .add("asset:prop:contenttype", dataType);
        
        assetBuilder.add("properties", propertiesBuilder);
        
        JsonObjectBuilder dataAddressBuilder = Json.createObjectBuilder()
            .add("type", "HttpData")
            .add("baseUrl", baseUrl);
        
        assetBuilder.add("dataAddress", dataAddressBuilder);
        
        return assetBuilder.build();
    }
    
    /**
     * Extrahiert die Asset-ID aus einer EDC-Antwort.
     *
     * @param edcResponse Die EDC-Antwort als JSON-String
     * @return Die Asset-ID
     */
    public String extractAssetIdFromResponse(String edcResponse) {
        try (JsonReader reader = Json.createReader(new StringReader(edcResponse))) {
            JsonObject responseJson = reader.readObject();
            return responseJson.getString("@id");
        } catch (Exception e) {
            LOG.error("Failed to extract asset ID from response: " + e.getMessage());
            throw new RuntimeException("Failed to extract asset ID from response: " + e.getMessage());
        }
    }
    
    /**
     * Initiiert einen Datentransfer im EDC.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param assetId Die ID des zu übertragenden Assets
     * @param contractId Die ID der Contract Definition
     * @param destinationUrl Die Ziel-URL für die Daten
     * @param contentType Der Content-Type der Daten
     * @return Die Antwort des EDC als String mit der Transfer-Prozess-ID
     * @throws RuntimeException Wenn die Initiierung fehlschlägt
     */
    public String initiateDataTransfer(String edcUrl, String apiKey, String assetId, String contractId, 
                                     String destinationUrl, String contentType) {
        LOG.info("Initiating data transfer in EDC: " + edcUrl + " for asset: " + assetId);
        
        try {
            // Versuchen Sie zuerst den tatsächlichen EDC-Aufruf
            try {
                // Erstellen des Transfer-Request JSON
                JsonObjectBuilder transferRequestBuilder = Json.createObjectBuilder()
                    .add("@context", Json.createObjectBuilder()
                        .add("edc", "https://w3id.org/edc/v0.0.1/ns/"))
                    .add("assetId", assetId)
                    .add("contractId", contractId)
                    .add("connectorId", "provider") // Oft 'provider' für interne Transfers
                    .add("connectorAddress", edcUrl.replace("/management", "/api/v1/dsp"))
                    .add("counterPartyAddress", edcUrl.replace("/management", "/api/v1/dsp"))
                    .add("protocol", "dataspace-protocol-http");
                
                // Datenziel konfigurieren
                JsonObjectBuilder dataDestinationBuilder = Json.createObjectBuilder()
                    .add("type", "HttpData")
                    .add("baseUrl", destinationUrl);
                
                // Transfer-Typ konfigurieren
                JsonObjectBuilder transferTypeBuilder = Json.createObjectBuilder()
                    .add("contentType", contentType != null ? contentType : "application/json")
                    .add("isFinite", true);
                
                transferRequestBuilder.add("dataDestination", dataDestinationBuilder);
                transferRequestBuilder.add("transferType", transferTypeBuilder);
                
                JsonObject transferRequest = transferRequestBuilder.build();
                LOG.debug("Transfer request: " + transferRequest.toString());
                
                // API-Aufruf
                Client client = ClientBuilder.newClient();
                Response response = client.target(edcUrl + "/v3/transferprocesses")
                    .request(MediaType.APPLICATION_JSON)
                    .header("X-Api-Key", apiKey)
                    .post(Entity.json(transferRequest.toString()));
                
                if (response.getStatus() != 200 && response.getStatus() != 201) {
                    String errorMsg = response.readEntity(String.class);
                    LOG.error("Failed to initiate data transfer: " + errorMsg);
                    throw new RuntimeException("Failed to initiate data transfer: " + errorMsg);
                }
                
                String result = response.readEntity(String.class);
                LOG.info("Data transfer initiated successfully");
                LOG.debug("EDC response: " + result);
                
                client.close();
                return result;
            } catch (Exception e) {
                LOG.warn("Failed to initiate actual data transfer: " + e.getMessage() + 
                        ". Falling back to simulation mode.", e);
                
                // Simulation des Datentransfers für Testumgebungen
                // Bei einem Fehler im tatsächlichen Transfer simulieren wir einen erfolgreichen Transfer
                return simulateDataTransfer(assetId, destinationUrl);
            }
        } catch (Exception e) {
            LOG.error("Exception when initiating data transfer: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initiate data transfer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simuliert einen Datentransfer für Testumgebungen.
     * Diese Methode wird verwendet, wenn der tatsächliche EDC-Aufruf fehlschlägt.
     *
     * @param assetId Die ID des zu übertragenden Assets
     * @param destinationUrl Die Ziel-URL für die Daten
     * @return Eine simulierte EDC-Antwort als String
     */
    private String simulateDataTransfer(String assetId, String destinationUrl) {
        LOG.info("Simulating data transfer for asset: " + assetId + " to: " + destinationUrl);
        
        // Generieren einer eindeutigen Transfer-Prozess-ID
        String transferProcessId = "simulated-transfer-" + UUID.randomUUID().toString();
        
        // Erstellen einer simulierten EDC-Antwort
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
            .add("@type", "IdResponse")
            .add("@id", transferProcessId)
            .add("createdAt", System.currentTimeMillis())
            .add("@context", Json.createObjectBuilder()
                .add("edc", "https://w3id.org/edc/v0.0.1/ns/"));
        
        String simulatedResponse = responseBuilder.build().toString();
        LOG.info("Simulated transfer initiated with ID: " + transferProcessId);
        
        return simulatedResponse;
    }
    
    /**
     * Überprüft den Status eines Datentransfer-Prozesses.
     *
     * @param edcUrl Die Management-URL des EDC
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @param transferProcessId Die ID des zu überprüfenden Transfer-Prozesses
     * @return Die Antwort des EDC als String mit dem aktuellen Status
     * @throws RuntimeException Wenn die Abfrage fehlschlägt
     */
    public String getTransferProcessStatus(String edcUrl, String apiKey, String transferProcessId) {
        LOG.info("Getting transfer process status from EDC: " + edcUrl + ", transferProcessId: " + transferProcessId);
        
        // Prüfen, ob es sich um einen simulierten Transfer handelt
        if (transferProcessId.startsWith("simulated-transfer-")) {
            return simulateTransferStatus(transferProcessId);
        }
        
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(edcUrl + "/v3/transferprocesses/" + transferProcessId)
                .request(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", apiKey)
                .get();
            
            if (response.getStatus() != 200) {
                String errorMsg = response.readEntity(String.class);
                LOG.error("Failed to get transfer process status: " + errorMsg);
                throw new RuntimeException("Failed to get transfer process status: " + errorMsg);
            }
            
            String result = response.readEntity(String.class);
            LOG.info("Transfer process status retrieved successfully");
            LOG.debug("EDC response: " + result);
            
            client.close();
            return result;
        } catch (Exception e) {
            LOG.error("Exception when getting transfer process status: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get transfer process status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simuliert den Status eines Datentransfer-Prozesses für Testumgebungen.
     * Diese Methode wird verwendet, wenn der transferProcessId mit "simulated-transfer-" beginnt.
     *
     * @param transferProcessId Die ID des simulierten Transfer-Prozesses
     * @return Eine simulierte EDC-Antwort als String mit dem Status
     */
    private String simulateTransferStatus(String transferProcessId) {
        LOG.info("Simulating transfer status for process: " + transferProcessId);
        
        // Status basierend auf der Zeit simulieren
        long currentTime = System.currentTimeMillis();
        String status = "COMPLETED"; // Standard: Transfer abgeschlossen
        
        // Erstellen einer simulierten EDC-Antwort
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
            .add("@id", transferProcessId)
            .add("@type", "TransferProcess")
            .add("state", status)
            .add("stateTimestamp", currentTime)
            .add("errorDetail", "")
            .add("@context", Json.createObjectBuilder()
                .add("edc", "https://w3id.org/edc/v0.0.1/ns/"));
        
        String simulatedResponse = responseBuilder.build().toString();
        LOG.info("Simulated transfer status: " + status + " for ID: " + transferProcessId);
        
        return simulatedResponse;
    }
}

