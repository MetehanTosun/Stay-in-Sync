package de.unistuttgart.stayinsync.core.configuration.edc.client;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;

@RegisterRestClient(configKey = "edc-api")
@Path("/assets")
public interface AssetEdcClient {

    @GET
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> getAssetById(@HeaderParam("X-Api-Key") String apiKey,
                                          @PathParam("id") String assetId);

    @POST
    @Path("/request")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> getAllAssets(@HeaderParam("X-Api-Key") String apiKey);

    @POST
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> createAsset(@HeaderParam("X-Api-Key") String apiKey, AssetDto jsonLdData);

    @PUT
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> updateAsset(@HeaderParam("X-Api-Key") String apiKey,
                                         @PathParam("id") String assetId,
                                         AssetDto jsonLdData);
    @DELETE
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Void> deleteAsset(@HeaderParam("X-Api-Key") String apiKey,
                                   @PathParam("id") String assetId);


    static AssetEdcClient createClient(final String baseUrl) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(  baseUrl))
                .build(AssetEdcClient.class);
    }
}
