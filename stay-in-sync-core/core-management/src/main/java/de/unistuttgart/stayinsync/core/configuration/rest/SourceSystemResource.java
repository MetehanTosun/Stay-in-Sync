package de.unistuttgart.stayinsync.core.configuration.rest;

import static jakarta.ws.rs.core.MediaType.*;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("api/aas")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SourceSystemResource {

    @Inject
    SourceSystemService ssService;

    @GET
    @Operation(summary = "Returns all source systems")
    @APIResponse(responseCode = "200", description = "List of all source systems", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = SourceSystem.class)))
    public List<SourceSystem> getAllSs() {
        return ssService.findAllSourceSystems();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a source system by its ID")
    @APIResponse(responseCode = "200", description = "The found source system", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystem.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response getSsById(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        var found = ssService.findSourceSystemById(id);
        if (found != null) {
            Log.debugf("Found source system: %s", found);
            return Response.ok(found).build();
        } else {
            Log.debugf("No source system found with id %d", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Operation(summary = "Creates a new source system")
    @APIResponse(responseCode = "201", description = "The URI of the created source system", headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class)))
    @APIResponse(responseCode = "400", description = "Invalid source system passed in")
    public Response createSs(
            @RequestBody(name = "source-system", required = true, content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystem.class), examples = @ExampleObject(name = "valid_source_system", value = "{\"name\":\"Sensor A\",\"description\":\"Raumtemperatur\",\"endpointUrl\":\"http://localhost:8080\"}"))) @Valid @NotNull SourceSystem input,
            @Context UriInfo uriInfo) {
        ssService.createSourceSystem(input);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(input.id));
        Log.debugf("New source system created with URI %s", builder.build().toString());
        return Response.created(builder.build()).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Updates an existing source system")
    @APIResponse(responseCode = "200", description = "The updated source system", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SourceSystem.class)))
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response updateSs(@Parameter(name = "id", required = true) @PathParam("id") Long id,
            @Valid @NotNull SourceSystem input) {
        input.id = id;
        ssService.updateSourceSystem(input);
        return Response.ok(input).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes a source system by its ID")
    @APIResponse(responseCode = "204", description = "Source system deleted")
    @APIResponse(responseCode = "404", description = "Source system not found")
    public Response deleteSs(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        ssService.deleteSourceSystemById(id);
        return Response.noContent().build();
    }
}
