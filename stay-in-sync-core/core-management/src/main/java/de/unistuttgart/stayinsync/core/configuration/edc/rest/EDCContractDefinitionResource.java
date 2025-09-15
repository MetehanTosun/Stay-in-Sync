package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCContractDefinitionMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCContractDefinitionService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "EDCContractDefinition", description = "Verwaltet Vertragsdefinitionen")
public class EDCContractDefinitionResource {

    private static final Logger LOG = Logger.getLogger(EDCContractDefinitionResource.class);

    @Inject
    EDCContractDefinitionService service;

    @GET
    @Path("{edcId}/contract-definitions")
    public List<EDCContractDefinitionDto> getContractDefinitionsForEdc(@PathParam("edcId") UUID edcId) {
        LOG.info("Fetching contract definitions for EDC: " + edcId);
        List<EDCContractDefinitionDto> contractDefs = service.listAllByEdcId(edcId).stream()
                .map(EDCContractDefinitionMapper::toDto)
                .collect(Collectors.toList());
        LOG.info("Returning " + contractDefs.size() + " contract definitions for EDC: " + edcId);
        return contractDefs;
    }

    @GET
    @Path("{edcId}/contract-definitions/{id}")
    public EDCContractDefinitionDto getContractDefinitionForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id) {
        LOG.info("Fetching contract definition " + id + " for EDC: " + edcId);
        return service.findByIdAndEdcId(id, edcId)
                .map(EDCContractDefinitionMapper::toDto)
                .orElseThrow(() -> new NotFoundException("ContractDefinition " + id + " not found for EDC " + edcId));
    }

    @POST
    @Path("{edcId}/contract-definitions")
    @Transactional
    public Response createContractDefinitionForEdc(@PathParam("edcId") UUID edcId, EDCContractDefinitionDto dto, @Context UriInfo uriInfo) {
        try {
            LOG.info("Creating contract definition for EDC: " + edcId);
            
            EDCInstance edcInstance = EDCInstance.findById(edcId);
            if (edcInstance == null) {
                LOG.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("EDC instance not found: " + edcId)
                              .build();
            }
            
            EDCContractDefinition entity = EDCContractDefinitionMapper.fromDto(dto);
            entity.setEdcInstance(edcInstance);
            
            // Persist with the owning EDC instance set to avoid detached references
            EDCContractDefinition created = service.create(entity);
            EDCContractDefinitionDto createdDto = EDCContractDefinitionMapper.toDto(created);
            
            URI uri = uriInfo.getAbsolutePathBuilder()
                             .path(createdDto.getId().toString())
                             .build();
            
            LOG.info("Contract definition created successfully with ID: " + createdDto.getId());
            return Response.created(uri)
                          .entity(createdDto)
                          .build();
        } catch (Exception e) {
            LOG.error("Error creating contract definition", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Error creating contract definition: " + e.getMessage())
                          .build();
        }
    }

    @PUT
    @Path("{edcId}/contract-definitions/{id}")
    @Transactional
    public Response updateContractDefinitionForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id, EDCContractDefinitionDto dto) {
        try {
            LOG.info("Updating contract definition " + id + " for EDC: " + edcId);
            dto.setId(id);
            
            EDCInstance edcInstance = EDCInstance.findById(edcId);
            if (edcInstance == null) {
                LOG.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("EDC instance not found: " + edcId)
                              .build();
            }
            
            EDCContractDefinition entity = EDCContractDefinitionMapper.fromDto(dto);
            entity.setEdcInstance(edcInstance);
            
            Optional<EDCContractDefinition> updated = service.update(id, entity);
            if (updated.isEmpty()) {
                LOG.error("Contract definition " + id + " not found for EDC " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("Contract definition " + id + " not found for EDC " + edcId)
                              .build();
            }
            
            EDCContractDefinitionDto updatedDto = EDCContractDefinitionMapper.toDto(updated.get());
            LOG.info("Contract definition updated successfully: " + updatedDto.getId());
            return Response.ok(updatedDto).build();
        } catch (Exception e) {
            LOG.error("Error updating contract definition", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Error updating contract definition: " + e.getMessage())
                          .build();
        }
    }

    @DELETE
    @Path("{edcId}/contract-definitions/{id}")
    @Transactional
    public Response deleteContractDefinitionForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id) {
        LOG.info("Deleting contract definition " + id + " for EDC: " + edcId);
        
        Optional<EDCContractDefinition> contractDef = service.findByIdAndEdcId(id, edcId);
        if (contractDef.isEmpty()) {
            LOG.warn("Contract definition " + id + " not found for EDC " + edcId);
            return Response.status(Response.Status.NOT_FOUND)
                          .entity("Contract definition " + id + " not found for EDC " + edcId)
                          .build();
        }
        
        if (service.delete(id)) {
            LOG.info("Contract definition " + id + " deleted successfully");
            return Response.noContent().build();
        } else {
            LOG.error("Failed to delete contract definition " + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Failed to delete contract definition " + id)
                          .build();
        }
    }
}
