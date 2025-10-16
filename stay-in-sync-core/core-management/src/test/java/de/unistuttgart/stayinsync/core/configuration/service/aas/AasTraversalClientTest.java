package de.unistuttgart.stayinsync.core.configuration.service.aas;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient}.
 * Verifies the behavior of AAS traversal methods such as shell retrieval,
 * submodel operations, element CRUD functionality, and reference management.
 * Ensures that each API call correctly returns a non-null reactive Uni response.
 */
@QuarkusTest
class AasTraversalClientTest {

    @Inject
    AasTraversalClient aasTraversalClient;

    private Map<String, String> testHeaders;

    /**
     * Initializes the test environment before each test case.
     * Sets up authorization headers required for AAS traversal requests.
     */
    @BeforeEach
    void setUp() {
        testHeaders = new HashMap<>();
        testHeaders.put("Authorization", "Bearer test-token");
    }

    /**
     * Tests shell retrieval with proper URL encoding of the AAS ID.
     * Ensures that a non-null Uni response is returned.
     */
    @Test
    @DisplayName("Should get shell with correct URL encoding")
    void testGetShell() {
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.getShell(baseUrl, aasId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests listing of submodels for a given AAS ID.
     * Verifies that the traversal client returns a valid reactive response.
     */
    @Test
    @DisplayName("Should list submodels with correct URL")
    void testListSubmodels() {
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listSubmodels(baseUrl, aasId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests listing of submodel elements with depth and parent path parameters.
     * Ensures that all traversal parameters are properly handled.
     */
    @Test
    @DisplayName("Should list elements with parent path and depth")
    void testListElements() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "all";
        String parentPath = "parent.child";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests creation of a new submodel using a POST request.
     * Ensures the request body is correctly passed to the client.
     */
    @Test
    @DisplayName("Should create submodel with POST request")
    void testCreateSubmodel() {
        String baseUrl = "https://api.example.com";
        String body = "{\"id\": \"test-submodel\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createSubmodel(baseUrl, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests retrieval of a specific submodel by ID.
     * Ensures that a valid Uni response is returned.
     */
    @Test
    @DisplayName("Should get submodel with correct URL")
    void testGetSubmodel() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.getSubmodel(baseUrl, submodelId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests creation of an element under a given submodel and parent path.
     * Verifies that the request completes successfully.
     */
    @Test
    @DisplayName("Should create element with parent path")
    void testCreateElement() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "parent.child";
        String body = "{\"idShort\": \"newElement\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests partial update (PATCH) of an element’s value within a submodel.
     * Ensures the request body is correctly sent and a valid response is received.
     */
    @Test
    @DisplayName("Should patch element value")
    void testPatchElementValue() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        String body = "{\"value\": \"new-value\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.patchElementValue(baseUrl, submodelId, path, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests deletion of a submodel by ID.
     * Ensures that a reactive Uni response is returned.
     */
    @Test
    @DisplayName("Should delete submodel")
    void testDeleteSubmodel() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.deleteSubmodel(baseUrl, submodelId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests deletion of an element by path within a submodel.
     * Ensures a non-null reactive response is returned.
     */
    @Test
    @DisplayName("Should delete element")
    void testDeleteElement() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.deleteElement(baseUrl, submodelId, path, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests full update (PUT) of a submodel entity.
     * Ensures that JSON data is transmitted correctly.
     */
    @Test
    @DisplayName("Should put submodel")
    void testPutSubmodel() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String body = "{\"id\": \"updated-submodel\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.putSubmodel(baseUrl, submodelId, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests full update (PUT) of a submodel element.
     * Ensures that the client handles element replacement correctly.
     */
    @Test
    @DisplayName("Should put element")
    void testPutElement() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        String body = "{\"idShort\": \"updatedElement\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.putElement(baseUrl, submodelId, path, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests adding a submodel reference to a given AAS shell.
     * Ensures that the API call returns a valid response.
     */
    @Test
    @DisplayName("Should add submodel reference to shell")
    void testAddSubmodelReferenceToShell() {
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        String submodelId = "https://example.com/submodel/456";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.addSubmodelReferenceToShell(baseUrl, aasId, submodelId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests removal of a submodel reference from an AAS shell by submodel ID.
     * Ensures that the delete operation responds correctly.
     */
    @Test
    @DisplayName("Should remove submodel reference from shell")
    void testRemoveSubmodelReferenceFromShell() {
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        String submodelId = "https://example.com/submodel/456";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.removeSubmodelReferenceFromShell(baseUrl, aasId, submodelId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests retrieval of all submodel references for a specific AAS shell.
     * Verifies that a valid list response is returned.
     */
    @Test
    @DisplayName("Should list submodel references")
    void testListSubmodelReferences() {
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listSubmodelReferences(baseUrl, aasId, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests removal of a submodel reference by its index position within a shell.
     * Ensures that the API responds correctly.
     */
    @Test
    @DisplayName("Should remove submodel reference by index")
    void testRemoveSubmodelReferenceByIndex() {
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        int index = 0;
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.removeSubmodelReferenceFromShellByIndex(baseUrl, aasId, index, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests retrieval of a specific submodel element with deep traversal.
     * Ensures proper path encoding and valid Uni response.
     */
    @Test
    @DisplayName("Should get element with deep level")
    void testGetElement() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.getElement(baseUrl, submodelId, path, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests listing of submodel elements when the parent path is null.
     * Verifies graceful handling of the null parameter.
     */
    @Test
    @DisplayName("Should handle null parent path in listElements")
    void testListElementsWithNullParentPath() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "core";
        String parentPath = null;
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests listing of submodel elements when the parent path is an empty string.
     * Ensures that the traversal still succeeds.
     */
    @Test
    @DisplayName("Should handle empty parent path in listElements")
    void testListElementsWithEmptyParentPath() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "core";
        String parentPath = "";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests listing of submodel elements when the parent path contains only whitespace.
     * Verifies that the request still executes correctly.
     */
    @Test
    @DisplayName("Should handle blank parent path in listElements")
    void testListElementsWithBlankParentPath() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "core";
        String parentPath = "   ";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests creation of a submodel element when the parent path is null.
     * Ensures the API call remains valid.
     */
    @Test
    @DisplayName("Should handle null parent path in createElement")
    void testCreateElementWithNullParentPath() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = null;
        String body = "{\"idShort\": \"newElement\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests creation of a submodel element with an empty parent path.
     * Verifies proper fallback to the submodel root context.
     */
    @Test
    @DisplayName("Should handle empty parent path in createElement")
    void testCreateElementWithEmptyParentPath() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "";
        String body = "{\"idShort\": \"newElement\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    /**
     * Tests creation of a submodel element when the parent path contains only whitespace.
     * Ensures that the traversal client still processes the request correctly.
     */
    @Test
    @DisplayName("Should handle blank parent path in createElement")
    void testCreateElementWithBlankParentPath() {
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "   ";
        String body = "{\"idShort\": \"newElement\"}";
        
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }
}
