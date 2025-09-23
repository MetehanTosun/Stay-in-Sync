package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.OpenApiSpecificationParserService;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OpenApiSynchronizationTest {

    @Inject
    EntityManager em;

    @Inject
    TargetSystemService targetSystemService;

    @Inject
    OpenApiSpecificationParserService openApiSpecificationParserService;

    Long targetSystemId;

    @BeforeEach
    @Transactional
    public void setup() {
        try {
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE TargetSystemEndpoint").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE TargetSystem").executeUpdate();
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
        } catch (Exception ignored) { }

        String minimalOpenApi = """
                openapi: 3.0.1
                info:
                  title: TS API
                  version: 1.0.0
                paths:
                  /items:
                    get:
                      responses:
                        '200':
                          description: ok
                  /products:
                    post:
                      responses:
                        '201':
                          description: created
                """;

        var ts = targetSystemService.createTargetSystem(new TargetSystemDTO(null, "TS-OAS", "http://ts", null, "REST", null, minimalOpenApi, java.util.Set.of()));
        targetSystemId = ts.id();
    }

    @Test
    public void synchronize_creates_endpoints() {
        var ts = targetSystemService.findById(targetSystemId).orElseThrow();
        openApiSpecificationParserService.synchronizeFromSpec(ts);
        @SuppressWarnings("unchecked")
        java.util.List<TargetSystemEndpoint> endpoints = (java.util.List<TargetSystemEndpoint>) (java.util.List<?>) TargetSystemEndpoint.listAll();
        assertFalse(endpoints.isEmpty());
        assertTrue(endpoints.stream().anyMatch(e -> e.endpointPath.equals("/items") && e.httpRequestType.equals("GET")));
        assertTrue(endpoints.stream().anyMatch(e -> e.endpointPath.equals("/products") && e.httpRequestType.equals("POST")));
    }
}


