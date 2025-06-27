package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestTransaction
public class TransformationServiceTest {

    @Inject
    TransformationService service;

    @InjectMock
    TransformationMapper mapper;

    @BeforeEach
    void cleanDatabase() {
        Transformation.deleteAll();
        TransformationScript.deleteAll();
        TransformationRule.deleteAll();
        SourceSystemEndpoint.deleteAll();
    }

    @Test
    void createShell_shouldPersistNewTransformation() {
        // Arrange
        var shellDto = new TransformationShellDTO("New Shell", "A test description");

        doAnswer(invocation -> {
            Transformation t = invocation.getArgument(1);
            t.name = shellDto.name();
            t.description = shellDto.description();
            return null;
        }).when(mapper).updateFromShellDTO(any(), any());

        // Act
        Transformation result = service.createShell(shellDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id).isNotNull();

        verify(mapper).updateFromShellDTO(eq(shellDto), any(Transformation.class));

        assertThat(Transformation.count()).isEqualTo(1);
        Transformation foundInDb = Transformation.findById(result.id);
        assertThat(foundInDb.name).isEqualTo("New Shell");
    }

    @Test
    void assemble_shouldLinkAllComponentsSuccessfully() {
        // Arrange
        Transformation shell = new Transformation();
        shell.persist();

        TransformationScript script = new TransformationScript();
        script.persist();

        TransformationRule rule = new TransformationRule();
        rule.persist();

        SourceSystemEndpoint endpoint1 = new SourceSystemEndpoint();
        endpoint1.persist();
        SourceSystemEndpoint endpoint2 = new SourceSystemEndpoint();
        endpoint2.persist();

        var assemblyDto = new TransformationAssemblyDTO(shell.id, null,
                Set.of(endpoint1.id, endpoint2.id),
                null,
                rule.id, script.id);

        // Act
        Transformation assembled = service.assemble(shell.id, assemblyDto);

        // Assert
        assertThat(assembled.id).isEqualTo(shell.id);
        assertThat(assembled.transformationScript.id).isEqualTo(script.id);
        assertThat(assembled.transformationRule.id).isEqualTo(rule.id);
        assertThat(assembled.sourceSystemEndpoints).hasSize(2)
                .extracting(e -> e.id).containsExactlyInAnyOrder(endpoint1.id, endpoint2.id);

        assertThat(assembled.transformationScript.transformation).isNotNull();
    }

    @Test
    void assemble_shouldFailWhenTransformationNotFound() {
        // Arrange
        long nonExistentId = 999L;
        var assemblyDto = new TransformationAssemblyDTO(nonExistentId, null, Set.of(), null, 1L, 1L);

        // Act & Assert
        assertThatThrownBy(() -> service.assemble(nonExistentId, assemblyDto))
                .isInstanceOf(CoreManagementWebException.class)
                .satisfies(ex -> {
                    var webEx = (CoreManagementWebException) ex;
                    Response response = webEx.getResponse();

                    assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
                    assertThat(response.getEntity())
                            .extracting("errorMessage")
                            .isEqualTo("Transformation with id 999 not found.");
                });
    }

    @Test
    void assemble_shouldFailWhenScriptNotFound() {
        // Arrange
        Transformation shell = new Transformation();
        shell.persist();

        long nonExistentScriptId = 999L;

        TransformationRule rule = new TransformationRule();
        rule.persist();

        var assemblyDto = new TransformationAssemblyDTO(shell.id, null, Set.of(), null, rule.id, nonExistentScriptId);

        // Act & Assert
        assertThatThrownBy(() -> service.assemble(shell.id, assemblyDto))
                .isInstanceOf(CoreManagementWebException.class)
                .satisfies(ex -> {
                    var webEx = (CoreManagementWebException) ex;
                    Response response = webEx.getResponse();

                    assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
                    assertThat(response.getEntity())
                            .extracting("errorMessage")
                            .isEqualTo("TransformationScript with id 999 not found.");
                });
    }

    @Test
    void findById_shouldReturnTransformationWhenExists() {
        // Arrange
        Transformation t = new Transformation();
        t.name = "Find Me";
        t.persist();

        // Act
        Optional<Transformation> result = service.findById(t.id);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id).isEqualTo(t.id);
        assertThat(result.get().name).isEqualTo("Find Me");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // Act
        Optional<Transformation> result = service.findById(999L);
        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllTransformations() {
        // Arrange
        new Transformation().persist();
        new Transformation().persist();

        // Act
        List<Transformation> results = service.findAll();

        // Assert
        assertThat(results).hasSize(2);
    }

    @Test
    void delete_shouldRemoveTransformation() {
        // Arrange
        Transformation t = new Transformation();
        t.persist();
        assertThat(Transformation.count()).isEqualTo(1);

        // Act
        service.delete(t.id);

        // Assert
        assertThat(Transformation.count()).isZero();
    }

    @Test
    void delete_shouldThrowExceptionWhenNotFound() {
        // Arrange
        long nonExistentId = 999L;

        // Act & Assert
        assertThatThrownBy(() -> service.delete(nonExistentId))
                .isInstanceOf(CoreManagementWebException.class)
                .satisfies(ex -> {
                    var webEx = (CoreManagementWebException) ex;
                    Response response = webEx.getResponse();

                    assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);

                    assertThat(response.getEntity())
                            .extracting("errorMessage")
                            .isEqualTo("Transformation with id 999 could not be found for deletion.");
                });
    }
}