package validation_error;

import de.unistuttgart.graphengine.validation_error.CycleError;
import de.unistuttgart.graphengine.validation_error.NodeConfigurationError;
import de.unistuttgart.graphengine.validation_error.OperatorConfigurationError;
import de.unistuttgart.graphengine.validation_error.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ValidationError Interface Tests")
public class ValidationErrorInterfaceTest {

    @Test
    @DisplayName("should verify all tested implementations have correct error codes")
    void testAllImplementationsHaveCorrectErrorCodes() {
        // ARRANGE
        NodeConfigurationError nodeError = new NodeConfigurationError(1, "test", "message");
        CycleError cycleError = new CycleError(Arrays.asList(1, 2));
        OperatorConfigurationError operatorError = new OperatorConfigurationError(1, "test", "message");

        // ASSERT
        assertEquals("NODE_CONFIG_ERROR", nodeError.getErrorCode());
        assertEquals("CYCLE_DETECTED", cycleError.getErrorCode());
        assertEquals("OPERATOR_CONFIG_ERROR", operatorError.getErrorCode());
    }

    @Test
    @DisplayName("should verify all implementations implement ValidationError")
    void testAllImplementationsAreValidationErrors() {
        // ARRANGE
        NodeConfigurationError nodeError = new NodeConfigurationError(1, "test", "message");
        CycleError cycleError = new CycleError(Arrays.asList(1, 2));
        OperatorConfigurationError operatorError = new OperatorConfigurationError(1, "test", "message");

        // ASSERT
        assertTrue(nodeError instanceof ValidationError);
        assertTrue(cycleError instanceof ValidationError);
        assertTrue(operatorError instanceof ValidationError);

        assertNotNull(nodeError.getErrorCode());
        assertNotNull(cycleError.getErrorCode());
        assertNotNull(operatorError.getErrorCode());

        assertNotNull(nodeError.getMessage());
        assertNotNull(cycleError.getMessage());
        assertNotNull(operatorError.getMessage());
    }
}
