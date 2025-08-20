package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTestResultDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelElementNodeDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelSummaryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasElementLite;
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
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    AasStructureSnapshotService snapshotService;

    @Inject
    de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/test")
    public Uni<Response> test(@PathParam("sourceSystemId") Long sourceSystemId) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        Uni<HttpResponse<Buffer>> uni = traversal.getShell(ss.apiUrl, ss.aasId, headers);
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
    public Uni<Response> listSubmodels(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            return traversal.listSubmodels(ss.apiUrl, ss.aasId, headers).map(resp -> {
                int sc = resp.statusCode();
                if (sc >= 200 && sc < 300) {
                    return Response.ok(resp.bodyAsString()).build();
                }
                return Response.status(sc).entity(resp.bodyAsString()).build();
            });
        }
        var list = AasSubmodelLite.<AasSubmodelLite>list("sourceSystem.id", sourceSystemId)
                .stream()
                .map(sm -> new SubmodelSummaryDTO(sm.submodelId, sm.submodelIdShort, sm.semanticId, sm.kind))
                .toList();
        if (!list.isEmpty()) {
            return Uni.createFrom().item(Response.ok().entity(list).build());
        }
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        return traversal.listSubmodels(ss.apiUrl, ss.aasId, headers).map(resp -> {
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return Response.ok(resp.bodyAsString()).build();
            }
            return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        });
    }

    @GET
    @Path("/submodels/{smId}/elements")
    public Uni<Response> listElements(@PathParam("sourceSystemId") Long sourceSystemId,
                                 @PathParam("smId") String smId,
                                 @QueryParam("depth") @DefaultValue("shallow") String depth,
                                 @QueryParam("parentPath") String parentPath,
                                 @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            return traversal.listElements(ss.apiUrl, smId, depth, parentPath, headers).map(resp -> {
                int sc = resp.statusCode();
                if (sc >= 200 && sc < 300) {
                    return Response.ok(resp.bodyAsString()).build();
                }
                return Response.status(sc).entity(resp.bodyAsString()).build();
            });
        }
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, smId).firstResult();
        if (submodel == null) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            return traversal.listElements(ss.apiUrl, smId, depth, parentPath, headers).map(resp -> {
                int sc = resp.statusCode();
                if (sc >= 200 && sc < 300) {
                    return Response.ok(resp.bodyAsString()).build();
                }
                return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
            });
        }
        java.util.List<AasElementLite> elements;
        if ("all".equalsIgnoreCase(depth)) {
            if (parentPath == null || parentPath.isBlank()) {
                elements = AasElementLite.list("submodelLite.id", submodel.id);
            } else {
                String prefix = parentPath.endsWith("/") ? parentPath : parentPath + "/";
                elements = AasElementLite.list("submodelLite.id = ?1 and (idShortPath = ?2 or idShortPath like ?3)", submodel.id, parentPath, prefix + "%");
            }
        } else {
            if (parentPath == null || parentPath.isBlank()) {
                elements = AasElementLite.list("submodelLite.id = ?1 and parentPath is null", submodel.id);
            } else {
                elements = AasElementLite.list("submodelLite.id = ?1 and parentPath = ?2", submodel.id, parentPath);
            }
        }
        var list = elements.stream().map(e -> new SubmodelElementNodeDTO(
                e.modelType,
                e.idShort,
                e.idShortPath,
                e.hasChildren,
                e.valueType,
                e.semanticId,
                e.isReference,
                e.referenceTargetType,
                e.referenceKeys,
                e.targetSubmodelId,
                e.typeValueListElement,
                e.orderRelevant,
                null,
                null
        )).toList();
        if (!list.isEmpty()) {
            return Uni.createFrom().item(Response.ok().entity(list).build());
        }
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        return traversal.listElements(ss.apiUrl, smId, depth, parentPath, headers).map(resp -> {
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return Response.ok(resp.bodyAsString()).build();
            }
            return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        });
    }

    @POST
    @Path("/submodels")
    public Response createSubmodel(@PathParam("sourceSystemId") Long sourceSystemId, String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.createSubmodel(ss.apiUrl, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        if (sc >= 200 && sc < 300) {
            snapshotService.applySubmodelCreate(sourceSystemId, resp.bodyAsString());
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    @POST
    @Path("/submodels/{smId}/elements")
    public Response createElement(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @PathParam("smId") String smId,
                                  @QueryParam("parentPath") String parentPath,
                                  String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.createElement(ss.apiUrl, smId, parentPath, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        if (sc >= 200 && sc < 300) {
            snapshotService.applyElementCreate(sourceSystemId, smId, parentPath, resp.bodyAsString());
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    @PATCH
    @Path("/submodels/{smId}/elements/{path}/value")
    public Response patchElementValue(@PathParam("sourceSystemId") Long sourceSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.patchElementValue(ss.apiUrl, smId, path, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        if (sc >= 200 && sc < 300) {
            return Response.noContent().build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    @POST
    @Path("/snapshot/refresh")
    public Response refreshSnapshot(@PathParam("sourceSystemId") Long sourceSystemId) {
        snapshotService.refreshSnapshot(sourceSystemId);
        return Response.accepted().build();
    }
}


