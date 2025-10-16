package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link TargetSystemClient}.
 * Verifies that all required annotations and method signatures are correctly defined.
 * This test does not start Quarkus or perform real HTTP calls.
 */
class TargetSystemClientTest {

    @Test
    void testInterfaceAnnotations() {
        // Verify that the interface has a @Path annotation
        Path pathAnnotation = TargetSystemClient.class.getAnnotation(Path.class);
        assertNotNull(pathAnnotation, "@Path annotation should be present on TargetSystemClient");
        assertEquals("/api/config/target-systems", pathAnnotation.value(),
                "The @Path value should match the expected endpoint");

        // Verify that the interface is annotated with @RegisterRestClient
        assertTrue(TargetSystemClient.class.isAnnotationPresent(RegisterRestClient.class),
                "@RegisterRestClient should be present on TargetSystemClient");

        RegisterRestClient restClientAnnotation =
                TargetSystemClient.class.getAnnotation(RegisterRestClient.class);
        assertEquals("backend-api", restClientAnnotation.configKey(),
                "The configKey of @RegisterRestClient should be 'backend-api'");
    }

    @Test
    void testGetAllMethodAnnotations() throws Exception {
        Method method = TargetSystemClient.class.getDeclaredMethod("getAll");

        // Verify that the method has a @GET annotation
        assertTrue(method.isAnnotationPresent(GET.class),
                "The getAll() method should be annotated with @GET");

        // Verify that the return type is List
        assertEquals(List.class, method.getReturnType(),
                "The getAll() method should return a List");

        // Verify that the method takes no parameters
        assertEquals(0, method.getParameterCount(),
                "The getAll() method should not have any parameters");
    }
}
