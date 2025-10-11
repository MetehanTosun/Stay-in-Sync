package de.unistuttgart.stayinsync.core.configuration.edc.client;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContractDefinitionDto;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;

@RegisterRestClient(configKey = "edc-api")
@Path("/contractdefinitions")
public interface ContractDefinitionEdcClient {

    @GET
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> getContractDefinitionById(@HeaderParam("X-Api-Key") String apiKey,
                                                   @PathParam("id") String contractDefinitionId);

    @POST
    @Path("/request")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> getAllContractDefinitions(@HeaderParam("X-Api-Key") String apiKey);

    @POST
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> createContractDefinition(@HeaderParam("X-Api-Key") String apiKey, ContractDefinitionDto jsonLdData);

    @PUT
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> updateContractDefinition(@HeaderParam("X-Api-Key") String apiKey,
                                                  @PathParam("id") String contractDefinitionId,
                                                  ContractDefinitionDto jsonLdData);

    @DELETE
    @Path("/{id}")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Void> deleteContractDefinition(@HeaderParam("X-Api-Key") String apiKey,
                                                @PathParam("id") String contractDefinitionId);


    static ContractDefinitionEdcClient createClient(final String baseUrl) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(ContractDefinitionEdcClient.class);
    }
}
