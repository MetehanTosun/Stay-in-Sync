package validation_error;

import de.unistuttgart.graphengine.validation_error.NodeConfigurationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// =============================================================================
// 1. NodeConfigurationError Tests - HAT BUSINESS LOGIC
// =============================================================================
@DisplayName("NodeConfigurationError Tests")
public class NodeConfigurationErrorTest {

    @Test
    @DisplayName("should format message with valid node name")
    void testGetMessage_WithValidNodeName_ShouldIncludeNameInParentheses() {
        // ARRANGE
        NodeConfigurationError error = new NodeConfigurationError(123, "MyTestNode", "Missing required property");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID 123 ('MyTestNode') is invalid: Missing required property", result);
        assertTrue(result.contains("('MyTestNode')"));
    }

    @Test
    @DisplayName("should format message without node name when null")
    void testGetMessage_WithNullNodeName_ShouldExcludeNamePart() {
        // ARRANGE
        NodeConfigurationError error = new NodeConfigurationError(456, null, "Invalid configuration");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID 456 is invalid: Invalid configuration", result);
        assertFalse(result.contains("("));
        assertFalse(result.contains(")"));
    }

    @Test
    @DisplayName("should format message without node name when empty string")
    void testGetMessage_WithEmptyNodeName_ShouldExcludeNamePart() {
        // ARRANGE
        NodeConfigurationError error = new NodeConfigurationError(789, "", "Property validation failed");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID 789 is invalid: Property validation failed", result);
        assertFalse(result.contains("('')"));
    }

    @Test
    @DisplayName("should format message without node name when whitespace only")
    void testGetMessage_WithWhitespaceOnlyNodeName_ShouldExcludeNamePart() {
        // ARRANGE
        NodeConfigurationError error = new NodeConfigurationError(101, "   \t\n   ", "Schema validation error");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID 101 is invalid: Schema validation error", result);
        assertFalse(result.contains("("));
    }

    @Test
    @DisplayName("should return correct error code")
    void testGetErrorCode_ShouldReturnNodeConfigError() {
        // ARRANGE
        NodeConfigurationError error = new NodeConfigurationError(1, "Test", "Message");

        // ACT & ASSERT
        assertEquals("NODE_CONFIG_ERROR", error.getErrorCode());
    }

    @Test
    @DisplayName("should store nodeId correctly")
    void testGetNodeId_ShouldReturnCorrectValue() {
        // ARRANGE
        NodeConfigurationError error = new NodeConfigurationError(42, "Test", "Message");

        // ACT & ASSERT
        assertEquals(42, error.getNodeId());
    }
}
