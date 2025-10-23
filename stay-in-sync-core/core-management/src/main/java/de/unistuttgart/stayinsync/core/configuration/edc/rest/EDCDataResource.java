package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.exception.CustomException;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCAssetService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * REST-Endpunkt für erweiterte EDC-Datenabfragen.
 * Unterstützt jetzt auch erweiterte Parameter wie Pfad, Query-Parameter und Header-Parameter.
 */
@Path("/api/edc/data")
public class EDCDataResource {

    @Inject
    EDCAssetService assetService;

    /**
     * Ruft Daten von einem Asset ab.
     *
     * @param assetId Die ID des Assets
     * @return Die abgerufenen Daten als Response
     */
    @GET
    @Path("/assets/{assetId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response fetchAssetData(@PathParam("assetId") Long assetId) {
        try {
            String data = assetService.fetchAssetData(assetId);
            return Response.ok(data).build();
        } catch (CustomException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Fehler beim Abrufen der Daten: " + e.getMessage())
                    .build();
        }
    }
}