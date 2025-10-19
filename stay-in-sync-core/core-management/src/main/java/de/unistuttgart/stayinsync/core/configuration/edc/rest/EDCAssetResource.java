package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCAssetService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST-Ressource für die Verwaltung von EDC-Assets.
 * <p>
 * Stellt Endpunkte für grundlegende CRUD-Operationen auf EDC-Assets bereit.
 * Unterstützt sowohl allgemeine Asset-Operationen als auch EDC-spezifische Asset-Operationen.
 */
@Path("/api/config/edcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCAssetResource {

    private static final Logger LOG = Logger.getLogger(EDCAssetResource.class);

    @Inject
    EDCAssetService service;

    /**
     * Listet alle verfügbaren Assets auf.
     *
     * @return Liste aller Assets als DTOs
     */
    @GET
    @Path("/assets")
    public List<EDCAssetDto> list() {
        return service.listAll();
    }

    /**
     * Holt ein spezifisches Asset anhand seiner ID.
     *
     * @param id Die ID des Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn das Asset nicht gefunden wird
     */
    @GET
    @Path("/assets/{id}")
    public EDCAssetDto get(@PathParam("id") Long id) throws CustomException {
        return service.findById(id);
    }

    /**
     * Erstellt ein neues Asset.
     *
     * @param assetDto Das zu erstellende Asset als DTO
     * @param uriInfo  Kontext-Information für die URI-Erstellung
     * @return HTTP-Response mit dem erstellten Asset
     */
    @POST
    @Path("/assets")
    @Transactional
    public Response create(final EDCAssetDto assetDto, @Context final UriInfo uriInfo) throws CustomException {
        final EDCAssetDto createdAsset = service.create(assetDto);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(createdAsset.id().toString())
                .build();
        return Response.created(uri)
                .entity(createdAsset)
                .build();
    }

    /**
     * Aktualisiert ein bestehendes Asset.
     *
     * @param id       Die ID des zu aktualisierenden Assets
     * @param assetDto Das aktualisierte Asset als DTO
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn das Asset nicht gefunden wird
     */
    @PUT
    @Path("/assets/{id}")
    @Transactional
    public EDCAssetDto update(@PathParam("id") Long id, final EDCAssetDto assetDto) throws CustomException {
        return service.update(id, assetDto);
    }

    /**
     * Löscht ein Asset anhand seiner ID.
     *
     * @param id Die ID des zu löschenden Assets
     * @throws NotFoundException Wenn das Asset nicht gefunden wird
     */
    @DELETE
    @Path("/assets/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) throws CustomException {
        if (!service.delete(id)) {
            throw new NotFoundException("Asset " + id + " nicht gefunden");
        }
    }

    /**
     * Erstellt ein neues Asset für eine spezifische EDC-Instanz.
     * Akzeptiert ein Asset im Frontend-Format und konvertiert es in das Backend-Format.
     *
     * @param edcIdStr     Die ID der EDC-Instanz als String
     * @param frontendJson Das Asset im Frontend-Format
     * @param uriInfo      Kontext-Information für die URI-Erstellung
     * @return HTTP-Response mit dem erstellten Asset
     * @throws CustomException Wenn die EDC-Instanz nicht gefunden wird
     */
    @POST
    @Path("/{edcId}/assets")
    @Transactional
    public Response createForEdc(@PathParam("edcId") String edcIdStr, final Map<String, Object> frontendJson, @Context final UriInfo uriInfo) {
        try {
            Long edcId = Long.parseLong(edcIdStr);
            
            LOG.info("Creating asset for EDC: " + edcId);
            
            if (frontendJson == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Asset data is missing")
                        .build();
            }

            // Verarbeite das Frontend-Format und erstelle das Asset
            EDCAssetDto assetDto = service.processFrontendAsset(frontendJson, edcId);
            final EDCAssetDto createdAsset = service.createForEdc(edcId, assetDto);

            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(createdAsset.id().toString())
                    .build();

            LOG.info("Asset created successfully with ID: " + createdAsset.id());
            return Response.created(uri)
                    .entity(createdAsset)
                    .build();
        } catch (NumberFormatException e) {
            LOG.error("Invalid EDC ID format: " + edcIdStr);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid EDC ID format. Expected a number.")
                    .build();
        } catch (CustomException e) {
            LOG.error("Error creating asset: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Error creating asset: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to create asset: " + e.getMessage())
                    .build();
        }
    }


    /**
     * Holt alle Assets für eine spezifische EDC-Instanz.
     *
     * @param edcIdStr Die ID der EDC-Instanz als String
     * @return HTTP-Response mit der Liste der Assets
     */
    @GET
    @Path("/{edcId}/assets")
    public Response getAssetsForEdc(@PathParam("edcId") String edcIdStr) {
        try {
            Long edcId = Long.parseLong(edcIdStr);

            LOG.info("Fetching assets for EDC: " + edcId);
            List<EDCAssetDto> assets = service.listAllByEdcId(edcId);

            LOG.info("Returning " + assets.size() + " assets for EDC: " + edcId);
            return Response.ok(assets).build();
        } catch (NumberFormatException e) {
            LOG.error("Invalid EDC ID format: " + edcIdStr);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid EDC ID format. Expected a number.")
                    .build();
        } catch (Exception e) {
            LOG.error("Error fetching assets: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch assets: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Holt ein spezifisches Asset für eine spezifische EDC-Instanz.
     *
     * @param edcIdStr   Die ID der EDC-Instanz als String
     * @param assetIdStr Die ID des Assets als String
     * @return HTTP-Response mit dem gefundenen Asset
     */
    @GET
    @Path("/{edcId}/assets/{assetId}")
    public Response getAssetForEdc(@PathParam("edcId") String edcIdStr, @PathParam("assetId") String assetIdStr) {
        try {
            Long edcId = Long.parseLong(edcIdStr);
            Long assetId = Long.parseLong(assetIdStr);

            LOG.info("Fetching asset " + assetId + " for EDC: " + edcId);
            EDCAssetDto asset = service.findByIdAndEdcId(edcId, assetId);

            LOG.info("Found asset with ID: " + asset.id());
            return Response.ok(asset).build();
        } catch (NumberFormatException e) {
            LOG.error("Invalid ID format: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid ID format. Expected a number.")
                    .build();
        } catch (CustomException e) {
            LOG.error("Asset not found: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Error fetching asset: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to fetch asset: " + e.getMessage())
                    .build();
        }
    }


    /**
     * Aktualisiert ein spezifisches Asset für eine spezifische EDC-Instanz.
     * Akzeptiert ein Asset im Frontend-Format und konvertiert es in das Backend-Format.
     *
     * @param edcIdStr     Die ID der EDC-Instanz als String
     * @param assetIdStr   Die ID des Assets als String
     * @param frontendJson Das aktualisierte Asset im Frontend-Format
     * @return HTTP-Response mit dem aktualisierten Asset
     */
    @PUT
    @Path("/{edcId}/assets/{assetId}")
    @Transactional
    public Response updateAssetForEdc(@PathParam("edcId") String edcIdStr, @PathParam("assetId") String assetIdStr,
                                      final Map<String, Object> frontendJson) {
        try {
            Long edcId = Long.parseLong(edcIdStr);
            Long assetId = Long.parseLong(assetIdStr);

            LOG.info("Updating asset " + assetId + " for EDC: " + edcId);
            
            if (frontendJson == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Asset data is missing")
                        .build();
            }

            // Verarbeite das Frontend-Format und aktualisiere das Asset
            EDCAssetDto assetDto = service.processFrontendAsset(frontendJson, edcId);
            final EDCAssetDto updatedAsset = service.update(assetId, assetDto);

            LOG.info("Asset updated successfully with ID: " + updatedAsset.id());
            return Response.ok(updatedAsset).build();
        } catch (NumberFormatException e) {
            LOG.error("Invalid ID format: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid ID format. Expected a number.")
                    .build();
        } catch (CustomException e) {
            LOG.error("Asset not found: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Error updating asset: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to update asset: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Löscht ein spezifisches Asset für eine spezifische EDC-Instanz.
     * Unterstützt sowohl numerische IDs als auch EDC-Asset-IDs im String-Format.
     *
     * @param edcIdStr   Die ID der EDC-Instanz als String
     * @param assetIdStr Die ID des Assets als String
     * @return HTTP-Response mit dem Status der Löschoperation
     */
    @DELETE
    @Path("/{edcId}/assets/{assetId}")
    @Transactional
    public Response deleteAssetForEdc(@PathParam("edcId") String edcIdStr, @PathParam("assetId") String assetIdStr) {
        try {
            Long edcId = Long.parseLong(edcIdStr);
            
            LOG.info("Deleting asset with ID " + assetIdStr + " for EDC: " + edcId);
            
            if (assetIdStr == null || assetIdStr.isEmpty() || "undefined".equals(assetIdStr)) {
                LOG.error("Invalid asset ID: null, empty or undefined");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid asset ID: must not be null, empty or undefined")
                        .build();
            }
            
            boolean deleted = false;
            
            // Versuche, die Asset-ID als Long zu parsen, falls es eine numerische ID ist
            if (assetIdStr.matches("\\d+")) {
                try {
                    LOG.info("Trying to delete by numeric ID: " + assetIdStr);
                    Long assetId = Long.parseLong(assetIdStr);
                    deleted = service.deleteByIdAndEdcId(edcId, assetId);
                    
                    if (deleted) {
                        LOG.info("Asset deleted successfully by numeric ID: " + assetIdStr);
                        return Response.status(Response.Status.OK)
                                .entity("Asset with ID " + assetIdStr + " was successfully deleted")
                                .build();
                    } else {
                        LOG.warn("Asset with numeric ID " + assetIdStr + " not found");
                    }
                } catch (Exception e) {
                    LOG.warn("Could not delete asset by numeric ID: " + e.getMessage());
                    // Weiter zum nächsten Versuch
                }
            }
            
            // Versuche, das Asset anhand seiner EDC-Asset-ID zu löschen
            try {
                LOG.info("Trying to delete by EDC asset ID: " + assetIdStr);
                deleted = service.deleteByEdcAssetId(edcId, assetIdStr);
                
                if (deleted) {
                    LOG.info("Asset deleted successfully by EDC asset ID: " + assetIdStr);
                    return Response.status(Response.Status.OK)
                            .entity("Asset with EDC ID " + assetIdStr + " was successfully deleted")
                            .build();
                } else {
                    LOG.warn("Asset not found for deletion by EDC asset ID: " + assetIdStr);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Asset with ID " + assetIdStr + " not found for the specified EDC")
                            .build();
                }
            } catch (Exception e) {
                LOG.error("Error deleting asset by EDC asset ID: " + e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Failed to delete asset with ID " + assetIdStr + ": " + e.getMessage())
                        .build();
            }
            
        } catch (NumberFormatException e) {
            LOG.error("Invalid EDC ID format: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid EDC ID format. Expected a number.")
                    .build();
        } catch (Exception e) {
            LOG.error("Error deleting asset: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete asset: " + e.getMessage())
                    .build();
        }
    }

}
