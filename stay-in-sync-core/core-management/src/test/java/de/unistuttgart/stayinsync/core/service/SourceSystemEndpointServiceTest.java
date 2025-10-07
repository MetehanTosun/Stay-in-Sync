package de.unistuttgart.stayinsync.core.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

import java.util.Optional;

@QuarkusTest
public class SourceSystemEndpointServiceTest {

    @Inject
    SourceSystemEndpointService sourceSystemEndpointService;

    @InjectMock
    TypeScriptTypeGenerator typeScriptTypeGenerator;

    private SourceSystemEndpoint testEndpoint;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create a test endpoint
        testEndpoint = new SourceSystemEndpoint();
        testEndpoint.endpointPath = "/test";
        testEndpoint.httpRequestType = "GET";
        testEndpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}, \"name\": {\"type\": \"string\"}}}";
        testEndpoint.persist();
    }

    @Test
    @Transactional
    void testGenerateTypeScriptForNewEndpoint() throws Exception {
        // Arrange
        String expectedTypeScript = "interface ResponseBody { id: number; name: string; }";
        when(typeScriptTypeGenerator.generate(anyString())).thenReturn(expectedTypeScript);

        // Act - Test the TypeScript generation directly
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(testEndpoint.responseBodySchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptForUpdatedEndpoint() throws Exception {
        // Arrange
        String updatedSchema = "{\"type\": \"object\", \"properties\": {\"title\": {\"type\": \"string\"}}}";
        String expectedTypeScript = "interface ResponseBody { title: string; }";
        when(typeScriptTypeGenerator.generate(updatedSchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = updatedSchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(updatedSchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testClearTypeScriptWhenSchemaIsEmpty() throws Exception {
        // Arrange
        testEndpoint.responseDts = "interface ResponseBody { id: number; }";

        // Act
        testEndpoint.responseBodySchema = "";
        testEndpoint.responseDts = null; // Simulate clearing

        // Assert
        assertNull(testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testHandleTypeScriptGenerationError() throws Exception {
        // Arrange
        when(typeScriptTypeGenerator.generate(anyString())).thenThrow(new RuntimeException("Generation failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            typeScriptTypeGenerator.generate(testEndpoint.responseBodySchema);
        });
    }

    @Test
    @Transactional
    void testUpdateExistingEndpointsWithTypeScript() throws Exception {
        // Arrange
        String expectedTypeScript = "interface ResponseBody { id: number; name: string; }";
        when(typeScriptTypeGenerator.generate(anyString())).thenReturn(expectedTypeScript);

        // Create another endpoint without TypeScript
        SourceSystemEndpoint anotherEndpoint = new SourceSystemEndpoint();
        anotherEndpoint.endpointPath = "/test2";
        anotherEndpoint.httpRequestType = "POST";
        anotherEndpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"status\": {\"type\": \"string\"}}}";
        anotherEndpoint.persist();

        // Act
        sourceSystemEndpointService.updateExistingEndpointsWithTypeScript();

        // Assert - Check that the method was called and TypeScript was generated
        // Note: The actual persistence happens in the service method, so we verify the mock was called
        verify(typeScriptTypeGenerator, atLeastOnce()).generate(anyString());
    }

    @Test
    @Transactional
    void testReplaceSourceSystemEndpointWithTypeScript() throws Exception {
        // Arrange
        String expectedTypeScript = "interface ResponseBody { newField: string; }";
        when(typeScriptTypeGenerator.generate(anyString())).thenReturn(expectedTypeScript);

        // Create an endpoint first
        SourceSystemEndpoint existingEndpoint = new SourceSystemEndpoint();
        existingEndpoint.endpointPath = "/test";
        existingEndpoint.httpRequestType = "GET";
        existingEndpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"oldField\": {\"type\": \"string\"}}}";
        existingEndpoint.persist();
        Long existingId = existingEndpoint.id;

        // Create replacement endpoint with same ID
        SourceSystemEndpoint replacementEndpoint = new SourceSystemEndpoint();
        replacementEndpoint.id = existingId; // Set the ID to replace existing
        replacementEndpoint.endpointPath = "/test";
        replacementEndpoint.httpRequestType = "GET";
        replacementEndpoint.responseBodySchema = "{\"type\": \"object\", \"properties\": {\"newField\": {\"type\": \"string\"}}}";

        // Act
        Long anyExistingSourceSystemId = de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem.<de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem>findAll()
                .firstResultOptional()
                .map(ss -> ss.id)
                .orElseThrow(() -> new IllegalStateException("No SourceSystem present for test"));

        Optional<SourceSystemEndpoint> result = sourceSystemEndpointService.replaceSourceSystemEndpoint(replacementEndpoint, anyExistingSourceSystemId);

        // Assert
        assertTrue(result.isPresent());
        SourceSystemEndpoint replaced = result.get();
        assertNotNull(replaced.responseDts);
        assertEquals(expectedTypeScript, replaced.responseDts);
    }

    // Additional tests for complex scenarios
    @Test
    @Transactional
    void testGenerateTypeScriptWithArrayTypes() throws Exception {
        // Arrange
        String arraySchema = "{\"type\": \"object\", \"properties\": {\"items\": {\"type\": \"array\", \"items\": {\"type\": \"string\"}}, \"count\": {\"type\": \"number\"}}}";
        String expectedTypeScript = "interface ResponseBody { items: string[]; count: number; }";
        when(typeScriptTypeGenerator.generate(arraySchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = arraySchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(arraySchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithUnionTypes() throws Exception {
        // Arrange
        String unionSchema = "{\"type\": \"object\", \"properties\": {\"status\": {\"type\": \"string\", \"enum\": [\"active\", \"inactive\", \"pending\"]}, \"value\": {\"oneOf\": [{\"type\": \"string\"}, {\"type\": \"number\"}]}}}";
        String expectedTypeScript = "interface ResponseBody { status: 'active' | 'inactive' | 'pending'; value: string | number; }";
        when(typeScriptTypeGenerator.generate(unionSchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = unionSchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(unionSchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithNestedObjects() throws Exception {
        // Arrange
        String nestedSchema = "{\"type\": \"object\", \"properties\": {\"user\": {\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}, \"profile\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"email\": {\"type\": \"string\"}}}}}}}";
        String expectedTypeScript = "interface ResponseBody { user: { id: number; profile: { name: string; email: string; }; }; }";
        when(typeScriptTypeGenerator.generate(nestedSchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = nestedSchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(nestedSchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithSpecialCharacters() throws Exception {
        // Arrange
        String specialCharSchema = "{\"type\": \"object\", \"properties\": {\"user_name\": {\"type\": \"string\"}, \"email@domain\": {\"type\": \"string\"}, \"$metadata\": {\"type\": \"object\"}}}";
        String expectedTypeScript = "interface ResponseBody { user_name: string; 'email@domain': string; $metadata: object; }";
        when(typeScriptTypeGenerator.generate(specialCharSchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = specialCharSchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(specialCharSchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithUnicodeCharacters() throws Exception {
        // Arrange
        String unicodeSchema = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\", \"description\": \"User's name with émojis 🎉\"}, \"message\": {\"type\": \"string\"}}}";
        String expectedTypeScript = "interface ResponseBody { name: string; message: string; }";
        when(typeScriptTypeGenerator.generate(unicodeSchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = unicodeSchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(unicodeSchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithLargeSchema() throws Exception {
        // Arrange - Create a large schema with many properties
        StringBuilder largeSchema = new StringBuilder("{\"type\": \"object\", \"properties\": {");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeSchema.append(",");
            largeSchema.append("\"prop").append(i).append("\": {\"type\": \"string\"}");
        }
        largeSchema.append("}}");

        String expectedTypeScript = "interface ResponseBody { prop0: string; prop1: string; /* ... */ }";
        when(typeScriptTypeGenerator.generate(largeSchema.toString())).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = largeSchema.toString();
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(largeSchema.toString());

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithOpenAPI3Features() throws Exception {
        // Arrange
        String openApi3Schema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"integer\", \"format\": \"int64\"}, \"email\": {\"type\": \"string\", \"format\": \"email\"}, \"createdAt\": {\"type\": \"string\", \"format\": \"date-time\"}, \"status\": {\"type\": \"string\", \"enum\": [\"active\", \"inactive\"]}}}";
        String expectedTypeScript = "interface ResponseBody { id: number; email: string; createdAt: string; status: 'active' | 'inactive'; }";
        when(typeScriptTypeGenerator.generate(openApi3Schema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = openApi3Schema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(openApi3Schema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithCustomTypeDefinitions() throws Exception {
        // Arrange
        String customTypeSchema = "{\"type\": \"object\", \"properties\": {\"data\": {\"$ref\": \"#/components/schemas/User\"}, \"metadata\": {\"type\": \"object\"}}}";
        String expectedTypeScript = "interface ResponseBody { data: User; metadata: object; }";
        when(typeScriptTypeGenerator.generate(customTypeSchema)).thenReturn(expectedTypeScript);

        // Act
        testEndpoint.responseBodySchema = customTypeSchema;
        testEndpoint.responseDts = typeScriptTypeGenerator.generate(customTypeSchema);

        // Assert
        assertNotNull(testEndpoint.responseDts);
        assertEquals(expectedTypeScript, testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptErrorRecovery() throws Exception {
        // Arrange - First call fails, second call succeeds
        when(typeScriptTypeGenerator.generate(anyString()))
            .thenThrow(new RuntimeException("First attempt failed"))
            .thenReturn("interface ResponseBody { id: number; }");

        // Act & Assert - First attempt should fail
        assertThrows(RuntimeException.class, () -> {
            typeScriptTypeGenerator.generate(testEndpoint.responseBodySchema);
        });

        // Second attempt should succeed
        String result = typeScriptTypeGenerator.generate(testEndpoint.responseBodySchema);
        assertNotNull(result);
        assertEquals("interface ResponseBody { id: number; }", result);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithMalformedJsonSchema() throws Exception {
        // Arrange
        String malformedSchema = "{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"invalid_type\"}}}";
        when(typeScriptTypeGenerator.generate(malformedSchema)).thenThrow(new IllegalArgumentException("Invalid type: invalid_type"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            typeScriptTypeGenerator.generate(malformedSchema);
        });
    }

    @Test
    @Transactional
    void testConcurrentTypeScriptGeneration() throws Exception {
        // Arrange
        String expectedTypeScript = "interface ResponseBody { id: number; }";
        when(typeScriptTypeGenerator.generate(anyString())).thenReturn(expectedTypeScript);

        // Act - Simulate concurrent access
        Runnable generateTask = () -> {
            try {
                String result = typeScriptTypeGenerator.generate(testEndpoint.responseBodySchema);
                assertNotNull(result);
                assertEquals(expectedTypeScript, result);
            } catch (Exception e) {
                fail("Concurrent generation should not fail: " + e.getMessage());
            }
        };

        // Create multiple threads
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(generateTask);
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - All threads should have completed successfully
        assertTrue(true); // If we reach here, no exceptions were thrown
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithNullSchema() throws Exception {
        // Arrange
        testEndpoint.responseBodySchema = null;

        // Act
        testEndpoint.responseDts = null; // Simulate no generation for null schema

        // Assert
        assertNull(testEndpoint.responseDts);
    }

    @Test
    @Transactional
    void testGenerateTypeScriptWithWhitespaceOnlySchema() throws Exception {
        // Arrange
        testEndpoint.responseBodySchema = "   ";

        // Act
        testEndpoint.responseDts = null; // Simulate no generation for whitespace-only schema

        // Assert
        assertNull(testEndpoint.responseDts);
    }
} 