package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.ContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EdcInstance;
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
import java.util.stream.Collectors;

@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "EDCContractDefinition", description = "Verwaltet Vertragsdefinitionen")
public class EDCContractDefinitionResource {

    private static final Logger LOG = Logger.getLogger(EDCContractDefinitionResource.class);

    @Inject
    EDCContractDefinitionService service;
    
    @Inject
    EDCContractDefinitionMapper mapper;

    @GET
    @Path("{edcId}/contract-definitions")
    public List<EDCContractDefinitionDto> getContractDefinitionsForEdc(@PathParam("edcId") Long edcId) {
        LOG.info("Fetching contract definitions for EDC: " + edcId);
        List<EDCContractDefinitionDto> contractDefs = service.listAllByEdcId(edcId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        LOG.info("Returning " + contractDefs.size() + " contract definitions for EDC: " + edcId);
        return contractDefs;
    }

    @GET
    @Path("{edcId}/contract-definitions/{id}")
    public EDCContractDefinitionDto getContractDefinitionForEdc(@PathParam("edcId") Long edcId, @PathParam("id") Long id) {
        LOG.info("Fetching contract definition " + id + " for EDC: " + edcId);
        return service.findByIdAndEdcId(id, edcId)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("ContractDefinition " + id + " not found for EDC " + edcId));
    }

    @POST
    @Path("{edcId}/contract-definitions")
    @Transactional
    public Response createContractDefinitionForEdc(@PathParam("edcId") Long edcId, EDCContractDefinitionDto dto, @Context UriInfo uriInfo) {
        try {
            LOG.info("Creating contract definition for EDC: " + edcId);
            
            EdcInstance edcInstance = EdcInstance.findById(edcId);
            if (edcInstance == null) {
                LOG.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("EDC instance not found: " + edcId)
                              .build();
            }
            
            ContractDefinition entity = EDCContractDefinitionMapper.fromDto(dto);
            entity.edcInstance = edcInstance;
            
            // Persist with the owning EDC instance set to avoid detached references
            ContractDefinition created = service.create(entity);
            EDCContractDefinitionDto createdDto = mapper.toDto(created);
            
            URI uri = uriInfo.getAbsolutePathBuilder()
                             .path(createdDto.id().toString())
                             .build();
            
            LOG.info("Contract definition created successfully with ID: " + createdDto.id());
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
    public Response updateContractDefinitionForEdc(@PathParam("edcId") Long edcId, @PathParam("id") Long id, EDCContractDefinitionDto dto) {
        try {
            LOG.info("Updating contract definition " + id + " for EDC: " + edcId);
            // Create a new DTO with updated id - only use the fields available in the record
            dto = new EDCContractDefinitionDto(
                id, 
                dto.contractDefinitionId(),
                dto.assetId(),
                dto.rawJson(),
                dto.accessPolicyId(),
                dto.accessPolicyIdStr(),
                dto.contractPolicyId(),
                dto.contractPolicyIdStr()
            );
            
            EdcInstance edcInstance = EdcInstance.findById(edcId);
            if (edcInstance == null) {
                LOG.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("EDC instance not found: " + edcId)
                              .build();
            }
            
            ContractDefinition entity = EDCContractDefinitionMapper.fromDto(dto);
            entity.edcInstance = edcInstance;
            
            Optional<ContractDefinition> updated = service.update(id, entity);
            if (updated.isEmpty()) {
                LOG.error("Contract definition " + id + " not found for EDC " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("Contract definition " + id + " not found for EDC " + edcId)
                              .build();
            }
            
            EDCContractDefinitionDto updatedDto = mapper.toDto(updated.get());
            LOG.info("Contract definition updated successfully: " + updatedDto.id());
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
    public Response deleteContractDefinitionForEdc(@PathParam("edcId") Long edcId, @PathParam("id") Long id) {
        LOG.info("Deleting contract definition " + id + " for EDC: " + edcId);
        
        Optional<ContractDefinition> contractDef = service.findByIdAndEdcId(id, edcId);
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
