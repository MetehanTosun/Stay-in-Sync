package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCContractDefinitionMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCContractDefinitionService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/config/edcs/contract-definitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "EDCContractDefinition", description = "Verwaltet Vertragsdefinitionen")
public class EDCContractDefinitionResource {

    @Inject
    EDCContractDefinitionService service;

    @GET
    public List<EDCContractDefinitionDto> list() {
        return service.listAll().stream()
            .map(EDCContractDefinitionMapper::toDto)
            .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    public EDCContractDefinitionDto get(@PathParam("id") UUID id) {
        return service.findById(id)
            .map(EDCContractDefinitionMapper::toDto)
            .orElseThrow(() -> new NotFoundException("ContractDefinition " + id + " nicht gefunden"));
    }

    @POST
    @Transactional
    public Response create(EDCContractDefinitionDto dto, @Context UriInfo uriInfo) {
        var entity     = EDCContractDefinitionMapper.fromDto(dto);
        var created    = service.create(entity);
        var createdDto = EDCContractDefinitionMapper.toDto(created);
        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(createdDto.getId().toString())
                         .build();
        return Response.created(uri)
                       .entity(createdDto)
                       .build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public EDCContractDefinitionDto update(@PathParam("id") UUID id, EDCContractDefinitionDto dto) {
        dto.setId(id);
        return service.update(id, dto)
            .map(EDCContractDefinitionMapper::toDto)
            .orElseThrow(() -> new NotFoundException("ContractDefinition " + id + " nicht gefunden"));
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException("ContractDefinition " + id + " nicht gefunden");
        }
    }
}
