package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;

import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;
import de.unistuttgart.stayinsync.core.configuration.edc.service.PolicyDefinitionService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;



@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PolicyDefinitionResource extends EdcEntityResource<PolicyDefinitionDto> {

    @Inject
    PolicyDefinitionService service;

    @GET
    @Path("{edcId}/policies/{id}")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response getEntity(@PathParam("edcId") final Long edcId, @PathParam("id") final Long policyDefinitionId) {
        if(policyDefinitionId == null){
            return handleNullArgument();
        }
        try {
            final PolicyDefinitionDto policyDefinition = service.getEntityWithSyncCheck(policyDefinitionId);
            Log.debugf("Found policy definition with its id", policyDefinitionId);
            return Response.status(Response.Status.OK)
                    .entity(policyDefinition)
                    .build();

        } catch(EntityNotFoundException e){
            return handleNotFoundException(policyDefinitionId);

        } catch(EntityFetchingException e){
            Log.errorf("Error fetching policy definition: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch policy definition: " + e.getMessage())
                    .build();

        }
    }

    @GET
    @Path("{edcId}/policies")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response getListOfEntities(@PathParam("edcId") final Long edcId) {
        if(edcId == null){
            return handleNullArgument();
        }
        try{
            final List<PolicyDefinitionDto> policyDefinitionDtos = service.getEntitiesAsListWithSyncCheck(edcId);
            Log.debug("Policy definitions successfully fetched.");
            return Response.status(Response.Status.OK)
                    .entity(policyDefinitionDtos)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(edcId);
        } catch(EntityFetchingException e){
            Log.errorf("Error fetching policy definitions: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch policy definitions: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("{edcId}/policies")
    @JsonView(VisibilitySidesForDto.Ui.class)
    @Override
    public Response createEntity(@PathParam("edcId") final Long edcId, final PolicyDefinitionDto policyDefinitionToCreateDto) {
        if(edcId == null || policyDefinitionToCreateDto == null){
            return handleNullArgument();
        }
        try{
            final PolicyDefinitionDto createdPolicyDefinitionDto = service.createEntityInDatabaseAndEdc(edcId, policyDefinitionToCreateDto);
            Log.infof("Policy definition successfully created", createdPolicyDefinitionDto.policyDefinitionId());
            return Response.status(Response.Status.CREATED)
                    .entity(createdPolicyDefinitionDto)
                    .build();

        } catch(EntityCreationFailedException e){
            final String exceptionMessage = "The policy definition could not be created: " + e.getMessage();
            Log.errorf(exceptionMessage, policyDefinitionToCreateDto);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(edcId);
        }
    }

    @PUT
    @Path("{edcId}/policies/{id}")
    @Transactional
    public Response updateEntity(@PathParam("edcId") final Long edcId, @PathParam("id") final Long policyDefinitionId, final PolicyDefinitionDto policyDefinitionToUpdateDto) {
        if(policyDefinitionId == null || policyDefinitionToUpdateDto == null){
            return handleNullArgument();
        }
        try{
            PolicyDefinitionDto updatedPolicyDefinition = service.updateEntityInDatabaseAndEdc(policyDefinitionId, policyDefinitionToUpdateDto);
            Log.infof("Policy definition successfully updated", policyDefinitionId);
            return Response.status(Response.Status.OK)
                    .entity(updatedPolicyDefinition)
                    .build();

        } catch(EntityNotFoundException e){
            return handleNotFoundException(policyDefinitionId);

        } catch(EntityUpdateFailedException e){
            final String exceptionMessage = "The policy definition could not be updated: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }

    @DELETE
    @Path("{edcId}/policies/{id}")
    @Transactional
    public Response deleteEntity(@PathParam("edcId") final Long edcId, @PathParam("id") final Long policyDefinitionId) {
        if(policyDefinitionId == null){
            return handleNullArgument();
        }
        try{
            service.deleteEntityFromDatabaseAndEdc(policyDefinitionId);
            Log.infof("Policy definition successfully deleted. ");
            return Response.status(Response.Status.OK)
                    .build();
        } catch(EntityNotFoundException e){
            return handleNotFoundException(policyDefinitionId);

        } catch (EntityDeletionFailedException e){
            final String exceptionMessage = "Error deleting the policy definition: " + e.getMessage();
            Log.errorf(exceptionMessage);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionMessage)
                    .build();

        }
    }
}
