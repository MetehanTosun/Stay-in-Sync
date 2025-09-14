package de.unistuttgart.stayinsync.transformation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.util.GraphMapper;
import de.unistuttgart.stayinsync.syncnode.domain.ExecutionPayload;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.syncjob.TransformationExecutionService;
import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationActionRole;
import de.unistuttgart.stayinsync.core.transport.dto.TransformationMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.targetsystems.ActionMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@QuarkusTest
@QuarkusTestResource(WiremockTestResource.class)
public class TransformationExecutionIntegrationTest {

    @Inject
    TransformationExecutionService executionService;

    @Inject
    @ConfigProperty(name = "quarkus.wiremock.devservices.url")
    String wiremockBaseUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GraphMapper graphMapper;

    private WireMock wiremock;

    @BeforeEach
    void setup() throws URISyntaxException {
        URI uri = new URI(wiremockBaseUrl);
        String host = uri.getHost();
        int port = uri.getPort();

        this.wiremock = new WireMock(host, port);
        this.wiremock.resetMappings();
        this.wiremock.resetRequests();
    }

    @Test
    void testHappyPath_UpdateExistingEntity() throws JsonProcessingException {
        ExecutionPayload payload = createTestExecutionPayload(wiremockBaseUrl);
        wiremock.register(WireMock.get(WireMock.urlEqualTo("/products/search?sku=TEST-SKU-001"))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": \"real-product-id-123\", \"name\": \"Old Name\"}")));
        wiremock.register(WireMock.put(WireMock.urlEqualTo("/products/real-product-id-123"))
                .willReturn(WireMock.aResponse().withStatus(200)));

        executionService.execute(payload).await().indefinitely();

        wiremock.verifyThat(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/products/search?sku=TEST-SKU-001")));
        Map<String, Object> expectedPutPayload = Map.of("name", "Transformed Awesome Product", "price", 129.99);
        wiremock.verifyThat(1, WireMock.putRequestedFor(WireMock.urlEqualTo("/products/real-product-id-123"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(expectedPutPayload))));
        wiremock.verifyThat(0, WireMock.postRequestedFor(WireMock.urlMatching("/products")));
    }

    // === HELPER METHODS FOR DATA GENERATION ===
    private ExecutionPayload createTestExecutionPayload(String mockApiBaseUrl) {
        TransformationMessageDTO txContext = createTransformationContext(mockApiBaseUrl);
        List<Node> graphNodes = createGraphNodes();
        TransformJob transformJob = new TransformJob(1L,
                "Test Job", "job-id-1", "test-script", generateUserScript(), "js",
                "user-script-hash", generateTestSdk(), "sdk-hash", createSourceData()
        );
        return new ExecutionPayload(transformJob, graphNodes, txContext);
    }

    private TransformationMessageDTO createTransformationContext(String mockApiBaseUrl) {
        Set<RequestConfigurationMessageDTO> targetArcs = Set.of(
                new RequestConfigurationMessageDTO("synchronizeProducts", mockApiBaseUrl,
                        List.of(
                                new ActionMessageDTO(TargetApiRequestConfigurationActionRole.CHECK, 1, "GET", "/products/search"),
                                new ActionMessageDTO(TargetApiRequestConfigurationActionRole.CREATE, 2, "POST", "/products"),
                                new ActionMessageDTO(TargetApiRequestConfigurationActionRole.UPDATE, 3, "PUT", "/products/{id}")
                        )
                )
        );
        return new TransformationMessageDTO(1L, "Test Transformation", null, null,
                JobDeploymentStatus.DEPLOYED, Set.of(), List.of(), targetArcs);
    }

    private List<Node> createGraphNodes() {
        String graphJson = "{\"nodes\":[{\"id\":0,\"name\":\"Final Result\",\"offsetX\":0.0,\"offsetY\":0.0,\"nodeType\":\"FINAL\",\"inputNodes\":null,\"arcId\":null,\"jsonPath\":null,\"value\":null,\"operatorType\":null,\"inputTypes\":null,\"outputType\":null,\"inputLimit\":null}]}";

        try {
            GraphDTO graphDto = objectMapper.readValue(graphJson, GraphDTO.class);
            GraphMapper.MappingResult mappingResult = graphMapper.toNodeGraph(graphDto);

            if (!mappingResult.mappingErrors().isEmpty()) {
                throw new RuntimeException("Failed to map graph for test setup: " + mappingResult.mappingErrors());
            }

            return mappingResult.nodes();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse graph JSON for test setup", e);
        }
    }

    private String generateTestSdk() {
        return "var targets = {}; (function() { 'use strict'; const arcAlias = 'synchronizeProducts'; class UpsertBuilder { constructor() { this.directive = { __directiveType: arcAlias + '_UpsertDirective', checkConfiguration: {}, createConfiguration: {}, updateConfiguration: {} }; } usingCheck(c) { const s = this; const b = { withQueryParamSku: function(v) { s.directive.checkConfiguration.parameters = s.directive.checkConfiguration.parameters || {}; s.directive.checkConfiguration.parameters.query = s.directive.checkConfiguration.parameters.query || {}; s.directive.checkConfiguration.parameters.query['sku'] = v; return this; } }; c(b); return s; } usingCreate(c) { const s = this; const b = { withPayload: function(p) { s.directive.createConfiguration.payload = p; return this; } }; c(b); return s; } usingUpdate(c) { const s = this; const b = { withPathId: function(v) { if(typeof v === 'function') { s.directive.updateConfiguration.pathParameters = s.directive.updateConfiguration.pathParameters || {}; s.directive.updateConfiguration.pathParameters['id'] = '{{checkResponse.body.id}}'; } else { s.directive.updateConfiguration.pathParameters['id'] = v; } return this; }, withPayload: function(p) { s.directive.updateConfiguration.payload = p; return this; } }; c(b); return s; } build() { return this.directive; } } targets['synchronizeProducts'] = { defineUpsert: function () { return new UpsertBuilder(); } }; })();";
    }

    private String generateUserScript() {
        return "function transform() { stayinsync.log('User script started.', 'INFO'); const product = source.myCrm.products.data[0]; const directive = targets.synchronizeProducts.defineUpsert() .usingCheck(config => { config.withQueryParamSku(product.sku); }) .usingCreate(config => { config.withPayload({ name: 'Transformed ' + product.name, price: product.price + 30.0 }); }) .usingUpdate(config => { config.withPathId(checkResponse => checkResponse.id).withPayload({ name: 'Transformed ' + product.name, price: product.price + 30.0 }); }) .build(); const directiveMap = { synchronizeProducts: [directive] }; return directiveMap; }";
    }

    private Object createSourceData() {
        Map<String, Object> product = Map.of("sku", "TEST-SKU-001", "name", "Awesome Product", "price", 99.99);
        Map<String, Object> productsArc = Map.of("data", List.of(product));
        Map<String, Object> myCrmSystem = Map.of("products", productsArc);
        return Map.of("source", Map.of("myCrm", myCrmSystem));
    }
}
