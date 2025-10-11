package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceSystemClientTest {

    @Test
    void testInterfaceAnnotations() {
        // Prüfe @Path auf Interface-Ebene
        Path pathAnnotation = SourceSystemClient.class.getAnnotation(Path.class);
        assertNotNull(pathAnnotation);
        assertEquals("/api/config/source-system", pathAnnotation.value());

        // Prüfe @RegisterRestClient
        assertTrue(SourceSystemClient.class.isAnnotationPresent(
                org.eclipse.microprofile.rest.client.inject.RegisterRestClient.class));
    }

    @Test
    void testGetAllMethodAnnotations() throws Exception {
        Method method = SourceSystemClient.class.getDeclaredMethod("getAll");
        assertTrue(method.isAnnotationPresent(GET.class),
                "@GET sollte auf der getAll()-Methode vorhanden sein");
        assertEquals(List.class, method.getReturnType(),
                "getAll sollte eine Liste zurückgeben");
    }
}
