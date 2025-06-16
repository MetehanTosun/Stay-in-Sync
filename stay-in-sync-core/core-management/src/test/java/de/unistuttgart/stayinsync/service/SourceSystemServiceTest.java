package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SourceSystemServiceTest {

    @Inject
    SourceSystemService sourceSystemService;

    @BeforeEach
    public void cleanup() {
        // Lösche alle SourceSystems vor jedem Test für Isolation
        List<SourceSystem> all = sourceSystemService.findAllSourceSystems();
        all.forEach(ss -> sourceSystemService.deleteSourceSystemById(ss.id));
    }

    @Test
    public void testCreateAndFindById() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "TestSensor";
        sourceSystem.apiUrl = "http://localhost/test";
        sourceSystemService.createSourceSystem(sourceSystem);

        Optional<SourceSystem> found = sourceSystemService.findSourceSystemById(sourceSystem.id);
        assertTrue(found.isPresent(), "SourceSystem should be found after creation");
        SourceSystem actual = found.get();
        assertEquals(sourceSystem.id, actual.id);
        assertEquals(sourceSystem.name, actual.name);
    }

    @Test
    public void testFindAllSourceSystems() {
        assertTrue(sourceSystemService.findAllSourceSystems().isEmpty(), "DB should start empty");

        SourceSystem ss1 = new SourceSystem();
        ss1.name = "Sensor1";
        sourceSystemService.createSourceSystem(ss1);

        SourceSystem ss2 = new SourceSystem();
        ss2.name = "Sensor2";
        sourceSystemService.createSourceSystem(ss2);

        List<SourceSystem> all = sourceSystemService.findAllSourceSystems();
        assertEquals(2, all.size(), "Should find 2 source systems");
    }

    @Test
    public void testUpdateSs() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "toEdit";
        sourceSystemService.createSourceSystem(sourceSystem);

        sourceSystem.name = "edited";
        sourceSystemService.updateSourceSystem(sourceSystem);

        Optional<SourceSystem> updated = sourceSystemService.findSourceSystemById(sourceSystem.id);
        assertNotNull(updated.isPresent(), "Updated SourceSystem should be found");
        assertEquals("edited", updated.get().name, "Name should be updated");
    }

    @Test
    public void testDeleteSs() {
        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.name = "toDelete";
        sourceSystemService.createSourceSystem(sourceSystem);

        // Verify that the object exists before deleting
        assertNotNull(sourceSystemService.findSourceSystemById(sourceSystem.id),
                "SourceSystem should exist before delete");

        sourceSystemService.deleteSourceSystemById(sourceSystem.id);

        // Verify the deletion of the Object
        assertEquals(0, sourceSystemService.findAllSourceSystems().size(), "Database should be empty after deletion");
    }

    @Test
    public void testDeleteNonExistentIdReturnsFalse() {
        Long nonExistentId = 9999L;
        boolean result = sourceSystemService.deleteSourceSystemById(nonExistentId);
        assertFalse(result, "Deleting non-existent ID should return false");
    }

    @Test
    public void testFindByIdNotFoundReturnsEmpty() {
        Long nonExistentId = 9999L;
        Optional<SourceSystem> result = sourceSystemService.findSourceSystemById(nonExistentId);
        assertTrue(result.isEmpty(), "Should return empty Optional for non-existent ID");
    }
}
