package de.unistuttgart.stayinsync.core.configuration.service;

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
    void init(){
        this.webClient = WebClient.create(vertx);
    }

    @Inject
    TypeScriptTypeGenerator typeGenerator;

    public Uni<ArcTestResponseDTO> performTestCall(ArcTestRequestDTO requestDTO){
        SourceSystem system = SourceSystem.<SourceSystem>findByIdOptional(requestDTO.sourceSystemId())
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Source system not found", "Source system with ID %s not found", requestDTO.sourceSystemId()));

        SourceSystemEndpoint endpoint = SourceSystemEndpoint.<SourceSystemEndpoint>findByIdOptional(requestDTO.endpointId())
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Endpoint not found", "Endpoint with ID %s not found", requestDTO.endpointId()));

        String path = endpoint.endpointPath;
        if(requestDTO.pathParameters() != null){
            for(Map.Entry<String, String> entry : requestDTO.pathParameters().entrySet()){
                path = path.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        String fullUrl = system.apiUrl + path;
        Log.infof("Executing test call to: %s", fullUrl);

        var request = webClient.requestAbs(HttpMethod.valueOf(endpoint.httpRequestType.toUpperCase()), fullUrl);

        if(requestDTO.queryParameterValues() != null) {
            requestDTO.queryParameterValues().forEach(request::addQueryParam);
        }

        if(requestDTO.headerValues() != null) {
            requestDTO.headerValues().forEach(request.headers()::add);
        }

        return request
                .send()
                .map(response -> {
                    boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                    String generatedDts = response.getHeader("X-DTS-Generated"); // if you need to echo back some header
                    return new ArcTestResponseDTO(
                            success,
                            response.statusCode(),
                            response.bodyAsJsonObject(),  // Jackson will marshall this into JsonNode
                            generatedDts,
                            success ? null : response.statusMessage()
                    );
                })
                .onFailure().recoverWithItem(th -> {
                    // network error, timeout, etc.
                    return new ArcTestResponseDTO(
                            false,
                            0,
                            null,
                            null,
                            th.getMessage()
                    );
                });
    }
}
