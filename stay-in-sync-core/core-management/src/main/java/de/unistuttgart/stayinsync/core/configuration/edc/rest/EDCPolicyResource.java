package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCPolicyMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCPolicyService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST-Ressource für die Verwaltung von EDC-Policies.
 * 
 * Diese Klasse stellt Endpunkte für CRUD-Operationen auf EDC-Policies bereit.
 * Die Policies sind einem bestimmten EDC (Eclipse Dataspace Connector) zugeordnet.
 */
@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCPolicyResource {

    @Inject
    EDCPolicyService service;
    
    @Inject
    EDCPolicyMapper mapper;

    /**
     * Listet alle Policies für eine bestimmte EDC-Instanz auf.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @return Eine Liste aller Policies für diese EDC-Instanz
     */
    @GET
    @Path("{edcId}/policies")
    public List<EDCPolicyDto> listPoliciesForEdc(@PathParam("edcId") Long edcId) {
        Log.info("Fetching policies for EDC: " + edcId);
        List<EDCPolicy> policies = service.listAllByEdcId(edcId);
        
        List<EDCPolicyDto> policyDtos = policies.stream()
                .map(mapper::policyToPolicyDto)
                .collect(Collectors.toList());
                
        Log.info("Returning " + policyDtos.size() + " policies for EDC: " + edcId);
        return policyDtos;
    }

    /**
     * Ruft eine bestimmte Policy für eine EDC-Instanz ab.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param id Die ID der Policy
     * @return Die gefundene Policy
     * @throws NotFoundException Wenn die Policy nicht gefunden wird
     */
    @GET
    @Path("{edcId}/policies/{id}")
    public EDCPolicyDto getPolicyForEdc(@PathParam("edcId") Long edcId, @PathParam("id") Long id) {
        Log.info("Fetching policy " + id + " for EDC: " + edcId);
        Optional<EDCPolicy> policyOpt = service.findByIdAndEdcId(id, edcId);
        
        if (policyOpt.isEmpty()) {
            Log.warn("Policy " + id + " not found for EDC " + edcId);
            throw new NotFoundException("Policy " + id + " not found for EDC " + edcId);
        }
        
        return mapper.policyToPolicyDto(policyOpt.get());
    }

    /**
     * Erstellt eine neue Policy für eine EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param dto Die zu erstellende Policy als DTO
     * @param uriInfo Kontext-Informationen für die URI-Erstellung
     * @return Die erstellte Policy mit Location-Header
     */
    @POST
    @Path("{edcId}/policies")
    @Transactional
    public Response createPolicyForEdc(@PathParam("edcId") Long edcId, EDCPolicyDto dto, @Context UriInfo uriInfo) {
        try {
            Log.info("Creating policy for EDC: " + edcId);
            // Normalisieren bevor gemappt wird (falls verschachtelt übergeben)
            normalizePolicy(dto);
            
            // Setze die EDC-ID im DTO
            dto = withEdcId(dto, edcId);
            
            // Generiere automatisch eine Policy-ID, wenn keine vorhanden ist
            if (dto.policyId() == null || dto.policyId().isEmpty()) {
                String generatedPolicyId = "policy-" + System.currentTimeMillis();
                Log.info("No policy ID provided, generating one: " + generatedPolicyId);
                dto = withPolicyId(dto, generatedPolicyId);
                
                // Aktualisiere auch die ID im Policy-Objekt, falls vorhanden
                if (dto.policy() != null && dto.policy() instanceof Map<?, ?>) {
                    Map<String, Object> policyMap = new HashMap<>(dto.policy());
                    policyMap.put("@id", generatedPolicyId);
                    dto = withPolicy(dto, policyMap);
                }
            }

            // Überprüfe zuerst, ob die EDC-Instanz existiert
            EDCInstance edcInstance = EDCInstance.findById(edcId);
            if (edcInstance == null) {
                Log.error("EDC instance not found in database: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                             .entity("EDC instance not found: " + edcId)
                             .build();
            }
            
            Log.info("Found EDC instance: " + edcInstance.id + " with name: " + edcInstance.name);
            
            // Konvertiere DTO zu Entity
            EDCPolicy entity = mapper.policyDtoToPolicy(dto);
            
            // Prüfe, ob die EDC-Instanz korrekt gesetzt wurde
            if (entity.getEdcInstance() == null) {
                Log.error("EDC instance not set in entity after mapping from DTO: " + edcId);
                
                // Setze die EDC-Instanz manuell
                Log.info("Manually setting EDC instance");
                entity.setEdcInstance(edcInstance);
            }
            
            // Speichere die Policy
            entity.setPolicyJson(dto.rawJson());
            EDCPolicy created = service.create(entity);
            EDCPolicyDto result = mapper.policyToPolicyDto(created);
            
            // Erstelle URI für Location-Header
            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(created.id.toString())
                    .build();
                    
            Log.info("Policy created successfully with ID: " + created.id);
            return Response.created(uri).entity(result).build();
        } catch (Exception e) {
            Log.error("Error creating policy", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity("Error creating policy: " + e.getMessage())
                         .build();
        }
    }

    /**
     * Aktualisiert eine bestehende Policy für eine EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param id Die ID der zu aktualisierenden Policy
     * @param dto Die aktualisierte Policy als DTO
     * @return Die aktualisierte Policy
     */
    @PUT
    @Path("{edcId}/policies/{id}")
    @Transactional
    public Response updatePolicyForEdc(@PathParam("edcId") Long edcId, @PathParam("id") Long id, EDCPolicyDto dto) {
        try {
            Log.info("Updating policy " + id + " for EDC: " + edcId);
            
            // Setze die IDs im DTO
            dto = withId(dto, id);
            dto = withEdcId(dto, edcId);
            
            // Normalisiere die Policy-Struktur
            dto = normalizePolicy(dto);

            // Prüfe, ob die Policy existiert
            Optional<EDCPolicy> existingPolicyOpt = service.findByIdAndEdcId(id, edcId);
            if (existingPolicyOpt.isEmpty()) {
                Log.error("Policy " + id + " not found for EDC " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                             .entity("Policy " + id + " not found for EDC " + edcId)
                             .build();
            }
            
            // Konvertiere DTO zu Entity
            EDCPolicy newState = mapper.policyDtoToPolicy(dto);
            
            // Prüfe, ob die EDC-Instanz existiert
            if (newState.getEdcInstance() == null) {
                Log.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                             .entity("EDC instance not found: " + edcId)
                             .build();
            }
            
            // Aktualisiere die bestehende Entity
            EDCPolicy existingPolicy = existingPolicyOpt.get();
            existingPolicy.setPolicyId(newState.getPolicyId());
            existingPolicy.setPolicyJson(newState.getPolicyJson());
            existingPolicy.setDisplayName(newState.getDisplayName());
            
            // Konvertiere zurück zu DTO
            EDCPolicyDto result = mapper.policyToPolicyDto(existingPolicy);
            
            Log.info("Policy updated successfully: " + result.id());
            return Response.ok(result).build();
        } catch (Exception e) {
            Log.error("Error updating policy", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity("Error updating policy: " + e.getMessage())
                         .build();
        }
    }

    /**
     * Löscht eine Policy für eine EDC-Instanz.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param id Die ID der zu löschenden Policy
     * @return 204 No Content bei Erfolg
     */
    @DELETE
    @Path("{edcId}/policies/{id}")
    @Transactional
    public Response deletePolicyForEdc(@PathParam("edcId") Long edcId, @PathParam("id") Long id) {
        Log.info("Deleting policy " + id + " for EDC: " + edcId);
        
        Optional<EDCPolicy> policy = service.findByIdAndEdcId(id, edcId);
        if (policy.isEmpty()) {
            Log.warn("Policy " + id + " not found for EDC " + edcId);
            return Response.status(Response.Status.NOT_FOUND)
                         .entity("Policy " + id + " not found for EDC " + edcId)
                         .build();
        }
        
        EDCPolicy policyEntity = policy.get();
        Log.info("Found policy to delete: id=" + policyEntity.id + ", policyId=" + policyEntity.getPolicyId());
        
        if (service.delete(id)) {
            Log.info("Policy " + id + " deleted successfully");
            
            // Verify the policy is actually gone from the database
            Optional<EDCPolicy> checkDeleted = service.findByIdAndEdcId(id, edcId);
            if (checkDeleted.isPresent()) {
                Log.error("Policy " + id + " still exists in database after deletion!");
            } else {
                Log.info("Verified policy " + id + " is no longer in database");
            }
            
            return Response.noContent().build();
        } else {
            Log.error("Failed to delete policy " + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity("Failed to delete policy " + id)
                         .build();
        }
    }

    /**
     * Normalisiert die Policy-Struktur, falls sie verschachtelt übergeben wurde.
     * 
     * @param dto Die zu normalisierende Policy
     */
    @SuppressWarnings("unchecked")
    private EDCPolicyDto normalizePolicy(EDCPolicyDto dto) {
        if (dto == null || dto.policy() == null) return dto;

        Map<String, Object> policy = new HashMap<>(dto.policy());
        Object nested = policy.get("policy");
        boolean changed = false;
        
        if (nested instanceof Map) {
            // Cast sicher, weil Map<String,Object>
            Map<String, Object> nestedMapCasted = (Map<String, Object>) nested;

            // permission hochziehen
            if (nestedMapCasted.containsKey("permission") && !policy.containsKey("permission")) {
                policy.put("permission", nestedMapCasted.get("permission"));
                changed = true;
            }

            // optional: context & id hochziehen
            if (nestedMapCasted.containsKey("@context") && !policy.containsKey("@context")) {
                policy.put("@context", nestedMapCasted.get("@context"));
                changed = true;
            }
            if (nestedMapCasted.containsKey("@id") && !policy.containsKey("@id")) {
                policy.put("@id", nestedMapCasted.get("@id"));
                changed = true;
            }

            // nested entfernen
            policy.remove("policy");
            changed = true;
        }
        
        return changed ? withPolicy(dto, policy) : dto;
    }
    
    /**
     * Creates a copy of the EDCPolicyDto with an updated edcId
     */
    private EDCPolicyDto withEdcId(EDCPolicyDto dto, Long edcId) {
        return new EDCPolicyDto(
                dto.id(),
                edcId,
                dto.policyId(),
                dto.displayName(),
                dto.policy(),
                dto.rawJson(),
                dto.context()
        );
    }
    
    /**
     * Creates a copy of the EDCPolicyDto with an updated id
     */
    private EDCPolicyDto withId(EDCPolicyDto dto, Long id) {
        return new EDCPolicyDto(
                id,
                dto.edcId(),
                dto.policyId(),
                dto.displayName(),
                dto.policy(),
                dto.rawJson(),
                dto.context()
        );
    }
    
    /**
     * Creates a copy of the EDCPolicyDto with an updated policyId
     */
    private EDCPolicyDto withPolicyId(EDCPolicyDto dto, String policyId) {
        return new EDCPolicyDto(
                dto.id(),
                dto.edcId(),
                policyId,
                dto.displayName(),
                dto.policy(),
                dto.rawJson(),
                dto.context()
        );
    }
    
    /**
     * Creates a copy of the EDCPolicyDto with an updated policy map
     */
    private EDCPolicyDto withPolicy(EDCPolicyDto dto, Map<String, Object> policy) {
        return new EDCPolicyDto(
                dto.id(),
                dto.edcId(),
                dto.policyId(),
                dto.displayName(),
                policy,
                dto.rawJson(),
                dto.context()
        );
    }
    
    /**
     * Creates a copy of the EDCPolicyDto with an updated rawJson
     */
    private EDCPolicyDto withRawJson(EDCPolicyDto dto, String rawJson) {
        return new EDCPolicyDto(
                dto.id(),
                dto.edcId(),
                dto.policyId(),
                dto.displayName(),
                dto.policy(),
                rawJson,
                dto.context()
        );
    }
}
