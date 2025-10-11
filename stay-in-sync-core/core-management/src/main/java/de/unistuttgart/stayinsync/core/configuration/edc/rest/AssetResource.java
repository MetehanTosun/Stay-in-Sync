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
public class AssetResource extends EdcEntityResource<AssetDto>{

    @Inject
    AssetService service;
    
    @GET
    @Path("/{edcId}/assets/{assetId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response getEntity(@PathParam("edcId") final Long edcId, @PathParam("assetId") final Long id) {
        if(id == null){
            return handleNullArgument();
        }
        try {
            final AssetDto asset = service.getEntityWithSyncCheck(id);
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

    @GET
    @Path("/{edcId}/assets")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response getListOfEntities(@PathParam("edcId") final Long edcId) {
        if(edcId == null){
            return handleNullArgument();
        }
        try{
            final List<AssetDto> assetDtos = service.getEntitiesAsListWithSyncCheck(edcId);
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

    @POST
    @Path("/{edcId}/assets")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response createEntity(@PathParam("edcId") final Long edcId, final AssetDto assetToCreate) {
        if(edcId == null || assetToCreate == null){
            return handleNullArgument();
        }
        try{
            final AssetDto createdAssetDto = service.createEntityInDatabaseAndEdc(edcId, assetToCreate);
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

    @Override
    @PUT
    @Path("/{edcId}/assets/{assetId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response updateEntity(@PathParam("edcId") final Long edcId, @PathParam("assetId") final Long id, final AssetDto assetToUpdate) {
        if(id == null || assetToUpdate == null){
            return handleNullArgument();
        }
        try{
            AssetDto updatedAsset = service.updateEntityInDatabaseAndEdc(id,assetToUpdate);
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


    @Override
    @DELETE
    @Path("/{edcId}/assets/{assetId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response deleteEntity(@PathParam("edcId") final Long edcId, @PathParam("assetId") final Long id) {
        if(id == null){
            return handleNullArgument();
        }
        try{
            service.deleteEntityFromDatabaseAndEdc(id);
            Log.infof("Asset successfully deleted. ");
            return Response.status(Response.Status.OK)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(id);

        } catch (EntityDeletionFailedException e){
            final String exceptionMessage = "Error deleting the asset: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }



}
