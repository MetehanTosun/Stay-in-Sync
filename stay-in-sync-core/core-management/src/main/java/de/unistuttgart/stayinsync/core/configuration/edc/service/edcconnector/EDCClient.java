package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;


import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient(configKey = "edc-api")
@Path("/")
public interface EDCClient {

    @POST
    @Path("/assets")
    RestResponse<JsonObject> createAsset(@HeaderParam("X-Api-Key") String apiKey, CreateEDCAssetDTO jsonLdData);

    @POST
    @Path("/policydefinitions")
    RestResponse<JsonObject> createPolicy(@HeaderParam("X-Api-Key") String apiKey, String jsonLdData);

    @POST
    @Path("/contractdefinitions")
    RestResponse<JsonObject> createContractDefinition(@HeaderParam("X-Api-Key") String apiKey, String jsonLdData);

    @GET
    @Path("/assets")
    RestResponse<JsonObject> getAllAssets(@HeaderParam("X-Api-Key") String apiKey);

    @GET
    @Path("/policydefinitions")
    RestResponse<JsonObject> getAllPolicies(@HeaderParam("X-Api-Key") String apiKey);
    
    @GET
    @Path("/policydefinitions/{policyId}")
    RestResponse<JsonObject> getPolicy(@HeaderParam("X-Api-Key") String apiKey, @PathParam("policyId") String policyId);
    
    @DELETE
    @Path("/policydefinitions/{policyId}")
    RestResponse<Void> deletePolicy(@HeaderParam("X-Api-Key") String apiKey, @PathParam("policyId") String policyId);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {

        Log.errorf("Error %d: %s", response.getStatus(), response.readEntity(String.class));

        return null;
    }
}
