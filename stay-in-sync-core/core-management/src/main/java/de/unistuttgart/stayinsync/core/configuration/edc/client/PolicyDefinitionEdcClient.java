package de.unistuttgart.stayinsync.core.configuration.edc.client;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDefinitionDto;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;

@RegisterRestClient(configKey = "edc-api")
@Path("/policies")
public interface PolicyDefinitionEdcClient {

    @GET
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> getPolicyDefinitionById(@HeaderParam("X-Api-Key") String apiKey,
                                                 @PathParam("id") String policyDefinitionId);

    @POST
    @Path("/request")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> getAllPolicyDefinitions(@HeaderParam("X-Api-Key") String apiKey);

    @POST
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> createPolicyDefinition(@HeaderParam("X-Api-Key") String apiKey, PolicyDefinitionDto jsonLdData);

    @PUT
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> updatePolicyDefinition(@HeaderParam("X-Api-Key") String apiKey,
                                                @PathParam("id") String policyDefinitionId,
                                                PolicyDefinitionDto jsonLdData);

    @DELETE
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Void> deletePolicyDefinition(@HeaderParam("X-Api-Key") String apiKey,
                                              @PathParam("id") String policyDefinitionId);



    static PolicyDefinitionEdcClient createClient(final String baseUrl) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(PolicyDefinitionEdcClient.class);
    }
}