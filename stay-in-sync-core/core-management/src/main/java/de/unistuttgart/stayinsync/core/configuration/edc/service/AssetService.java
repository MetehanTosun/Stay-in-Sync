package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.client.AssetEdcClient;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.AssetMapper;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service-Class to manage Asset objects.
 * Makes sure that if the state of an asset is changed in the database these changes are represented in its corresponding edc too.
 * If assets are fetched from the database then they are compared to their one self from the edc. If the states of both versions are inconsistent their boolean thirdPartyChanges is changed to true.
 */
@ApplicationScoped
public class AssetService extends EdcEntityService<AssetDto>{

    @Transactional
    @Override
    public AssetDto getEntityWithSyncCheck(final Long id) throws EntityNotFoundException, EntityFetchingException {
        final Asset persistedAsset = this.getAssetFromDatabase(id);
        final EDCInstance edcOfAsset = persistedAsset.getTargetEdc();
        final AssetEdcClient client = AssetEdcClient.createClient(edcOfAsset.getControlPlaneManagementUrl());
        try {

            final AssetDto fetchedAssetFromEdc = this.extractAssetDtosFromResponse(client.getAssetById(edcOfAsset.getApiKey(), persistedAsset.getAssetId())).getFirst();
            final AssetDto persistedAssetAsDto = AssetMapper.mapper.entityToDto(persistedAsset);
            persistedAsset.setEntityOutOfSync(!persistedAssetAsDto.equals(fetchedAssetFromEdc));
            return persistedAssetAsDto;

        } catch (DatabaseEntityOutOfSyncException e) {
            Log.warnf(e.getMessage(), persistedAsset.getAssetId());
            persistedAsset.setEntityOutOfSync(true);
            return AssetMapper.mapper.entityToDto(persistedAsset);

        } catch (AuthorizationFailedException | ResponseInvalidFormatException | ConnectionToEdcFailedException e) {
            Log.errorf(e.getMessage(), persistedAsset.getAssetId());
            throw new EntityFetchingException(e.getMessage());

        }
    }

    @Transactional
    @Override
    public List<AssetDto> getEntitiesAsListWithSyncCheck(final Long edcId) throws EntityNotFoundException, EntityFetchingException {
        final EDCInstance edcInstance = getEdcInstanceFromDatabase(edcId);
        final List<Asset> persistedAssetsForEdcInstance = getAllAssetsForEdcInstanceFromDatabase(edcInstance);
        try {
            this.checkForAllAssetsForThirdPartyChanges(persistedAssetsForEdcInstance, edcInstance);
            return persistedAssetsForEdcInstance.stream().map(AssetMapper.mapper::entityToDto).toList();

        } catch (AuthorizationFailedException | ConnectionToEdcFailedException | ResponseInvalidFormatException | DatabaseEntityOutOfSyncException e) {
            Log.errorf("AssetList fetching from Edc failed with message: " + e.getMessage());
            throw new EntityFetchingException("AssetList fetching from Edc failed with message: " + e.getMessage());
        }
    }


    @Transactional
    @Override
    public AssetDto createEntityInDatabaseAndEdc(final Long edcId, final AssetDto assetDto) throws EntityNotFoundException, EntityCreationFailedException {
        final EDCInstance assetsEdc = getEdcInstanceFromDatabase(edcId);
        final Asset assetToPersist = AssetMapper.mapper.dtoToEntity(assetDto);
        AssetEdcClient client = AssetEdcClient.createClient(assetToPersist.getTargetEdc().getControlPlaneManagementUrl());
        try {
            final AssetDto uploadedAsset = this.extractAssetDtosFromResponse(client.createAsset(assetToPersist.getTargetEdc().getApiKey(), assetDto)).getFirst();
            if (uploadedAsset.equals(AssetMapper.mapper.entityToDto(assetToPersist))) {
                assetToPersist.setTargetEdc(assetsEdc);
                Asset.persist(assetToPersist);
            } else {
                Log.warnf("The Asset was created on the edc, but its information on the edc did not match the information " +
                        "in the database during a later check. Database asset entry is now updated based on the information for it on the edc.");
                assetToPersist.updateValuesWithAssetDto(uploadedAsset);
                Asset.persist(assetToPersist);
            }
            return uploadedAsset;
        } catch (ResponseInvalidFormatException | DatabaseEntityOutOfSyncException | AuthorizationFailedException | ConnectionToEdcFailedException e) {
            Log.errorf("Asset creation from Edc failed with message: " + e.getMessage());
            throw new EntityCreationFailedException("Asset Creation failed with message: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public AssetDto updateEntityInDatabaseAndEdc(final Long assetId, final AssetDto updatedAssetDto) throws EntityNotFoundException, EntityUpdateFailedException {
        final Asset persistedAsset = this.getAssetFromDatabase(assetId);
        final AssetEdcClient client = AssetEdcClient.createClient(persistedAsset.getTargetEdc().getControlPlaneManagementUrl());
        try {
            final AssetDto returnedAssetAfterUpdateOnEdc = extractAssetDtosFromResponse(client.updateAsset(persistedAsset.getTargetEdc().getApiKey(), persistedAsset.getAssetId(), updatedAssetDto)).getFirst();
            persistedAsset.updateValuesWithAssetDto(returnedAssetAfterUpdateOnEdc);
            return AssetMapper.mapper.entityToDto(persistedAsset);
        } catch (ResponseInvalidFormatException | DatabaseEntityOutOfSyncException | AuthorizationFailedException |
                 ConnectionToEdcFailedException e) {
            throw new EntityUpdateFailedException("Update of Asset failed because it could not be uploaded to the EDC " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void deleteEntityFromDatabaseAndEdc(final Long id) throws EntityNotFoundException, EntityDeletionFailedException {
        Asset assetToDelete = this.getAssetFromDatabase(id);
        AssetEdcClient client = AssetEdcClient.createClient(assetToDelete.getTargetEdc().getControlPlaneManagementUrl());
        try (RestResponse<Void> response = client.deleteAsset(
                assetToDelete.getTargetEdc().getApiKey(),
                assetToDelete.getAssetId()
        )) {
            if (response.getStatus() == 200) {
                Asset.deleteById(id);
                Log.infof("Asset successfully deleted from Edc and Database.", id);
            } else {
                final String exceptionMessage = "Asset could not be deleted from edc.";
                Log.errorf(exceptionMessage, id);
                throw new EntityDeletionFailedException(exceptionMessage);
            }
        }
    }

    /**
     * Returns asset from database if it´s found with id. In other case EntityNotFoundException is thrown.
     *
     * @param id used to find the asset
     * @return the asset if it´s found.
     * @throws EntityNotFoundException if no asset was found.
     */
    private Asset getAssetFromDatabase(final Long id) throws EntityNotFoundException {
        final Asset asset = Asset.findById(id);
        if (asset == null) {
            final String exceptionMessage = "Asset could not be found with the given id.";
            Log.errorf(exceptionMessage, id);
            throw new EntityNotFoundException(exceptionMessage);
        }
        return asset;
    }

    /**
     * Returns all assets from database that are part of the given edcInstance.
     *
     * @param edcInstance used to filter assets for specifically this edc.
     * @return all assets for the given edc.
     */
    private List<Asset> getAllAssetsForEdcInstanceFromDatabase(final EDCInstance edcInstance) {
        final List<Asset> allAssets = Asset.listAll();
        return allAssets.stream()
                .filter(asset -> asset.getTargetEdc().equals(edcInstance))
                .toList();
    }

    /**
     * Returns edcInstance from database if it´s found with id. In other case EntityNotFoundException is thrown.
     *
     * @param id used to find the edcInstance
     * @return the edcInstance if it´s found.
     * @throws EntityNotFoundException if no edcInstance was found.
     */
    private EDCInstance getEdcInstanceFromDatabase(final Long id) throws EntityNotFoundException {
        final EDCInstance edcInstance = EDCInstance.findById(id);
        if (edcInstance == null) {
            final String exceptionMessage = "EdcInstance could not be found with the given id.";
            Log.errorf(exceptionMessage, id);
            throw new EntityNotFoundException(exceptionMessage);
        }
        return edcInstance;
    }

    private void checkForAllAssetsForThirdPartyChanges(final List<Asset> assetsToCheck, final EDCInstance edcInstance) throws AuthorizationFailedException, DatabaseEntityOutOfSyncException, ConnectionToEdcFailedException, ResponseInvalidFormatException {
        final Map<String, AssetDto> edcAssetDtosMappedToOwnAssetIds = extractAssetDtosFromResponse(AssetEdcClient.createClient(edcInstance.getControlPlaneManagementUrl()).getAllAssets(edcInstance.getApiKey()))
                .stream()
                .collect(Collectors.toMap(AssetDto::assetId, asset -> asset));

        for (Asset persistedAsset : assetsToCheck) {
            final AssetDto persistedAssetAsDto = AssetMapper.mapper.entityToDto(persistedAsset);
            final AssetDto edcAssetDto = edcAssetDtosMappedToOwnAssetIds.get(persistedAssetAsDto.assetId());
            persistedAsset.setEntityOutOfSync(!persistedAssetAsDto.equals(edcAssetDto));
        }
    }

    /**
     * Extracts AssetDtos from EDC response.
     * Always returns a List (empty for DELETE, single element for GET/POST/PUT, multiple for Post query).
     *
     * @param response the REST response from EDC
     * @return list of AssetDto
     * @throws ResponseInvalidFormatException,DatabaseEntityOutOfSyncException,AuthorizationFailedException,ConnectionToEdcFailedException if response code indicates error or body is invalid
     */
    private List<AssetDto> extractAssetDtosFromResponse(final RestResponse<Object> response) throws
            ResponseInvalidFormatException, DatabaseEntityOutOfSyncException, AuthorizationFailedException, ConnectionToEdcFailedException {
        final int status = response.getStatus();
        this.handleNegativeResponseCodes(status);

        final boolean acceptableResponse = status >= 200 && status < 300;
        final boolean deletionStatusOrEmptyResponse = status == 204 || response.getEntity() == null;

        if (acceptableResponse) {
            if (deletionStatusOrEmptyResponse) {
                return List.of();
            }
            return parseJsonToAssetDtoList(response.getEntity());
        }

        throw new ResponseInvalidFormatException(
                "Edc asset request failed with status " + status);
    }




    /**
     * Parses JsonObject into list of AssetDtos.
     * Handles two formats:
     * 1. Direct Array: [{Asset}, {Asset}, ...] (from query/getAll)
     * 2. Single Object: {Asset} (from get/create/update)
     *
     * @param json the JSON response
     * @return list of parsed AssetDtos
     * @throws ResponseInvalidFormatException if format is invalid
     */
    private List<AssetDto> parseJsonToAssetDtoList(final Object json) throws ResponseInvalidFormatException {
        try {
            if (json instanceof JsonArray jsonArray) {
                return jsonArray.stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(jsonObj -> {
                            try {
                                return parseJsonToAssetDto(jsonObj);
                            } catch (ResponseInvalidFormatException e) {
                                Log.error("Failed to parse one asset in JSON array", e);
                                return null;
                            }
                        })
                        .toList();
            }

            if (json instanceof JsonObject jsonObject) {
                if (jsonObject.containsKey("@type") || jsonObject.containsKey("@id")) {
                    return List.of(parseJsonToAssetDto(jsonObject));
                }
            }

            throw new ResponseInvalidFormatException("Unexpected EDC response format - neither Asset object nor Asset array");

        } catch (ClassCastException e) {
            throw new ResponseInvalidFormatException("Failed to parse EDC response structure", e);
        }
    }

    /**
     * Parses a single JsonObject into an AssetDto.
     */
    private AssetDto parseJsonToAssetDto(final JsonObject jsonObject)
            throws ResponseInvalidFormatException {
        try {
            return jsonObject.mapTo(AssetDto.class);
        } catch (IllegalArgumentException e) {
            Log.error("Failed to parse JSON to AssetDto: " + jsonObject.encode(), e);
            throw new ResponseInvalidFormatException("Failed to parse JSON to AssetDto", e);
        }
    }

}
