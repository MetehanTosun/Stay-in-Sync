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
            LOG.info("Received DTO: " + dto);
            
            EdcInstance edcInstance = EdcInstance.findById(edcId);
            if (edcInstance == null) {
                LOG.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("EDC instance not found: " + edcId)
                              .build();
            }
            
            // Validiere, dass entweder assetId oder asset gesetzt ist
            if (dto.assetId() == null || dto.assetId().isBlank()) {
                LOG.error("Asset ID is required");
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity("Asset ID is required")
                              .build();
            }
            
            // Validiere, dass mindestens eine Policy angegeben ist
            if ((dto.accessPolicyId() == null && (dto.accessPolicyIdStr() == null || dto.accessPolicyIdStr().isBlank())) ||
                (dto.contractPolicyId() == null && (dto.contractPolicyIdStr() == null || dto.contractPolicyIdStr().isBlank()))) {
                LOG.error("Access policy and contract policy are required");
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity("Both access policy and contract policy are required")
                              .build();
            }
            
            LOG.info("Converting DTO to entity");
            ContractDefinition entity = EDCContractDefinitionMapper.fromDto(dto);
            
            // Validiere, dass die Entity-Referenzen korrekt aufgelöst wurden
            if (entity.asset == null) {
                LOG.error("Asset with ID " + dto.assetId() + " not found");
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("Asset with ID " + dto.assetId() + " not found")
                              .build();
            }
            
            if (entity.accessPolicy == null) {
                String policyId = dto.accessPolicyIdStr() != null ? dto.accessPolicyIdStr() : 
                                 (dto.accessPolicyId() != null ? dto.accessPolicyId().toString() : "null");
                LOG.error("Access Policy with ID " + policyId + " not found");
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("Access Policy with ID " + policyId + " not found")
                              .build();
            }
            
            if (entity.contractPolicy == null) {
                String policyId = dto.contractPolicyIdStr() != null ? dto.contractPolicyIdStr() : 
                                 (dto.contractPolicyId() != null ? dto.contractPolicyId().toString() : "null");
                LOG.error("Contract Policy with ID " + policyId + " not found");
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("Contract Policy with ID " + policyId + " not found")
                              .build();
            }
            
            entity.edcInstance = edcInstance;
            LOG.info("Saving contract definition entity");
            
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
    public Response deleteContractDefinitionForEdc(@PathParam("edcId") Long edcId, @PathParam("id") String idParam) {
        try {
            LOG.info("Deleting contract definition with parameter: " + idParam + " for EDC: " + edcId);
            
            // Validiere Parameter
            if (edcId == null || edcId <= 0) {
                LOG.warn("Invalid EDC ID: " + edcId);
                return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid EDC ID: " + edcId)
                            .build();
            }
            
            if (idParam == null || idParam.isEmpty()) {
                LOG.warn("Invalid Contract Definition ID: " + idParam);
                return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid Contract Definition ID: " + idParam)
                            .build();
            }
            
            Optional<ContractDefinition> contractDef;
            
            // Prüfe, ob der Parameter eine Zahl (DB-ID) oder ein String (contractDefinitionId) ist
            if (idParam.matches("\\d+")) {
                // Wenn der Parameter eine Zahl ist, behandele ihn als Datenbank-ID
                Long id = Long.parseLong(idParam);
                LOG.info("Parameter scheint eine Datenbank-ID zu sein: " + id);
                contractDef = service.findByIdAndEdcId(id, edcId);
            } else {
                // Andernfalls behandele ihn als contractDefinitionId (String)
                LOG.info("Parameter scheint eine contractDefinitionId zu sein: " + idParam);
                contractDef = service.findByContractDefinitionIdAndEdcId(idParam, edcId);
            }
            
            if (contractDef.isEmpty()) {
                LOG.warn("Contract definition " + idParam + " not found for EDC " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("Contract definition " + idParam + " not found for EDC " + edcId)
                              .build();
            }
            
            ContractDefinition contractDefEntity = contractDef.get();
            LOG.info("Found contract definition to delete: id=" + contractDefEntity.id + 
                     ", contractDefinitionId=" + contractDefEntity.getContractDefinitionId());
            
            if (service.delete(contractDefEntity.id)) {
                LOG.info("Contract definition " + idParam + " (DB-ID: " + contractDefEntity.id + ") deleted successfully");
                
                // Verify the contract definition is actually gone from the database
                Optional<ContractDefinition> checkDeleted = service.findByIdAndEdcId(contractDefEntity.id, edcId);
                if (checkDeleted.isPresent()) {
                    LOG.error("Contract definition " + idParam + " (DB-ID: " + contractDefEntity.id + ") still exists in database after deletion!");
                } else {
                    LOG.info("Verified contract definition " + idParam + " (DB-ID: " + contractDefEntity.id + ") is no longer in database");
                }
                
                return Response.noContent().build();
            } else {
                LOG.error("Failed to delete contract definition " + idParam + " (DB-ID: " + contractDefEntity.id + ")");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                              .entity("Failed to delete contract definition " + idParam)
                              .build();
            }
        } catch (Exception e) {
            LOG.error("Error deleting contract definition", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Error deleting contract definition: " + e.getMessage())
                          .build();
        }
    }
}
