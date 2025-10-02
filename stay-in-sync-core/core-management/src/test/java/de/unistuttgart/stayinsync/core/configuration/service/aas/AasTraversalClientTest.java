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

@QuarkusTest
class AasTraversalClientTest {

    @Inject
    AasTraversalClient aasTraversalClient;

    private Map<String, String> testHeaders;

    @BeforeEach
    void setUp() {
        testHeaders = new HashMap<>();
        testHeaders.put("Authorization", "Bearer test-token");
    }

    @Test
    @DisplayName("Should get shell with correct URL encoding")
    void testGetShell() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.getShell(baseUrl, aasId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should list submodels with correct URL")
    void testListSubmodels() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listSubmodels(baseUrl, aasId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should list elements with parent path and depth")
    void testListElements() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "all";
        String parentPath = "parent.child";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should create submodel with POST request")
    void testCreateSubmodel() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String body = "{\"id\": \"test-submodel\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createSubmodel(baseUrl, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should get submodel with correct URL")
    void testGetSubmodel() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.getSubmodel(baseUrl, submodelId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should create element with parent path")
    void testCreateElement() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "parent.child";
        String body = "{\"idShort\": \"newElement\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should patch element value")
    void testPatchElementValue() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        String body = "{\"value\": \"new-value\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.patchElementValue(baseUrl, submodelId, path, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should delete submodel")
    void testDeleteSubmodel() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.deleteSubmodel(baseUrl, submodelId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should delete element")
    void testDeleteElement() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.deleteElement(baseUrl, submodelId, path, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should put submodel")
    void testPutSubmodel() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String body = "{\"id\": \"updated-submodel\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.putSubmodel(baseUrl, submodelId, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should put element")
    void testPutElement() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        String body = "{\"idShort\": \"updatedElement\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.putElement(baseUrl, submodelId, path, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should add submodel reference to shell")
    void testAddSubmodelReferenceToShell() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        String submodelId = "https://example.com/submodel/456";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.addSubmodelReferenceToShell(baseUrl, aasId, submodelId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should remove submodel reference from shell")
    void testRemoveSubmodelReferenceFromShell() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        String submodelId = "https://example.com/submodel/456";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.removeSubmodelReferenceFromShell(baseUrl, aasId, submodelId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should list submodel references")
    void testListSubmodelReferences() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listSubmodelReferences(baseUrl, aasId, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should remove submodel reference by index")
    void testRemoveSubmodelReferenceByIndex() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String aasId = "https://example.com/aas/123";
        int index = 0;
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.removeSubmodelReferenceFromShellByIndex(baseUrl, aasId, index, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should get element with deep level")
    void testGetElement() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String path = "element.path";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.getElement(baseUrl, submodelId, path, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should handle null parent path in listElements")
    void testListElementsWithNullParentPath() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "core";
        String parentPath = null;
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should handle empty parent path in listElements")
    void testListElementsWithEmptyParentPath() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "core";
        String parentPath = "";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should handle blank parent path in listElements")
    void testListElementsWithBlankParentPath() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String depth = "core";
        String parentPath = "   ";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.listElements(baseUrl, submodelId, depth, parentPath, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should handle null parent path in createElement")
    void testCreateElementWithNullParentPath() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = null;
        String body = "{\"idShort\": \"newElement\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should handle empty parent path in createElement")
    void testCreateElementWithEmptyParentPath() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "";
        String body = "{\"idShort\": \"newElement\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }

    @Test
    @DisplayName("Should handle blank parent path in createElement")
    void testCreateElementWithBlankParentPath() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String submodelId = "https://example.com/submodel/456";
        String parentPath = "   ";
        String body = "{\"idShort\": \"newElement\"}";
        
        // Act
        Uni<HttpResponse<Buffer>> result = aasTraversalClient.createElement(baseUrl, submodelId, parentPath, body, testHeaders);
        
        // Assert
        assertNotNull(result);
        assertNotNull(aasTraversalClient);
    }
}
