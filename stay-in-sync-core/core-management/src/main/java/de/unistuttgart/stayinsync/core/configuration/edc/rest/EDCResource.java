package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.EDCInstanceDto;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.EntityCreationFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.EntityUpdateFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCService;
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
    public Response createConnectionToExistingPartnerEdcInstance(@Valid EDCInstanceDto edcInstanceToCreate, @Context UriInfo uriInfo) {
        Log.infof("POST EdcInstance request");
        try {
            EDCInstanceDto createdEdcInstance = service.create(edcInstanceToCreate);
            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(createdEdcInstance.id().toString())
                    .build();
            Log.infof("POST EDCInstance request successfully completed");
            return Response.created(uri).entity(edcInstanceToCreate).build();
        } catch (EntityCreationFailedException e) {
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
        try {
            return service.update(id, dto);
        } catch (EntityUpdateFailedException e) {
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