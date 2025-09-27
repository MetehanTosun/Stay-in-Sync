package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;


import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient

public interface EDCClient {

    @POST
    @Path("/assets")
    @HeaderParam("X-Api-Key")
    RestResponse<JsonObject> createAsset(@HeaderParam("X-Api-Key") String apiKey, CreateEDCAssetDTO jsonLdData);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {

        Log.errorf("Error %d: %s", response.getStatus(), response.readEntity(String.class));

        return null;
    }
}
