package de.unistuttgart.stayinsync.core.persistence.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestTransaction
public class TransformationTest {

    @BeforeEach
    void setUp() {
        Transformation.deleteAll();
        TransformationScript.deleteAll();
        TransformationRule.deleteAll();
        SyncJob.deleteAll();
        TargetSystemEndpoint.deleteAll();
        SourceSystemEndpoint.deleteAll();
    }

    /**
     * Tests the first step of the workflow: creating a basic "shell" for a Transformation.
     * At this stage, it only has its own properties, and all relationships should be null.
     */
    @Test
    void shouldPersistTransformationShell() {
        // Arrange
        Transformation transformationShell = new Transformation();
        transformationShell.name = "Initial Draft Transformation";
        transformationShell.description = "A description for the draft.";

        // Act
        transformationShell.persist();
        Transformation.flush();

        // Assert
        assertThat(Transformation.count()).isEqualTo(1L);

        Transformation found = Transformation.findById(transformationShell.id);
        assertThat(found).isNotNull();
        assertThat(found.name).isEqualTo("Initial Draft Transformation");
        assertThat(found.description).isEqualTo("A description for the draft.");

        // Verify that all relationships are empty/null as expected for a shell
        assertThat(found.syncJob).isNull();
        assertThat(found.transformationScript).isNull();
        assertThat(found.transformationRule).isNull();
        assertThat(found.sourceSystemApiRequestConfigurations).isEmpty();
        assertThat(found.targetSystemApiRequestConfigurations).isEmpty();
//        assertThat(found.sourceSystemEndpoints).isNotNull().isEmpty();
    }

    /**
     * Tests the second step: updating an existing shell by "assembling" it with
     * all its component parts. This mimics the logic of your 'assemble' service method.
     */
    @Test
    void shouldUpdateAndAssembleTransformationWithAllAssociations() {
        // --- ARRANGE: Step 1 - Create the shell and all component parts ---
        Transformation transformationToUpdate = new Transformation();
        transformationToUpdate.name = "Transformation to be Assembled";
        transformationToUpdate.persistAndFlush();

        SyncJob syncJob = new SyncJob();
        syncJob.name = "Test SyncJob";
        syncJob.persist();

        SourceSystemEndpoint sourceEndpoint1 = new SourceSystemEndpoint();
        sourceEndpoint1.endpointPath = "Test Source 1";
        sourceEndpoint1.persist();

        SourceSystemEndpoint sourceEndpoint2 = new SourceSystemEndpoint();
        sourceEndpoint2.endpointPath = "Test Source 2";
        sourceEndpoint2.persist();

        TransformationScript script = new TransformationScript();
        script.name = "Test Script";
        script.persist();

        TransformationRule rule = new TransformationRule();
        rule.name = "Test Rule";
        rule.persist();


        // --- ACT: Update the shell with the links ---

        Transformation foundShell = Transformation.findById(transformationToUpdate.id);

        foundShell.syncJob = syncJob;
//        foundShell.sourceSystemEndpoints = Set.of(sourceEndpoint1, sourceEndpoint2);
        foundShell.transformationScript = script;
        foundShell.transformationRule = rule;
        rule.transformation = foundShell;

        script.transformation = foundShell;

        Transformation.flush();

        // --- ASSERT: Verify the assembly was successful ---
        assertThat(Transformation.count()).isEqualTo(1L);

        Transformation fullyAssembled = Transformation.findById(transformationToUpdate.id);
        assertThat(fullyAssembled).isNotNull();

        assertThat(fullyAssembled.syncJob.id).isEqualTo(syncJob.id);
        assertThat(fullyAssembled.transformationScript.id).isEqualTo(script.id);
        assertThat(fullyAssembled.transformationRule.id).isEqualTo(rule.id);

//        assertThat(fullyAssembled.sourceSystemEndpoints)
//                .hasSize(2)
//                .extracting(endpoint -> endpoint.id)
//                .containsExactlyInAnyOrder(sourceEndpoint1.id, sourceEndpoint2.id);
    }
}