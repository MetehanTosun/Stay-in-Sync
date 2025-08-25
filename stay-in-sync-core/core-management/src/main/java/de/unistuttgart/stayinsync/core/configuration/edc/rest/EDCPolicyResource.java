package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCPolicyMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCPolicyService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCPolicyResource {

    @Inject
    EDCPolicyService service;

    

    @GET
    public List<EDCPolicyDto> list() {
        return service.listAll().stream()
                .map(entity -> {
                    try {
                        return EDCPolicyMapper.toDto(entity);
                    } catch (JsonProcessingException e) {
                        throw new WebApplicationException("Error converting policy", e, 500);
                    }
                })
                .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    public EDCPolicyDto get(@PathParam("id") UUID id) {
        return service.findById(id)
                .map(entity -> {
                    try {
                        return EDCPolicyMapper.toDto(entity);
                    } catch (JsonProcessingException e) {
                        throw new WebApplicationException("Error converting policy", e, 500);
                    }
                })
                .orElseThrow(() -> new NotFoundException("Policy " + id + " nicht gefunden"));
    }

  @POST
@Transactional
public Response create(EDCPolicyDto dto, @Context UriInfo uriInfo) {
    try {
        // 🔑 Normalisieren bevor gemappt wird (falls verschachtelt übergeben)
        normalizePolicy(dto);

        EDCPolicy entity = EDCPolicyMapper.fromDto(dto);
        EDCPolicy created = service.create(entity);
        EDCPolicyDto result = EDCPolicyMapper.toDto(created);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(result.getId().toString())
                .build();
        return Response.created(uri).entity(result).build();
    } catch (Exception e) {
        throw new WebApplicationException("Error creating policy", e, 500);
    }
}


 @PUT
@Path("{id}")
@Transactional
public EDCPolicyDto update(@PathParam("id") UUID id, EDCPolicyDto dto) {
    try {
        dto.setId(id);
        // 🔑 Auch hier normalisieren
        normalizePolicy(dto);

        EDCPolicy newState = EDCPolicyMapper.fromDto(dto);
        return service.update(id, newState)
                .map(entity -> {
                    try {
                        return EDCPolicyMapper.toDto(entity);
                    } catch (JsonProcessingException e) {
                        throw new WebApplicationException("Error converting policy", e, 500);
                    }
                })
                .orElseThrow(() -> new NotFoundException("Policy " + id + " nicht gefunden"));
    } catch (Exception e) {
        throw new WebApplicationException("Error updating policy", e, 500);
    }
}


    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException("Policy " + id + " nicht gefunden");
        }
    }


    @SuppressWarnings("unchecked")
private static void normalizePolicy(EDCPolicyDto dto) {
    if (dto == null || dto.getPolicy() == null) return;

    Object nested = dto.getPolicy().get("policy");
    if (nested instanceof java.util.Map<?, ?> nestedMap) {
        // Cast sicher, weil Map<String,Object>
        java.util.Map<String, Object> nestedMapCasted = (java.util.Map<String, Object>) nestedMap;

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
