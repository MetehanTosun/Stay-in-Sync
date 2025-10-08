package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;
import de.unistuttgart.stayinsync.core.configuration.edc.service.AssetService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;


/**
 * REST-Ressource for asset administration that supports the standard CRUD-Endpoints to manage assets in the database and the Edc-Instances.
 */
@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssetResource {

    @Inject
    AssetService service;

    /**
     * Gets an asset from the service and returns it to the caller as a response.
     * @param edcId NOT USED SHOULD BE DELETED
     * @param id used to find the asset in the database
     * @return response with asset or error status
     */
    @GET
    @Path("/{edcId}/assets/{assetId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response getAsset(@PathParam("edcId") final Long edcId, @PathParam("assetId") final Long id) {
        if(id == null){
            return handleNullArgument();
        }
        try {
            final AssetDto asset = service.getAssetAndCheckForThirdPartyChanges(id);
            Log.debugf("Found asset with its id", id);
            return Response.status(Response.Status.OK)
                    .entity(asset)
                    .build();

        } catch(EntityNotFoundException e){
            return handleNotFoundException(id);

        } catch(EntityFetchingException e){
            Log.errorf("Error fetching asset: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch asset: " + e.getMessage())
                    .build();

        }
    }

    /**
     * Gets all Assets for a specific EDC-Instance
     *
     * @param edcId the id of the edc Instance
     * @return response with list of assets.
     */
    @GET
    @Path("/{edcId}/assets")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response getAssets(@PathParam("edcId") final Long edcId) {
        if(edcId == null){
            return handleNullArgument();
        }
        try{
            final List<AssetDto> assetDtos = service.listAllAndCheckForThirdPartyChanges(edcId);
            Log.debug("Asset successfully fetched.");
            return Response.status(Response.Status.OK)
                    .entity(assetDtos)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(edcId);
        } catch(EntityFetchingException e){
            Log.errorf("Error fetching assets: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch assets: " + e.getMessage())
                    .build();
        }

    }

    /**
     * Calls Service to create asset in edc and database.
     * @param edcId used to find the edc to which the asset should be uploaded.
     * @param assetToCreate contains the data to create the asset
     * @return response containing the data of the created asset if successful
     */
    @POST
    @Path("/{edcId}/assets")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response createAsset(@PathParam("edcId") final Long edcId, final AssetDto assetToCreate) {
        if(edcId == null || assetToCreate == null){
            return handleNullArgument();
        }
        try{
            final AssetDto createdAssetDto = service.create(edcId, assetToCreate);
            Log.infof("Asset successfully created", createdAssetDto.id());
            return Response.status(Response.Status.CREATED)
                    .entity(createdAssetDto)
                    .build();

        } catch(EntityCreationFailedException e){
            final String exceptionMessage = "The Asset could not be created: " + e.getMessage();
            Log.errorf(exceptionMessage, assetToCreate);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(edcId);
        }
    }

    /**
     * Updates Asset in Edc and Database.
     * @param edcId NOT USED SHOULD BE REMOVED
     * @param id used to find the asset in the database
     * @param assetToUpdate contains the data to update the asset in edc and database.
     * @return response with the data of the updated asset
     */
    @PUT
    @Path("/{edcId}/assets/{assetId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response updateAsset(@PathParam("edcId") final Long edcId, @PathParam("assetId") final Long id, final AssetDto assetToUpdate) {
        if(id == null || assetToUpdate == null){
            return handleNullArgument();
        }
        try{
            AssetDto updatedAsset = service.update(id,assetToUpdate);
            Log.infof("Asset successfully updated", id);
            return Response.status(Response.Status.OK)
                    .entity(updatedAsset)
                    .build();

        } catch(EntityNotFoundException e){
            return handleNotFoundException(id);

        } catch(EntityUpdateFailedException e){
            final String exceptionMessage = "The Asset could not be updated: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }

    /**
     * Removes Asset from its edc and the database.
     *
     * @param edcId NOT USED AND SHOULD BE DELETED
     * @param id to find the asset in the database
     * @return response with DELETED status
     */
    @DELETE
    @Path("/{edcId}/assets/{assetId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response deleteAsset(@PathParam("edcId") final Long edcId, @PathParam("assetId") final Long id) {
        if(id == null){
            return handleNullArgument();
        }
        try{
            service.delete(id);
            Log.infof("Asset successfully deleted. ");
            return Response.status(Response.Status.OK)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(id);

        } catch (EntityDeletionException e){
            final String exceptionMessage = "Error deleting the asset: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }

    private Response handleNotFoundException(final Long id){
        final String exceptionMessage = "The Entity was not found with the id in the database.";
        Log.warnf(exceptionMessage, id);
        return Response.status(Response.Status.NOT_FOUND)
                .entity(exceptionMessage)
                .build();
    }

    private Response handleNullArgument(){
            final String exceptionMessage = "Invalid Id. Can not be null.";
            Log.warnf(exceptionMessage);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(exceptionMessage)
                    .build();
    }

}
