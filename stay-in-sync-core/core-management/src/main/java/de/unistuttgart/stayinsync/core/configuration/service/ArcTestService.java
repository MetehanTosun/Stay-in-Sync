package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ArcTestRequestDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ArcTestResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@ApplicationScoped
public class ArcTestService {

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    @Inject
    TypeScriptTypeGenerator typeGenerator; // TODO: Serialize to Type and display as tree in frontend

    public Uni<ArcTestResponseDTO> performTestCall(ArcTestRequestDTO requestDTO) {
        SourceSystem system = SourceSystem.<SourceSystem>findByIdOptional(requestDTO.sourceSystemId())
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Source system not found", "Source system with ID %s not found", requestDTO.sourceSystemId()));

        SourceSystemEndpoint endpoint = SourceSystemEndpoint.<SourceSystemEndpoint>findByIdOptional(requestDTO.endpointId())
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Endpoint not found", "Endpoint with ID %s not found", requestDTO.endpointId()));

        String path = endpoint.endpointPath;
        if (requestDTO.pathParameters() != null) {
            for (Map.Entry<String, String> entry : requestDTO.pathParameters().entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        String fullUrl = system.apiUrl + path;
        Log.infof("Executing test call to: %s", fullUrl);

        var request = webClient.requestAbs(HttpMethod.valueOf(endpoint.httpRequestType.toUpperCase()), fullUrl);

        if (requestDTO.queryParameterValues() != null) {
            requestDTO.queryParameterValues().forEach(request::addQueryParam);
        }

        if (requestDTO.headerValues() != null) {
            requestDTO.headerValues().forEach(request.headers()::add);
        }

        return request
                .send()
                .map(response -> {
                    int statusCode = response.statusCode();
                    boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

                    if (!success) {
                        return new ArcTestResponseDTO(
                                false,
                                statusCode,
                                null,
                                null,
                                response.statusMessage()
                        );
                    }

                    String responseBody = response.bodyAsString();

                    if (responseBody == null || responseBody.isEmpty()) {
                        return new ArcTestResponseDTO(
                                true,
                                statusCode,
                                null,
                                "export type Root = any;",
                                null
                        );
                    }
                    try {
                        String generatedDts = typeGenerator.generate(response.bodyAsString());
                        return new ArcTestResponseDTO(
                                true,
                                response.statusCode(),
                                response.bodyAsJson(JsonNode.class),
                                generatedDts,
                                null
                        );
                    } catch (JsonProcessingException e) {
                        Log.warnf(e, "Could not generate DTS. Response may not be valid JSON.");
                        return new ArcTestResponseDTO(
                                true,
                                statusCode,
                                responseBody,
                                "/* Could not generate type: Response was not valid JSON. */",
                                "Response was not valid JSON."
                        );
                    }
                })
                .onFailure().recoverWithItem(th -> new ArcTestResponseDTO(
                        false,
                        503,
                        null,
                        null,
                        "Network Error: " + th.getMessage()
                ));
    }
}
