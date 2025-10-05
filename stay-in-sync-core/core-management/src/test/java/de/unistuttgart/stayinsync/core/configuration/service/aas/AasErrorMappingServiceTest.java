package de.unistuttgart.stayinsync.core.configuration.service.aas;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AasErrorMappingServiceTest {

    @Inject
    AasErrorMappingService errorMappingService;

    @Test
    void testMapHttpError_200() {
        // Given
        int statusCode = 200;
        String statusMessage = "OK";
        String body = "{\"success\":true}";

        // When
        Response response = errorMappingService.mapHttpError(statusCode, statusMessage, body);

        // Then
        assertEquals(200, response.getStatus());
        assertEquals("{\"success\":true}", response.getEntity());
    }

    @Test
    void testMapHttpError_400() {
        // Given
        int statusCode = 400;
        String statusMessage = "Bad Request";
        String body = "{\"error\":\"Invalid request\"}";

        // When
        Response response = errorMappingService.mapHttpError(statusCode, statusMessage, body);

        // Then
        assertEquals(400, response.getStatus());
        assertEquals("{\"error\":\"Invalid request\"}", response.getEntity());
    }

    @Test
    void testMapHttpError_404() {
        // Given
        int statusCode = 404;
        String statusMessage = "Not Found";
        String body = "{\"error\":\"Resource not found\"}";

        // When
        Response response = errorMappingService.mapHttpError(statusCode, statusMessage, body);

        // Then
        assertEquals(404, response.getStatus());
        assertEquals("{\"error\":\"Resource not found\"}", response.getEntity());
    }

    @Test
    void testMapHttpError_500() {
        // Given
        int statusCode = 500;
        String statusMessage = "Internal Server Error";
        String body = "{\"error\":\"Server error\"}";

        // When
        Response response = errorMappingService.mapHttpError(statusCode, statusMessage, body);

        // Then
        assertEquals(500, response.getStatus());
        assertEquals("{\"error\":\"Server error\"}", response.getEntity());
    }

    @Test
    void testMapHttpError_WithNullBody() {
        // Given
        int statusCode = 500;
        String statusMessage = "Internal Server Error";
        String body = null;

        // When
        Response response = errorMappingService.mapHttpError(statusCode, statusMessage, body);

        // Then
        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getEntity());
    }

    @Test
    void testMapHttpError_WithEmptyBody() {
        // Given
        int statusCode = 400;
        String statusMessage = "Bad Request";
        String body = "";

        // When
        Response response = errorMappingService.mapHttpError(statusCode, statusMessage, body);

        // Then
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getEntity());
    }
}
