package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.JsonTemplateDto;
import de.unistuttgart.stayinsync.core.configuration.edc.service.JsonTemplateService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/config/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JsonTemplateRessource {

    @Inject
    JsonTemplateService service;

    @GET
    @Path("{id}")
    public JsonTemplateDto get(@PathParam("id") UUID id){
        try {
            return service.fetchJsonTemplateFromDatabase(id);
        }catch(EntityNotFoundException e){
            Log.errorf(e.getMessage());
            return null;
        }
    }

    @GET
    public List<JsonTemplateDto> list() {
        return service.fetchAllTemplates();
    }

    @POST
    @Transactional
    public Response create(final JsonTemplateDto jsonTemplateDto, @Context final UriInfo uriInfo) {
        final JsonTemplateDto createdJsonTemplate = service.persistJsonTemplate(jsonTemplateDto);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(createdJsonTemplate.id())
                .build();
        return Response.created(uri)
                .entity(createdJsonTemplate)
                .build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update(final JsonTemplateDto jsonTemplateDto, @PathParam("id")final String jsonTemplateId){
        UUID jsonTemplateUuid = UUID.fromString(jsonTemplateId);
        service.update(jsonTemplateUuid, jsonTemplateDto);
        return Response.ok(jsonTemplateDto).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response deleteJsonTemplate(@PathParam("id") String jsonTemplateIdStr){
        UUID jsonTemplateUuid = UUID.fromString(jsonTemplateIdStr);
        service.removeJsonTemplateFromDatabase(jsonTemplateUuid);
        return Response.noContent().build();
    }

}
