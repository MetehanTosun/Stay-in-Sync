package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;

@ApplicationScoped
public class EDCAssetService {

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
}
