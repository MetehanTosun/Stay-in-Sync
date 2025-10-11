package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;
import de.unistuttgart.stayinsync.core.configuration.edc.service.ContractDefinitionService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;

/**
 * REST-Ressource for contract definition administration that supports the standard CRUD-Endpoints to manage contract definitions in the database and the Edc-Instances.
 */
@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractDefinitionResource extends EdcEntityResource<ContractDefinitionDto> {

    @Inject
    ContractDefinitionService service;

    @GET
    @Path("/{edcId}/contractdefinitions/{contractDefinitionId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response getEntity(@PathParam("edcId") final Long edcId, @PathParam("contractDefinitionId") final Long id) {
        if (id == null) {
            return handleNullArgument();
        }
        try {
            final ContractDefinitionDto contractDefinition = service.getEntityWithSyncCheck(id);
            Log.debugf("Found contract definition with its id", id);
            return Response.status(Response.Status.OK)
                    .entity(contractDefinition)
                    .build();

        } catch (EntityNotFoundException e) {
            return handleNotFoundException(id);

        } catch (EntityFetchingException e) {
            Log.errorf("Error fetching contract definition: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch contract definition: " + e.getMessage())
                    .build();

        }
    }

    @GET
    @Path("/{edcId}/contractdefinitions")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response getListOfEntities(@PathParam("edcId") final Long edcId) {
        if (edcId == null) {
            return handleNullArgument();
        }
        try {
            final List<ContractDefinitionDto> contractDefinitionDtos = service.getEntitiesAsListWithSyncCheck(edcId);
            Log.debug("Contract definitions successfully fetched.");
            return Response.status(Response.Status.OK)
                    .entity(contractDefinitionDtos)
                    .build();
        } catch (EntityNotFoundException e) {
            return handleNotFoundException(edcId);
        } catch (EntityFetchingException e) {
            Log.errorf("Error fetching contract definitions: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch contract definitions: " + e.getMessage())
                    .build();
        }

    }

    @POST
    @Path("/{edcId}/contractdefinitions")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response createEntity(@PathParam("edcId") final Long edcId, final ContractDefinitionDto contractDefinitionToCreate) {
        if (edcId == null || contractDefinitionToCreate == null) {
            return handleNullArgument();
        }
        try {
            final ContractDefinitionDto createdContractDefinitionDto = service.createEntityInDatabaseAndEdc(edcId, contractDefinitionToCreate);
            Log.infof("ContractDefinition successfully created", createdContractDefinitionDto.contractDefinitionId());
            return Response.status(Response.Status.CREATED)
                    .entity(createdContractDefinitionDto)
                    .build();

        } catch (EntityCreationFailedException e) {
            final String exceptionMessage = "The ContractDefinition could not be created: " + e.getMessage();
            Log.errorf(exceptionMessage, contractDefinitionToCreate);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();
        } catch (EntityNotFoundException e) {
            return handleNotFoundException(edcId);
        }
    }

    @Override
    @PUT
    @Path("/{edcId}/contractdefinitions/{contractDefinitionId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response updateEntity(@PathParam("edcId") final Long edcId, @PathParam("contractDefinitionId") final Long id, final ContractDefinitionDto contractDefinitionToUpdate) {
        if (id == null || contractDefinitionToUpdate == null) {
            return handleNullArgument();
        }
        try {
            ContractDefinitionDto updatedContractDefinition = service.updateEntityInDatabaseAndEdc(id, contractDefinitionToUpdate);
            Log.infof("ContractDefinition successfully updated", id);
            return Response.status(Response.Status.OK)
                    .entity(updatedContractDefinition)
                    .build();

        } catch (EntityNotFoundException e) {
            return handleNotFoundException(id);

        } catch (EntityUpdateFailedException e) {
            final String exceptionMessage = "The ContractDefinition could not be updated: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }

    @Override
    @DELETE
    @Path("/{edcId}/contractdefinitions/{contractDefinitionId}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    public Response deleteEntity(@PathParam("edcId") final Long edcId, @PathParam("contractDefinitionId") final Long id) {
        if (id == null) {
            return handleNullArgument();
        }
        try {
            service.deleteEntityFromDatabaseAndEdc(id);
            Log.infof("ContractDefinition successfully deleted. ");
            return Response.status(Response.Status.OK)
                    .build();
        } catch (EntityNotFoundException e) {
            return handleNotFoundException(id);

        } catch (EntityDeletionFailedException e) {
            final String exceptionMessage = "Error deleting the contract definition: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }

}