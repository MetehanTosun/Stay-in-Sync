package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.PolicyDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCPolicyMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCPolicyService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    /**
     * Listet alle Policies für eine bestimmte EDC-Instanz auf.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @return Eine Liste aller Policies für diese EDC-Instanz
     */
    @GET
    @Path("{edcId}/policies")
    public List<PolicyDefinitionDto> listPoliciesForEdc(@PathParam("edcId") UUID edcId) {
        Log.info("Fetching policies for EDC: " + edcId);
        List<PolicyDefinition> policies = service.listAllByEdcId(edcId);
        
        List<PolicyDefinitionDto> policyDtos = policies.stream()
                .map(entity -> EDCPolicyMapper.policyMapper.policyToPolicyDto(entity))
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
    public PolicyDefinitionDto getPolicyForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id) {
        Log.info("Fetching policy " + id + " for EDC: " + edcId);
        Optional<PolicyDefinition> policyOpt = service.findByIdAndEdcId(id, edcId);
        
        if (policyOpt.isEmpty()) {
            Log.warn("Policy " + id + " not found for EDC " + edcId);
            throw new NotFoundException("Policy " + id + " not found for EDC " + edcId);
        }
        
        return EDCPolicyMapper.policyMapper.policyToPolicyDto(policyOpt.get());
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
    public Response createPolicyForEdc(@PathParam("edcId") UUID edcId, PolicyDefinitionDto dto, @Context UriInfo uriInfo) {
        try {
            Log.info("Creating policy for EDC: " + edcId);
            // Normalisieren bevor gemappt wird (falls verschachtelt übergeben)
            normalizePolicy(dto);
            
            // Setze die EDC-ID im DTO
            dto.setEdcId(edcId);
            
            // Generiere automatisch eine Policy-ID, wenn keine vorhanden ist
            if (dto.getPolicyId() == null || dto.getPolicyId().isEmpty()) {
                String generatedPolicyId = "policy-" + System.currentTimeMillis();
                Log.info("No policy ID provided, generating one: " + generatedPolicyId);
                dto.setPolicyId(generatedPolicyId);
                
                // Aktualisiere auch die ID im Policy-Objekt, falls vorhanden
                if (dto.getPolicy() != null && dto.getPolicy() instanceof Map<?, ?>) {
                    Map<String, Object> policyMap = (Map<String, Object>) dto.getPolicy();
                    policyMap.put("@id", generatedPolicyId);
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
            
            Log.info("Found EDC instance: " + edcInstance.id + " with name: " + edcInstance.getName());
            
            // Konvertiere DTO zu Entity
            PolicyDefinition entity = EDCPolicyMapper.policyMapper.policyDtoToPolicy(dto);
            
            // Prüfe, ob die EDC-Instanz korrekt gesetzt wurde
            if (entity.getEdcInstance() == null) {
                Log.error("EDC instance not set in entity after mapping from DTO: " + edcId);
                
                // Setze die EDC-Instanz manuell
                Log.info("Manually setting EDC instance");
                entity.setEdcInstance(edcInstance);
            }
            
            // Speichere die Policy
            PolicyDefinition created = service.create(entity);
            PolicyDefinitionDto result = EDCPolicyMapper.policyMapper.policyToPolicyDto(created);
            
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
    public Response updatePolicyForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id, PolicyDefinitionDto dto) {
        try {
            Log.info("Updating policy " + id + " for EDC: " + edcId);
            
            // Setze die IDs im DTO
            dto.setId(id);
            dto.setEdcId(edcId);
            
            // Normalisiere die Policy-Struktur
            normalizePolicy(dto);

            // Prüfe, ob die Policy existiert
            Optional<PolicyDefinition> existingPolicyOpt = service.findByIdAndEdcId(id, edcId);
            if (existingPolicyOpt.isEmpty()) {
                Log.error("Policy " + id + " not found for EDC " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                             .entity("Policy " + id + " not found for EDC " + edcId)
                             .build();
            }
            
            // Konvertiere DTO zu Entity
            PolicyDefinition newState = EDCPolicyMapper.policyMapper.policyDtoToPolicy(dto);
            
            // Prüfe, ob die EDC-Instanz existiert
            if (newState.getEdcInstance() == null) {
                Log.error("EDC instance not found: " + edcId);
                return Response.status(Response.Status.NOT_FOUND)
                             .entity("EDC instance not found: " + edcId)
                             .build();
            }
            
            // Aktualisiere die bestehende Entity
            PolicyDefinition existingPolicy = existingPolicyOpt.get();
            existingPolicy.setPolicyId(newState.getPolicyId());
            existingPolicy.setPolicyJson(newState.getPolicyJson());
            existingPolicy.setDisplayName(newState.getDisplayName());
            
            // Konvertiere zurück zu DTO
            PolicyDefinitionDto result = EDCPolicyMapper.policyMapper.policyToPolicyDto(existingPolicy);
            
            Log.info("Policy updated successfully: " + result.getId());
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
    public Response deletePolicyForEdc(@PathParam("edcId") UUID edcId, @PathParam("id") UUID id) {
        Log.info("Deleting policy " + id + " for EDC: " + edcId);
        
        Optional<PolicyDefinition> policy = service.findByIdAndEdcId(id, edcId);
        if (policy.isEmpty()) {
            Log.warn("Policy " + id + " not found for EDC " + edcId);
            return Response.status(Response.Status.NOT_FOUND)
                         .entity("Policy " + id + " not found for EDC " + edcId)
                         .build();
        }
        
        PolicyDefinition policyEntity = policy.get();
        Log.info("Found policy to delete: id=" + policyEntity.id + ", policyId=" + policyEntity.getPolicyId());
        
        if (service.delete(id)) {
            Log.info("Policy " + id + " deleted successfully");
            
            // Verify the policy is actually gone from the database
            Optional<PolicyDefinition> checkDeleted = service.findByIdAndEdcId(id, edcId);
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
    private static void normalizePolicy(PolicyDefinitionDto dto) {
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
