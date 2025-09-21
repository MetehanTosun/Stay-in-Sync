package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class TransformationServiceTest {

    @Inject
    TransformationService transformationService;

    @Test
    public void testServiceInjection() {
        // Test that the service is properly injected
        assertThat(transformationService).isNotNull();
    }

    @Test
    public void testCreateTransformationShellDTO() {
        // Test that we can create a DTO
        TransformationShellDTO shellDTO = new TransformationShellDTO("Test", "Description");
        assertThat(shellDTO.name()).isEqualTo("Test");
        assertThat(shellDTO.description()).isEqualTo("Description");
    }

    @Test
    public void testServiceMethodsExist() {
        // Test that the service has the expected methods
        assertThat(transformationService).isInstanceOf(TransformationService.class);
    }
}