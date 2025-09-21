package validation_error;

import de.unistuttgart.graphengine.validation_error.OperatorConfigurationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("OperatorConfigurationError Tests")
public class OperatorConfigurationErrorTest {

    @Test
    @DisplayName("should format message correctly")
    void testGetMessage_ShouldFormatWithNodeIdAndMessage() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(123, "TestNode", "requires exactly 2 inputs");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 123 : requires exactly 2 inputs", result);
        assertTrue(result.contains("Node_ID: 123"));
        assertTrue(result.contains("requires exactly 2 inputs"));
    }

    @Test
    @DisplayName("should format message with zero node ID")
    void testGetMessage_WithZeroNodeId_ShouldWork() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(0, "ZeroNode", "invalid operator");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 0 : invalid operator", result);
    }

    @Test
    @DisplayName("should format message with negative node ID")
    void testGetMessage_WithNegativeNodeId_ShouldWork() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(-1, "ErrorNode", "node ID cannot be negative");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: -1 : node ID cannot be negative", result);
    }

    @Test
    @DisplayName("should handle null message")
    void testGetMessage_WithNullMessage_ShouldHandleGracefully() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(123, "TestNode", null);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 123 : null", result);
    }

    @Test
    @DisplayName("should handle empty message")
    void testGetMessage_WithEmptyMessage_ShouldWork() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(123, "TestNode", "");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 123 : ", result);
    }

    @Test
    @DisplayName("should return correct error code")
    void testGetErrorCode_ShouldReturnOperatorConfigError() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(1, "Test", "Message");

        // ACT & ASSERT
        assertEquals("OPERATOR_CONFIG_ERROR", error.getErrorCode());
    }

    @Test
    @DisplayName("should handle very long messages")
    void testGetMessage_WithLongMessage_ShouldFormatCorrectly() {
        // ARRANGE
        String longMessage = "This is a very long error message that describes in detail what went wrong with the operator configuration and provides helpful debugging information";
        OperatorConfigurationError error = new OperatorConfigurationError(999, "LongMessageNode", longMessage);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 999 : " + longMessage, result);
        assertTrue(result.contains(longMessage));
    }

    @Test
    @DisplayName("should handle very large node IDs")
    void testGetMessage_WithLargeNodeId_ShouldFormatCorrectly() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(999999, "LargeIdNode", "configuration issue");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 999999 : configuration issue", result);
        assertTrue(result.contains("999999"));
    }

    @Test
    @DisplayName("should handle special characters in message")
    void testGetMessage_WithSpecialCharacters_ShouldFormatCorrectly() {
        // ARRANGE
        String specialMessage = "Error with symbols: @#$%^&*(){}[]|\\:;\"'<>,.?/~`";
        OperatorConfigurationError error = new OperatorConfigurationError(42, "SpecialNode", specialMessage);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 42 : " + specialMessage, result);
        assertTrue(result.contains(specialMessage));
    }

    @Test
    @DisplayName("should handle unicode characters in message")
    void testGetMessage_WithUnicodeCharacters_ShouldFormatCorrectly() {
        // ARRANGE
        String unicodeMessage = "Error with unicode: αβγδε 中文 🚀 ñáéíóú";
        OperatorConfigurationError error = new OperatorConfigurationError(100, "UnicodeNode", unicodeMessage);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 100 : " + unicodeMessage, result);
        assertTrue(result.contains(unicodeMessage));
    }

    @Test
    @DisplayName("should handle newlines and tabs in message")
    void testGetMessage_WithNewlinesAndTabs_ShouldFormatCorrectly() {
        // ARRANGE
        String messageWithWhitespace = "Line 1\nLine 2\tTabbed text";
        OperatorConfigurationError error = new OperatorConfigurationError(200, "WhitespaceNode", messageWithWhitespace);

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 200 : " + messageWithWhitespace, result);
        assertTrue(result.contains("Line 1\nLine 2\tTabbed text"));
    }

    @Test
    @DisplayName("should create OperatorConfigurationError with no-args constructor")
    void testNoArgsConstructor_ShouldCreateInstance() {
        // ACT
        OperatorConfigurationError error = new OperatorConfigurationError();

        // ASSERT
        assertNotNull(error);
        assertEquals("OPERATOR_CONFIG_ERROR", error.getErrorCode());
    }

    @Test
    @DisplayName("should handle getter and setter for all fields")
    void testGetterSetter_ForAllFields() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError();

        // ACT
        error.setNodeId(777);
        error.setNodeName("SetterTestNode");
        error.setMessage("Setter test message");

        // ASSERT
        assertEquals(777, error.getNodeId());
        assertEquals("SetterTestNode", error.getNodeName());
        assertEquals("Node_ID: 777 : Setter test message", error.getMessage());
    }

    @Test
    @DisplayName("should format message correctly with getters")
    void testGetMessage_UsingGettersAfterSetting_ShouldFormatCorrectly() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError();
        error.setNodeId(888);
        error.setMessage("Getter test message");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 888 : Getter test message", result);
    }

    @Test
    @DisplayName("should be consistent across multiple getMessage calls")
    void testGetMessage_ConsistentAcrossMultipleCalls() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(555, "ConsistentNode", "Same message");

        // ACT
        String result1 = error.getMessage();
        String result2 = error.getMessage();
        String result3 = error.getMessage();

        // ASSERT
        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals("Node_ID: 555 : Same message", result1);
    }

    @Test
    @DisplayName("should handle maximum integer node ID")
    void testGetMessage_WithMaxIntegerNodeId_ShouldWork() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(Integer.MAX_VALUE, "MaxNode", "max test");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: 2147483647 : max test", result);
        assertTrue(result.contains("2147483647"));
    }

    @Test
    @DisplayName("should handle minimum integer node ID")
    void testGetMessage_WithMinIntegerNodeId_ShouldWork() {
        // ARRANGE
        OperatorConfigurationError error = new OperatorConfigurationError(Integer.MIN_VALUE, "MinNode", "min test");

        // ACT
        String result = error.getMessage();

        // ASSERT
        assertEquals("Node_ID: -2147483648 : min test", result);
        assertTrue(result.contains("-2147483648"));
    }
}
