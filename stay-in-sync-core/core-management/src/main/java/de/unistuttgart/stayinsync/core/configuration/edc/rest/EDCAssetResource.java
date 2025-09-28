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
import java.util.UUID;

/**
 * REST-Ressource für die Verwaltung von EDC-Assets.
 * <p>
 * Stellt Endpunkte für CRUD-Operationen auf EDC-Assets bereit.
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
     * @param id Die UUID des Assets
     * @return Das gefundene Asset als DTO
     * @throws CustomException Wenn das Asset nicht gefunden wird
     */
    @GET
    @Path("/assets/{id}")
    public EDCAssetDto get(@PathParam("id") UUID id) throws CustomException {
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
    public Response create(final EDCAssetDto assetDto, @Context final UriInfo uriInfo) {
        final EDCAssetDto createdAsset = service.create(assetDto);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(createdAsset.getId().toString())
                .build();
        return Response.created(uri)
                .entity(createdAsset)
                .build();
    }

    /**
     * Aktualisiert ein bestehendes Asset.
     *
     * @param id       Die UUID des zu aktualisierenden Assets
     * @param assetDto Das aktualisierte Asset als DTO
     * @return Das aktualisierte Asset als DTO
     * @throws CustomException Wenn das Asset nicht gefunden wird
     */
    @PUT
    @Path("/assets/{id}")
    @Transactional
    public EDCAssetDto update(@PathParam("id") UUID id, final EDCAssetDto assetDto) throws CustomException {
        return service.update(id, assetDto);
    }

    /**
     * Löscht ein Asset anhand seiner ID.
     *
     * @param id Die UUID des zu löschenden Assets
     * @throws NotFoundException Wenn das Asset nicht gefunden wird
     */
    @DELETE
    @Path("/assets/{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
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
    public Response createForEdc(@PathParam("edcId") String edcIdStr, final Map<String, Object> frontendJson, @Context final UriInfo uriInfo) throws CustomException {
        try {
            UUID edcId;
            try {
                edcId = UUID.fromString(edcIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid EDC ID format: " + edcIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid EDC ID format. Expected UUID.")
                        .build();
            }

            LOG.info("Received request to create asset for EDC: " + edcId);
            LOG.info("Asset data received: " + (frontendJson != null ? frontendJson.toString() : "null"));

            if (frontendJson == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Asset data is missing")
                        .build();
            }

            // Verarbeite das Frontend-Format
            EDCAssetDto assetDto = service.processFrontendAsset(frontendJson, edcId);
            final EDCAssetDto createdAsset = service.createForEdc(edcId, assetDto);

            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(createdAsset.getId().toString())
                    .build();

            LOG.info("Asset created successfully with ID: " + createdAsset.getId());
            return Response.created(uri)
                    .entity(createdAsset)
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
            UUID edcId;
            try {
                edcId = UUID.fromString(edcIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid EDC ID format: " + edcIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid EDC ID format. Expected UUID.")
                        .build();
            }

            LOG.info("Fetching assets for EDC: " + edcId);
            List<EDCAssetDto> assets = service.listAllByEdcId(edcId);

            LOG.info("Returning " + assets.size() + " assets for EDC: " + edcId);
            return Response.ok(assets).build();
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
            UUID edcId;
            try {
                edcId = UUID.fromString(edcIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid EDC ID format: " + edcIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid EDC ID format. Expected UUID.")
                        .build();
            }

            UUID assetId;
            try {
                assetId = UUID.fromString(assetIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid Asset ID format: " + assetIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid Asset ID format. Expected UUID.")
                        .build();
            }

            LOG.info("Fetching asset " + assetId + " for EDC: " + edcId);
            EDCAssetDto asset = service.findByIdAndEdcId(edcId, assetId);

            LOG.info("Found asset with ID: " + asset.getId());
            return Response.ok(asset).build();
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
            UUID edcId;
            try {
                edcId = UUID.fromString(edcIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid EDC ID format: " + edcIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid EDC ID format. Expected UUID.")
                        .build();
            }

            UUID assetId;
            try {
                assetId = UUID.fromString(assetIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid Asset ID format: " + assetIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid Asset ID format. Expected UUID.")
                        .build();
            }

            LOG.info("Updating asset " + assetId + " for EDC: " + edcId);
            LOG.info("Asset data received: " + (frontendJson != null ? frontendJson.toString() : "null"));

            if (frontendJson == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Asset data is missing")
                        .build();
            }

            // Verarbeite das Frontend-Format
            EDCAssetDto assetDto = service.processFrontendAsset(frontendJson, edcId);
            final EDCAssetDto updatedAsset = service.updateForEdc(edcId, assetId, assetDto);

            LOG.info("Asset updated successfully with ID: " + updatedAsset.getId());
            return Response.ok(updatedAsset).build();
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
            UUID edcId;
            try {
                edcId = UUID.fromString(edcIdStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid EDC ID format: " + edcIdStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid EDC ID format. Expected UUID.")
                        .build();
            }

            // AssetId wird als String behandelt (ODRL @id), nicht als UUID
            LOG.info("Deleting asset " + assetIdStr + " for EDC: " + edcId);
            boolean deleted = service.deleteFromEdcByStringId(edcId, assetIdStr);

            if (deleted) {
                LOG.info("Asset deleted successfully");
                return Response.noContent().build();
            } else {
                LOG.warn("Asset not found for deletion");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Asset not found")
                        .build();
            }
        } catch (CustomException e) {
            LOG.error("Error deleting asset: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Error deleting asset: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete asset: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Startet einen Datentransfer für ein Asset.
     *
     * @param id          Die ID des Assets, für das der Datentransfer initiiert werden soll
     * @param requestData Die Request-Daten mit der Ziel-URL
     * @return HTTP-Response mit der Transfer-Prozess-ID
     */
    @POST
    @Path("/{id}/transfer")
    public Response initiateDataTransfer(@PathParam("id") String idStr, Map<String, String> requestData) {
        try {
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid asset ID format: " + idStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid asset ID format. Expected UUID.")
                        .build();
            }

            // Überprüfe, ob die Ziel-URL im Request enthalten ist
            String destinationUrl = requestData.get("destinationUrl");
            if (destinationUrl == null || destinationUrl.isEmpty()) {
                LOG.error("Missing destinationUrl in request");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Missing destinationUrl in request")
                        .build();
            }

            LOG.info("Initiating data transfer for asset: " + id + " to: " + destinationUrl);
            String transferProcessId = service.initiateDataTransfer(id, destinationUrl);

            LOG.info("Data transfer initiated, process ID: " + transferProcessId);
            return Response.ok(Map.of("transferProcessId", transferProcessId)).build();
        } catch (CustomException e) {
            LOG.error("Error initiating data transfer: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Error initiating data transfer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to initiate data transfer: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Überprüft den Status eines Datentransfer-Prozesses.
     *
     * @param id                Die ID des Assets
     * @param transferProcessId Die ID des Transfer-Prozesses
     * @return HTTP-Response mit dem aktuellen Status des Transfers
     */
    @GET
    @Path("/{id}/transfer/{transferProcessId}")
    public Response checkTransferStatus(@PathParam("id") String idStr,
                                        @PathParam("transferProcessId") String transferProcessId) {
        try {
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid asset ID format: " + idStr);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid asset ID format. Expected UUID.")
                        .build();
            }

            LOG.info("Checking transfer status for asset: " + id + ", process: " + transferProcessId);
            String status = service.checkTransferStatus(id, transferProcessId);

            LOG.info("Transfer status: " + status);
            return Response.ok(Map.of("status", status)).build();
        } catch (CustomException e) {
            LOG.error("Error checking transfer status: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Error checking transfer status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to check transfer status: " + e.getMessage())
                    .build();
        }
    }
}
