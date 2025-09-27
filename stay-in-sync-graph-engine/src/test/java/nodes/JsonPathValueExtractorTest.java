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

    // ===== ROOT PATH TESTS =====

    @Test
    @DisplayName("should return root node for root path '/'")
    void testExtractValue_WithRootSlash_ShouldReturnRoot() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": \"value\"}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "/");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(rootNode, result.get());
    }

    @Test
    @DisplayName("should return root node for root path '$'")
    void testExtractValue_WithRootDollar_ShouldReturnRoot() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": \"value\"}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "$");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(rootNode, result.get());
    }

    @Test
    @DisplayName("should return root node for empty path")
    void testExtractValue_WithEmptyPath_ShouldReturnRoot() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": \"value\"}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(rootNode, result.get());
    }

    @Test
    @DisplayName("should convert primitive root value")
    void testExtractValue_WithPrimitiveRoot_ShouldConvert() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("42");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "/");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    // ===== PATH NAVIGATION TESTS =====

    @Test
    @DisplayName("should extract nested string value")
    void testExtractValue_WithNestedString_ShouldExtract() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"user\": {\"name\": \"John\"}}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "user.name");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("John", result.get());
    }

    @Test
    @DisplayName("should extract nested number value")
    void testExtractValue_WithNestedNumber_ShouldExtract() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"data\": {\"count\": 123}}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "data.count");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(123, result.get());
    }

    @Test
    @DisplayName("should extract boolean value")
    void testExtractValue_WithBoolean_ShouldExtract() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"settings\": {\"enabled\": true}}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "settings.enabled");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(true, result.get());
    }

    @Test
    @DisplayName("should extract array as List")
    void testExtractValue_WithArray_ShouldExtractAsList() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"items\": [1, 2, 3]}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "items");

        // ASSERT
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof List);
        assertEquals(Arrays.asList(1, 2, 3), result.get());
    }

    @Test
    @DisplayName("should extract nested object as JsonNode")
    void testExtractValue_WithNestedObject_ShouldExtractAsJsonNode() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"config\": {\"timeout\": 30, \"retries\": 3}}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "config");

        // ASSERT
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof JsonNode);
    }

    // ===== NULL AND MISSING TESTS =====

    @Test
    @DisplayName("should return empty for null root node")
    void testExtractValue_WithNullRoot_ShouldReturnEmpty() {
        // ACT
        Optional<Object> result = extractor.extractValue(null, "path");

        // ASSERT
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for null path")
    void testExtractValue_WithNullPath_ShouldReturnEmpty() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": \"value\"}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, null);

        // ASSERT
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for nonexistent path")
    void testExtractValue_WithNonexistentPath_ShouldReturnEmpty() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": \"value\"}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "nonexistent.path");

        // ASSERT
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for null value in path")
    void testExtractValue_WithNullValue_ShouldReturnEmpty() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": null}");

        // ACT
        Optional<Object> result = extractor.extractValue(rootNode, "key");

        // ASSERT
        assertFalse(result.isPresent());
    }

    // ===== PATH EXISTS TESTS =====

    @Test
    @DisplayName("should return true for existing path")
    void testPathExists_WithExistingPath_ShouldReturnTrue() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"user\": {\"name\": \"John\"}}");

        // ACT
        boolean exists = extractor.pathExists(rootNode, "user.name");

        // ASSERT
        assertTrue(exists);
    }

    @Test
    @DisplayName("should return false for nonexistent path")
    void testPathExists_WithNonexistentPath_ShouldReturnFalse() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"user\": {\"name\": \"John\"}}");

        // ACT
        boolean exists = extractor.pathExists(rootNode, "user.age");

        // ASSERT
        assertFalse(exists);
    }

    @Test
    @DisplayName("should return true for null value but existing path")
    void testPathExists_WithNullValue_ShouldReturnTrue() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": null}");

        // ACT
        boolean exists = extractor.pathExists(rootNode, "key");

        // ASSERT
        assertTrue(exists); // Path exists even if value is null
    }

    @Test
    @DisplayName("should return true for root path existence")
    void testPathExists_WithRootPath_ShouldReturnTrue() throws Exception {
        // ARRANGE
        JsonNode rootNode = objectMapper.readTree("{\"key\": \"value\"}");

        // ACT
        boolean exists = extractor.pathExists(rootNode, "/");

        // ASSERT
        assertTrue(exists);
    }

    @Test
    @DisplayName("should return false for null root node")
    void testPathExists_WithNullRoot_ShouldReturnFalse() {
        // ACT
        boolean exists = extractor.pathExists(null, "path");

        // ASSERT
        assertFalse(exists);
    }
}
