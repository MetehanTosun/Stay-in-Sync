package de.unistuttgart.stayinsync.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationAssemblyDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationDetailsDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationShellDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TransformationResourceTest {

    // --- Constants for test data ---
    private static final long DEFAULT_TRANSFORMATION_ID = 1L;
    private static final String DEFAULT_NAME = "Test Transformation";
    private static final String DEFAULT_DESCRIPTION = "A description of the test.";
    private static final long DEFAULT_SYNC_JOB_ID = 10L;
    private static final long DEFAULT_TARGET_ENDPOINT_ID = 20L;
    private static final long DEFAULT_SCRIPT_ID = 50L;
    private static final long DEFAULT_RULE_ID = 70L;
    private static final Set<Long> DEFAULT_SOURCE_ENDPOINT_IDS = Set.of(30L, 31L);

    @InjectMock
    TransformationService transformationService;

    @BeforeAll
    static void beforeAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Tests the POST endpoint for creating a new transformation "shell".
     */
    @Test
    void testCreateTransformationShell() {
        // Arrange
        var shellDto = new TransformationShellDTO(DEFAULT_NAME, DEFAULT_DESCRIPTION);

        var persistedShell = new Transformation();
        persistedShell.id = DEFAULT_TRANSFORMATION_ID;
        persistedShell.name = DEFAULT_NAME;
        persistedShell.description = DEFAULT_DESCRIPTION;

        when(transformationService.createTransformation(any(TransformationShellDTO.class))).thenReturn(persistedShell);

        // Act & Assert
        var createdDto = given()
                .when()
                .body(shellDto)
                .contentType(JSON)
                .accept(JSON)
                .post("/api/config/transformation")
                .then()
                .statusCode(CREATED.getStatusCode())
                .header("Location", Matchers.endsWith("/api/config/transformation/" + DEFAULT_TRANSFORMATION_ID))
                .extract().as(TransformationDetailsDTO.class);

        assertThat(createdDto.id()).isEqualTo(DEFAULT_TRANSFORMATION_ID);
        assertThat(createdDto.name()).isEqualTo(DEFAULT_NAME);
        assertThat(createdDto.description()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(createdDto.syncJobId()).isNull();
        assertThat(createdDto.script()).isNull();

        var dtoCaptor = ArgumentCaptor.forClass(TransformationShellDTO.class);
        verify(transformationService).createTransformation(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue()).isEqualTo(shellDto);
    }

    /**
     * Tests the PUT endpoint for assembling a transformation with its components.
     */
    @Test
    void testAssembleTransformation() {
        // Arrange
        var assemblyDto = new TransformationAssemblyDTO(DEFAULT_TRANSFORMATION_ID, DEFAULT_SYNC_JOB_ID,
                DEFAULT_SOURCE_ENDPOINT_IDS, DEFAULT_TARGET_ENDPOINT_ID, DEFAULT_RULE_ID, DEFAULT_SCRIPT_ID);

        var fullyAssembledEntity = createFullyAssembledMockEntity();
        when(transformationService.updateTransformation(eq(DEFAULT_TRANSFORMATION_ID), any(TransformationAssemblyDTO.class)))
                .thenReturn(fullyAssembledEntity);

        // Act & Assert
        var resultingDto = given()
                .when()
                .body(assemblyDto)
                .contentType(JSON)
                .accept(JSON)
                .put("/api/config/transformation/{id}", DEFAULT_TRANSFORMATION_ID)
                .then()
                .statusCode(OK.getStatusCode())
                .extract().as(TransformationDetailsDTO.class);

        assertThat(resultingDto.id()).isEqualTo(DEFAULT_TRANSFORMATION_ID);
        assertThat(resultingDto.syncJobId()).isEqualTo(DEFAULT_SYNC_JOB_ID);
        assertThat(resultingDto.transformationRuleId()).isEqualTo(DEFAULT_RULE_ID);
        assertThat(resultingDto.script().id()).isEqualTo(DEFAULT_SCRIPT_ID);
//        assertThat(resultingDto.sourceSystemEndpointIds()).containsExactlyInAnyOrderElementsOf(DEFAULT_SOURCE_ENDPOINT_IDS);

        var dtoCaptor = ArgumentCaptor.forClass(TransformationAssemblyDTO.class);
        verify(transformationService).updateTransformation(eq(DEFAULT_TRANSFORMATION_ID), dtoCaptor.capture());
        assertThat(dtoCaptor.getValue()).isEqualTo(assemblyDto);
    }

    @Test
    void testGetTransformationById() {
        // Arrange
        var fullEntity = createFullyAssembledMockEntity();
        when(transformationService.findById(DEFAULT_TRANSFORMATION_ID)).thenReturn(Optional.of(fullEntity));

        // Act & Assert
        var actualDto = given()
                .when()
                .accept(JSON)
                .get("/api/config/transformation/{id}", DEFAULT_TRANSFORMATION_ID)
                .then()
                .statusCode(OK.getStatusCode())
                .extract().as(TransformationDetailsDTO.class);

        assertThat(actualDto.id()).isEqualTo(DEFAULT_TRANSFORMATION_ID);
        assertThat(actualDto.name()).isEqualTo(DEFAULT_NAME);
        assertThat(actualDto.script().id()).isEqualTo(DEFAULT_SCRIPT_ID);
    }

    @Test
    void testGetAllTransformations() {
        // Arrange
        var entity1 = createFullyAssembledMockEntity();
        var entity2 = createFullyAssembledMockEntity();
        entity2.id = 2L;

        when(transformationService.findAll()).thenReturn(List.of(entity1, entity2));

        // Act & Assert
        var dtoList = given()
                .when()
                .accept(JSON)
                .get("/api/config/transformation")
                .then()
                .statusCode(OK.getStatusCode())
                .extract().body().jsonPath().getList(".", TransformationDetailsDTO.class);

        assertThat(dtoList).hasSize(2);
        assertThat(dtoList.get(0).id()).isEqualTo(1L);
        assertThat(dtoList.get(1).id()).isEqualTo(2L);
    }

    @Test
    void testDeleteTransformation_Success() {
        // Arrange
        long transformationIdToDelete = 1L;
        when(transformationService.delete(eq(transformationIdToDelete))).thenReturn(true);

        // Act & Assert
        given()
                .when()
                .delete("/api/config/transformation/{id}", transformationIdToDelete)
                .then()
                .statusCode(NO_CONTENT.getStatusCode())
                .body(blankOrNullString());

        verify(transformationService).delete(transformationIdToDelete);
    }

    @Test
    void testDeleteTransformation_NotFound() {
        // Arrange
        long nonExistentId = 999L;
        when(transformationService.delete(eq(nonExistentId))).thenReturn(false);

        // Act & Assert
        given()
                .when()
                .delete("/api/config/transformation/{id}", nonExistentId)
                .then()
                .statusCode(NOT_FOUND.getStatusCode());

        verify(transformationService).delete(nonExistentId);
    }

    // --- Helper Method to create a fully populated mock entity for service responses ---

    private static Transformation createFullyAssembledMockEntity() {
        var transformation = new Transformation();
        transformation.id = DEFAULT_TRANSFORMATION_ID;
        transformation.name = DEFAULT_NAME;
        transformation.description = DEFAULT_DESCRIPTION;

        var sj = new SyncJob();
        sj.id = DEFAULT_SYNC_JOB_ID;
        transformation.syncJob = sj;

        var script = new TransformationScript();
        script.id = DEFAULT_SCRIPT_ID;
        script.name = "Test Script";
        transformation.transformationScript = script;

        var rule = new TransformationRule();
        rule.id = DEFAULT_RULE_ID;
        transformation.transformationRule = rule;

//        transformation.sourceSystemEndpoints = DEFAULT_SOURCE_ENDPOINT_IDS.stream()
//                .map(id -> {
//                    var ssep = new SourceSystemEndpoint();
//                    ssep.id = id;
//                    return ssep;
//                }).collect(Collectors.toSet());

        return transformation;
    }
}