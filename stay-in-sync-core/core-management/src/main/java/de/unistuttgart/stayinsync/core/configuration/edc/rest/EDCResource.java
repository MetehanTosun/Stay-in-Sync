package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCInstanceMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCService;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import io.quarkus.logging.Log;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCResource {

    @Inject
    public EDCService service;
    
    @Inject
    EDCInstanceMapper mapper;

    @GET
    public List<EDCInstanceDto> list() {
        Log.info("Abrufen aller EDC-Instanzen");
        return service.listAll();
    }

    @GET
    @Path("{id}")
    public EDCInstanceDto get(@PathParam("id") UUID id) {
        Log.info("Abrufen der EDC-Instanz mit ID: " + id);
        try {
            return service.findById(id);
        } catch (CustomException e) {
            Log.info("EDC-Instanz mit ID " + id + " nicht gefunden");
            throw new NotFoundException(e.getMessage());
        }
    }

    @POST
    @Transactional
    public Response create(@Valid EDCInstanceDto dto, @Context UriInfo uriInfo) {
        Log.info("Erstellen einer neuen EDC-Instanz: " + dto.getName());
        try {
            var created = service.create(dto);
            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(created.getId().toString())
                    .build();
            Log.info("EDC-Instanz erfolgreich erstellt mit ID: " + created.getId());
            return Response.created(uri).entity(created).build();
        } catch (Exception e) {
            Log.error("Fehler beim Erstellen der EDC-Instanz: " + e.getMessage());
            throw new WebApplicationException("Fehler beim Erstellen der EDC-Instanz: " + e.getMessage(), 
                    Response.Status.BAD_REQUEST);
        }
    }

    @PUT
    @Path("{id}")
    @Transactional
    public EDCInstanceDto update(@PathParam("id") UUID id, @Valid EDCInstanceDto dto) {
        Log.info("Aktualisieren der EDC-Instanz mit ID: " + id);
        dto.setId(id);
        try {
            return service.update(id, dto);
        } catch (CustomException e) {
            Log.info("EDC-Instanz mit ID " + id + " für Update nicht gefunden");
            throw new NotFoundException(e.getMessage());
        }
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        Log.info("Löschen der EDC-Instanz mit ID: " + id);
        if (!service.delete(id)) {
            Log.info("EDC-Instanz mit ID " + id + " für Löschung nicht gefunden");
            throw new NotFoundException("EDC-Instanz mit ID " + id + " nicht gefunden");
        }
        Log.info("EDC-Instanz mit ID " + id + " erfolgreich gelöscht");
    }
}