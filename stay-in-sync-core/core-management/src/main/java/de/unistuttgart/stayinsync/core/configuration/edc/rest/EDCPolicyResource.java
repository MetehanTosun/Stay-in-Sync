package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCPolicyMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCPolicyService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCPolicyResource {

    private static final Logger LOG = Logger.getLogger(EDCPolicyResource.class);

    @Inject
    EDCPolicyService service;

    @GET
    @Path("{edcId}/policies")
    public List<EDCPolicyDto> listPoliciesForEdc(@PathParam("edcId") UUID edcId) {
        LOG.info("Fetching policies for EDC: " + edcId);
        List<EDCPolicyDto> policies = service.listAllByEdcId(edcId).stream()
                .map(entity -> EDCPolicyMapper.toDto(entity)
                        .orElseGet(() -> {
                            LOG.warn("Konnte Policy nicht konvertieren: " + entity.id);
                            return new EDCPolicyDto();
                        }))
                .collect(Collectors.toList());
        LOG.info("Returning " + policies.size() + " policies for EDC: " + edcId);
        return policies;
    }

    @GET
    @Path("{edcId}/policies/{id}")
    public EDCPolicyDto getPolicyForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id) {
        LOG.info("Fetching policy " + id + " for EDC: " + edcId);
        return service.findByIdAndEdcId(id, edcId)
                .flatMap(entity -> EDCPolicyMapper.toDto(entity))
                .orElseThrow(() -> new NotFoundException("Policy " + id + " not found for EDC " + edcId));
    }

  @POST
  @Path("{edcId}/policies")
  @Transactional
  public Response createPolicyForEdc(@PathParam("edcId") UUID edcId, EDCPolicyDto dto, @Context UriInfo uriInfo) {
      try {
          LOG.info("Creating policy for EDC: " + edcId);
          // Normalisieren bevor gemappt wird (falls verschachtelt übergeben)
          normalizePolicy(dto);
          
          // Generiere automatisch eine Policy-ID, wenn keine vorhanden ist
          if (dto.getPolicyId() == null || dto.getPolicyId().isEmpty()) {
              String generatedPolicyId = "policy-" + System.currentTimeMillis();
              LOG.info("No policy ID provided, generating one: " + generatedPolicyId);
              dto.setPolicyId(generatedPolicyId);
              
              // Aktualisiere auch die ID im Policy-Objekt, falls vorhanden
              if (dto.getPolicy() != null && dto.getPolicy() instanceof Map<?, ?>) {
                  Map<String, Object> policyMap = (Map<String, Object>) dto.getPolicy();
                  policyMap.put("@id", generatedPolicyId);
              }
          }

          EDCPolicy entity = EDCPolicyMapper.fromDto(dto);
          entity.setEdcInstance(EDCInstance.findById(edcId));
          
          if (entity.getEdcInstance() == null) {
              LOG.error("EDC instance not found: " + edcId);
              return Response.status(Response.Status.NOT_FOUND)
                             .entity("EDC instance not found: " + edcId)
                             .build();
          }
          
          EDCPolicy created = service.create(entity);
          Optional<EDCPolicyDto> resultOpt = EDCPolicyMapper.toDto(created);
          
          if (resultOpt.isEmpty()) {
              LOG.error("Failed to convert created policy to DTO");
              return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                             .entity("Failed to convert created policy to DTO")
                             .build();
          }
          
          EDCPolicyDto result = resultOpt.get();
          
          URI uri = uriInfo.getAbsolutePathBuilder()
                  .path(result.getId().toString())
                  .build();
                  
          LOG.info("Policy created successfully with ID: " + result.getId());
          return Response.created(uri).entity(result).build();
      } catch (Exception e) {
          LOG.error("Error creating policy", e);
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity("Error creating policy: " + e.getMessage())
                         .build();
      }
  }


  @PUT
  @Path("{edcId}/policies/{id}")
  @Transactional
  public Response updatePolicyForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id, EDCPolicyDto dto) {
      try {
          LOG.info("Updating policy " + id + " for EDC: " + edcId);
          dto.setId(id);
          normalizePolicy(dto);

          EDCPolicy newState = EDCPolicyMapper.fromDto(dto);
          newState.setEdcInstance(EDCInstance.findById(edcId));
          
          if (newState.getEdcInstance() == null) {
              LOG.error("EDC instance not found: " + edcId);
              return Response.status(Response.Status.NOT_FOUND)
                             .entity("EDC instance not found: " + edcId)
                             .build();
          }
          
          Optional<EDCPolicy> updated = service.findByIdAndEdcId(id, edcId)
                  .map(entity -> {
                      entity.setPolicyId(newState.getPolicyId());
                      entity.setPolicyJson(newState.getPolicyJson());
                      entity.setEdcInstance(newState.getEdcInstance());
                      return entity;
                  });
                  
          if (updated.isEmpty()) {
              LOG.error("Policy " + id + " not found for EDC " + edcId);
              return Response.status(Response.Status.NOT_FOUND)
                             .entity("Policy " + id + " not found for EDC " + edcId)
                             .build();
          }
          
          try {
              Optional<EDCPolicyDto> resultOpt = EDCPolicyMapper.toDto(updated.get());
              if (resultOpt.isEmpty()) {
                  LOG.error("Failed to convert updated policy to DTO");
                  return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                 .entity("Failed to convert updated policy to DTO")
                                 .build();
              }
              
              EDCPolicyDto result = resultOpt.get();
              LOG.info("Policy updated successfully: " + result.getId());
              return Response.ok(result).build();
          } catch (Exception e) {
              LOG.error("Error during policy conversion", e);
              return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                             .entity("Error during policy conversion: " + e.getMessage())
                             .build();
          }
      } catch (Exception e) {
          LOG.error("Error updating policy", e);
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity("Error updating policy: " + e.getMessage())
                         .build();
      }
  }


  @DELETE
  @Path("{edcId}/policies/{id}")
  @Transactional
  public Response deletePolicyForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id) {
      LOG.info("Deleting policy " + id + " for EDC: " + edcId);
      
      Optional<EDCPolicy> policy = service.findByIdAndEdcId(id, edcId);
      if (policy.isEmpty()) {
          LOG.warn("Policy " + id + " not found for EDC " + edcId);
          return Response.status(Response.Status.NOT_FOUND)
                         .entity("Policy " + id + " not found for EDC " + edcId)
                         .build();
      }
      
      EDCPolicy policyEntity = policy.get();
      LOG.info("Found policy to delete: id=" + policyEntity.id + ", policyId=" + policyEntity.getPolicyId());
      
      if (service.delete(id)) {
          LOG.info("Policy " + id + " deleted successfully");
          
          // Verify the policy is actually gone from the database
          Optional<EDCPolicy> checkDeleted = service.findByIdAndEdcId(id, edcId);
          if (checkDeleted.isPresent()) {
              LOG.error("Policy " + id + " still exists in database after deletion!");
          } else {
              LOG.info("Verified policy " + id + " is no longer in database");
          }
          
          return Response.noContent().build();
      } else {
          LOG.error("Failed to delete policy " + id);
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity("Failed to delete policy " + id)
                         .build();
      }
  }


    @SuppressWarnings("unchecked")
    private static void normalizePolicy(EDCPolicyDto dto) {
        if (dto == null || dto.getPolicy() == null) return;

        Object nested = dto.getPolicy().get("policy");
        if (nested instanceof Map) {
            // Cast sicher, weil Map<String,Object>
            Map<String, Object> nestedMapCasted = (Map<String, Object>) nested;

            // permission hochziehen
            if (nestedMapCasted.containsKey("permission") && !dto.getPolicy().containsKey("permission")) {
                dto.getPolicy().put("permission", nestedMapCasted.get("permission"));
            }

            // optional: context & id hochziehen
            if (nestedMapCasted.containsKey("@context") && !dto.getPolicy().containsKey("@context")) {
                dto.getPolicy().put("@context", nestedMapCasted.get("@context"));
            }
            if (nestedMapCasted.containsKey("@id") && !dto.getPolicy().containsKey("@id")) {
                dto.getPolicy().put("@id", nestedMapCasted.get("@id"));
            }

            // nested entfernen
            dto.getPolicy().remove("policy");
        }
    }

}
