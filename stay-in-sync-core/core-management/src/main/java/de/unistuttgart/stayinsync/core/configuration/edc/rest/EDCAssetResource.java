package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.EDCAssetMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.EDCAssetService;
import de.unistuttgart.stayinsync.transport.exception.CustomException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@Path("/api/config/edcs/assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EDCAssetResource {

    private static final Logger LOG = Logger.getLogger(EDCAssetResource.class);

    @Inject
    EDCAssetService service;

    @GET
    public List<EDCAssetDto> list() {
        return service.listAll();
    }
    
    @GET
    @Path("{id}")
    public EDCAssetDto get(@PathParam("id") UUID id) throws CustomException {
        return service.findById(id);
            
    }

    @POST
    @Transactional
    public Response create(final EDCAssetDto assetDto, @Context final UriInfo uriInfo) {
        final EDCAssetDto createdAsset  = service.create(assetDto);

        URI uri = uriInfo.getAbsolutePathBuilder()
                         .path(createdAsset.id().toString())
                         .build();
        return Response.created(uri)
                       .entity(createdAsset)
                       .build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public EDCAssetDto update(@PathParam("id") UUID id, final EDCAssetDto assetDto) throws CustomException {
        return service.update(id, assetDto);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") UUID id) {
        if (!service.delete(id)) {
            throw new NotFoundException("Asset " + id + " nicht gefunden");
        }
    }
    
    /**
     * Create a new asset for a specific EDC instance.
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
                             .path(createdAsset.id().toString())
                             .build();
            
            LOG.info("Asset created successfully with ID: " + createdAsset.id());
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
     * Get all assets for a specific EDC instance.
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
     * Get a specific asset for a specific EDC instance.
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
            
            LOG.info("Found asset with ID: " + asset.id());
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
     * Update a specific asset for a specific EDC instance.
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
            
            LOG.info("Asset updated successfully with ID: " + updatedAsset.id());
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
     * Delete a specific asset for a specific EDC instance.
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
}
