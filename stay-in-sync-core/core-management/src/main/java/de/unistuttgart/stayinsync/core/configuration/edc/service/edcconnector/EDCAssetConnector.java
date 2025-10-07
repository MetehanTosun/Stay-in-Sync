package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.AssetDataAddress;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.AssetProperties;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.ConnectionToEdcFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.RequestBuildingException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.RequestExecutionException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.ResponseSubscriptionException;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

import java.util.List;

public class EDCAssetConnector {

    EDCRequestBuilder requestBuilder;

    EDCRestClient restClient;

    public void getAssetFromEdc(){

    }

    public List<Asset> getAllAssetsFromEdc(final EDCInstance edcInstance) throws ConnectionToEdcFailedException{
        try {
            final JsonObject listOfJsonAssets = restClient.executeEdcCall(requestBuilder.buildRequest(edcInstance, "GET", edcInstance.getEdcAssetEndpoint()), null);
            return this.convertJsonObjectToAsset(listOfJsonAssets);
        } catch(RequestBuildingException | ResponseSubscriptionException | RequestExecutionException e){
            final String exceptionMessage = "It was not possible to retrieve the Assets.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        }

    }

    /**
     * Creates Asset on Edc.
     * @param asset contains the data to upload on the edc.
     * @throws ConnectionToEdcFailedException if the asset was not created on the edc.
     */
    public void createAssetOnEdc(final Asset asset) throws ConnectionToEdcFailedException{
        try {

            final HttpRequest<Buffer> request = requestBuilder.buildRequest(asset.getTargetEDC(), "POST", asset.getTargetEDC().getEdcAssetEndpoint());
            restClient.executeEdcCall(request, asset.getContent());
            Log.debugf("Successfully created the Asset on the EDC");

        } catch(RequestBuildingException e) {
            final String exceptionMessage = "No valid HttpRequest could be built with the provided Details.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        } catch(RequestExecutionException | ResponseSubscriptionException e) {
            final String exceptionMessage = "Failed to connect to the EdC to create the asset.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        }
    }

    /**
     * Updates Asset on Edc.
     * @param asset contains the data to upload on the edc.
     * @throws ConnectionToEdcFailedException if the asset was not updated on the edc.
     */
    public void updateAssetOnEdc(final Asset asset) throws ConnectionToEdcFailedException{
        try {
            final HttpRequest<Buffer> request = requestBuilder.buildRequest(asset.getTargetEDC(), "PUT", asset.getTargetEDC().getEdcAssetEndpoint());
            restClient.executeEdcCall(request, asset.getContent());
            Log.debugf("Successfully updated the Asset on the EDC");

        } catch(RequestBuildingException e) {
            final String exceptionMessage = "No valid HttpRequest could be built with the provided Details for asset update.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        } catch(RequestExecutionException | ResponseSubscriptionException e) {
            final String exceptionMessage = "Failed to connect to the EDC to update the asset.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        }
    }

    public void removeAssetFromEdc(final Asset asset) throws ConnectionToEdcFailedException{
        try {
            String deleteEndpoint = asset.getTargetEDC().getEdcAssetEndpoint() + "/" + asset.id;
            final HttpRequest<Buffer> request = requestBuilder.buildRequest(asset.getTargetEDC(), "DELETE", deleteEndpoint);
            restClient.executeEdcCall(request, null);

            Log.debugf("Successfully removed the Asset from the EDC");

        } catch(RequestBuildingException e) {
            final String exceptionMessage = "No valid HttpRequest could be built with the provided Details for asset deletion.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        } catch(RequestExecutionException | ResponseSubscriptionException e) {
            final String exceptionMessage = "Failed to connect to the EDC to remove the asset.";
            Log.errorf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        }
    }

    /**
     * Konvertiert ein einzelnes Asset-JsonObject in ein EDCAsset.
     *
     * @param assetJson Das JsonObject eines einzelnen Assets
     * @param edcInstance Die EDC-Instanz
     * @return EDCAsset oder null bei Fehlern
     */
    private Asset convertJsonToSingleAsset(JsonObject assetJson, EDCInstance edcInstance) {
        try {
            // Asset-ID extrahieren (EDC verwendet @id)
            String assetId = assetJson.getString("@id");
            if (assetId == null) {
                assetId = assetJson.getString("id"); // Fallback
            }

            if (assetId == null || assetId.isEmpty()) {
                Log.warn("Asset without ID found, skipping: " + assetJson.toString());
                return null;
            }

            // Properties extrahieren
            JsonObject propertiesJson = assetJson.getJsonObject("properties");
            AssetProperties properties = convertJsonToProperties(propertiesJson, assetId);

            // DataAddress extrahieren
            JsonObject dataAddressJson = assetJson.getJsonObject("dataAddress");
            AssetDataAddress dataAddress = convertJsonToDataAddress(dataAddressJson);

            // Asset erstellen
            Asset asset = new Asset();
            asset.setAssetId(assetId);
            asset.setTargetEDC(edcInstance);
            asset.setProperties(properties);
            asset.setDataAddress(dataAddress);

            // URL und Type aus DataAddress übernehmen
            if (dataAddress != null) {
                asset.setUrl(dataAddress.getBaseUrl());
                asset.setType(dataAddress.getType());
            }

            // ContentType und Description aus Properties übernehmen
            if (properties != null) {
                asset.setContentType(properties.getContentType());
                asset.setDescription(properties.getDescription());
            }

            Log.debug("Successfully converted asset: " + assetId);
            return asset;

        } catch (Exception e) {
            Log.error("Error converting single asset JSON: " + assetJson.toString(), e);
            return null;
        }
    }

    //I´m not sure yet if I should make one method convertJsonObjectToAsset that returns a lsit whcih I use for both cases (one asset returned or all assets returned)
    // where the user needs to extract the first one from the lsit or just two methods were the user picks the one he needs.

}
