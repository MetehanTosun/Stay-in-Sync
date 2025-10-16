package nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.nodes.JsonPathValueExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonPathValueExtractor Tests")
public class JsonPathValueExtractorTest {

    private JsonPathValueExtractor extractor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        extractor = new JsonPathValueExtractor();
        objectMapper = new ObjectMapper();
    }

    /**
     * Helper method to parse JSON without checked exception handling in tests
     */
    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test JSON: " + json, e);
        }
    }

    // ===== ROOT PATH TESTS =====

    @Test
    @DisplayName("should return root node for root path '/'")
    void testExtractValue_WithRootSlash_ShouldReturnRoot() {
        JsonNode rootNode = parseJson("{\"key\": \"value\"}");

        Optional<Object> result = extractor.extractValue(rootNode, "/");

        assertTrue(result.isPresent());
        assertEquals(rootNode, result.get());
    }

    @Test
    @DisplayName("should return root node for root path '$'")
    void testExtractValue_WithRootDollar_ShouldReturnRoot() {
        JsonNode rootNode = parseJson("{\"key\": \"value\"}");

        Optional<Object> result = extractor.extractValue(rootNode, "$");

        assertTrue(result.isPresent());
        assertEquals(rootNode, result.get());
    }

    @Test
    @DisplayName("should return root node for empty path")
    void testExtractValue_WithEmptyPath_ShouldReturnRoot() {
        JsonNode rootNode = parseJson("{\"key\": \"value\"}");

        Optional<Object> result = extractor.extractValue(rootNode, "");

        assertTrue(result.isPresent());
        assertEquals(rootNode, result.get());
    }

    @Test
    @DisplayName("should convert primitive root value")
    void testExtractValue_WithPrimitiveRoot_ShouldConvert() {
        JsonNode rootNode = parseJson("42");

        Optional<Object> result = extractor.extractValue(rootNode, "/");

        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    // ===== PATH NAVIGATION TESTS =====

    @Test
    @DisplayName("should extract nested string value")
    void testExtractValue_WithNestedString_ShouldExtract() {
        JsonNode rootNode = parseJson("{\"user\": {\"name\": \"John\"}}");

        Optional<Object> result = extractor.extractValue(rootNode, "user.name");

        assertTrue(result.isPresent());
        assertEquals("John", result.get());
    }

    @Test
    @DisplayName("should extract nested number value")
    void testExtractValue_WithNestedNumber_ShouldExtract() {
        JsonNode rootNode = parseJson("{\"data\": {\"count\": 123}}");

        Optional<Object> result = extractor.extractValue(rootNode, "data.count");

        assertTrue(result.isPresent());
        assertEquals(123, result.get());
    }

    @Test
    @DisplayName("should extract boolean value")
    void testExtractValue_WithBoolean_ShouldExtract() {
        JsonNode rootNode = parseJson("{\"settings\": {\"enabled\": true}}");

        Optional<Object> result = extractor.extractValue(rootNode, "settings.enabled");

        assertTrue(result.isPresent());
        assertEquals(true, result.get());
    }

    @Test
    @DisplayName("should extract array as List")
    void testExtractValue_WithArray_ShouldExtractAsList() {
        JsonNode rootNode = parseJson("{\"items\": [1, 2, 3]}");

        Optional<Object> result = extractor.extractValue(rootNode, "items");

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof List);
        assertEquals(Arrays.asList(1, 2, 3), result.get());
    }

    @Test
    @DisplayName("should extract array element by index")
    void testExtractValue_WithArrayIndex_ShouldExtract() {
        JsonNode rootNode = parseJson(
                "{\"sensors\": [{\"value\": 10}, {\"value\": 20}, {\"value\": 30}]}"
        );

        Optional<Object> result = extractor.extractValue(rootNode, "sensors[1].value");

        assertTrue(result.isPresent());
        assertEquals(20, result.get());
    }

    @Test
    @DisplayName("should extract nested object as JsonNode")
    void testExtractValue_WithNestedObject_ShouldExtractAsJsonNode() {
        JsonNode rootNode = parseJson("{\"config\": {\"timeout\": 30, \"retries\": 3}}");

        Optional<Object> result = extractor.extractValue(rootNode, "config");

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof JsonNode);
    }

    // ===== NULL AND MISSING TESTS =====

    @Test
    @DisplayName("should return empty for null root node")
    void testExtractValue_WithNullRoot_ShouldReturnEmpty() {
        Optional<Object> result = extractor.extractValue(null, "path");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for null path")
    void testExtractValue_WithNullPath_ShouldReturnEmpty() {
        JsonNode rootNode = parseJson("{\"key\": \"value\"}");

        Optional<Object> result = extractor.extractValue(rootNode, null);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for nonexistent path")
    void testExtractValue_WithNonexistentPath_ShouldReturnEmpty() {
        JsonNode rootNode = parseJson("{\"key\": \"value\"}");

        Optional<Object> result = extractor.extractValue(rootNode, "nonexistent.path");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for null value in path")
    void testExtractValue_WithNullValue_ShouldReturnEmpty() {
        JsonNode rootNode = parseJson("{\"key\": null}");

        Optional<Object> result = extractor.extractValue(rootNode, "key");

        assertFalse(result.isPresent());
    }

    // ===== PATH EXISTS TESTS =====

    @Test
    @DisplayName("should return true for existing path")
    void testPathExists_WithExistingPath_ShouldReturnTrue() {
        JsonNode rootNode = parseJson("{\"user\": {\"name\": \"John\"}}");

        boolean exists = extractor.pathExists(rootNode, "user.name");

        assertTrue(exists);
    }

    @Test
    @DisplayName("should return false for nonexistent path")
    void testPathExists_WithNonexistentPath_ShouldReturnFalse() {
        JsonNode rootNode = parseJson("{\"user\": {\"name\": \"John\"}}");

        boolean exists = extractor.pathExists(rootNode, "user.age");

        assertFalse(exists);
    }

    @Test
    @DisplayName("should return true for null value but existing path")
    void testPathExists_WithNullValue_ShouldReturnTrue() {
        JsonNode rootNode = parseJson("{\"key\": null}");

        boolean exists = extractor.pathExists(rootNode, "key");

        assertTrue(exists);
    }

    @Test
    @DisplayName("should return true for root path existence")
    void testPathExists_WithRootPath_ShouldReturnTrue() {
        JsonNode rootNode = parseJson("{\"key\": \"value\"}");

        boolean exists = extractor.pathExists(rootNode, "/");

        assertTrue(exists);
    }

    @Test
    @DisplayName("should return false for null root node")
    void testPathExists_WithNullRoot_ShouldReturnFalse() {
        boolean exists = extractor.pathExists(null, "path");

        assertFalse(exists);
    }
}