package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTestResultDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelElementNodeDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelSummaryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/config/source-system/{sourceSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AasResource {

    @Inject
    SourceSystemService sourceSystemService;

    @Inject
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @POST
    @Path("/test")
    public Uni<Response> test(@PathParam("sourceSystemId") Long sourceSystemId) {
        SourceSystem ss = sourceSystemService.findSourceSystemById(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        Uni<HttpResponse<Buffer>> uni = traversal.getShell(ss.apiUrl, ss.aasId, Map.of());
        return uni.map(resp -> {
                    int sc = resp.statusCode();
                    if (sc >= 200 && sc < 300) {
                        return Response.ok(new AasTestResultDTO("shell", "asset")).build();
                    }
                    Log.warnf("AAS test failed: %d %s", sc, resp.statusMessage());
                    return Response.status(sc).entity(resp.bodyAsString()).build();
                });
    }

    @GET
    @Path("/submodels")
    public Response listSubmodels(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Path("/submodels/{smId}/elements")
    public Response listElements(@PathParam("sourceSystemId") Long sourceSystemId,
                                 @PathParam("smId") String smId,
                                 @QueryParam("depth") @DefaultValue("shallow") String depth,
                                 @QueryParam("parentPath") String parentPath,
                                 @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("/submodels")
    public Response createSubmodel(@PathParam("sourceSystemId") Long sourceSystemId, String body) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("/submodels/{smId}/elements")
    public Response createElement(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @PathParam("smId") String smId,
                                  @QueryParam("parentPath") String parentPath,
                                  String body) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @PATCH
    @Path("/submodels/{smId}/elements/{path}/value")
    public Response patchElementValue(@PathParam("sourceSystemId") Long sourceSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      String body) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @POST
    @Path("/snapshot/refresh")
    public Response refreshSnapshot(@PathParam("sourceSystemId") Long sourceSystemId) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}


