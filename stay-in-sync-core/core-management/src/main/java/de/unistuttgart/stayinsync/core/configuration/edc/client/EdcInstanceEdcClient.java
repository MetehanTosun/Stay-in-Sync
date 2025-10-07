package de.unistuttgart.stayinsync.core.configuration.edc.client;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient(configKey = "edc-api")
public interface EdcInstanceEdcClient {

    @POST
    @Path("assets/request")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> checkForAssetEndpoint(@HeaderParam("X-Api-Key") String apiKey);

    @POST
    @Path("policies/request")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> checkForPolicyEndpoint(@HeaderParam("X-Api-Key") String apiKey);

    @POST
    @Path("contract-definitions/request")
    @JsonView(VisibilitySidesForDto.Edc.class)
    RestResponse<Object> checkForContractDefinitionEndpoint(@HeaderParam("X-Api-Key") String apiKey);
}
