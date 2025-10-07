package de.unistuttgart.stayinsync.core.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
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

    @Inject
    SourceSystemFullUpdateMapper mapper;

    @BeforeEach
    public void cleanup() {
        // Lösche alle SourceSystems vor jedem Test für Isolation
        List<SourceSystem> all = sourceSystemService.findAllSourceSystems();
        all.forEach(ss -> sourceSystemService.deleteSourceSystemById(ss.id));
    }
    //TODO: Fix test
//    @Test
//    public void testCreateAndFindById() {
//        CreateSourceSystemDTO sourceSystem = new CreateSourceSystemDTO(1L, "TestSensor", "http://localhost/test", null, null, null, null, null);
//        sourceSystemService.createSourceSystem(sourceSystem);
//
//        Optional<SourceSystem> found = sourceSystemService.findSourceSystemById(sourceSystem.id());
//        assertTrue(found.isPresent(), "SourceSystem should be found after creation");
//        SourceSystem actual = found.get();
//        assertEquals(sourceSystem.id(), actual.id);
//        assertEquals(sourceSystem.name(), actual.name);
//    }

    @Test
    public void testFindAllSourceSystems() {
        assertTrue(sourceSystemService.findAllSourceSystems().isEmpty(), "DB should start empty");

        CreateSourceSystemDTO sourceSystem1 = new CreateSourceSystemDTO(1L, "Sensor1", null, null, null, null, null, null);
        sourceSystemService.createSourceSystem(sourceSystem1);

        CreateSourceSystemDTO sourceSystem2 = new CreateSourceSystemDTO(1L, "Sensor2", null, null, null, null, null, null);
        sourceSystemService.createSourceSystem(sourceSystem2);

        List<SourceSystem> all = sourceSystemService.findAllSourceSystems();
        assertEquals(2, all.size(), "Should find 2 source systems");
    }
    //TODO: Fix test
//    @Test
//    public void testUpdateSs() {
//        CreateSourceSystemDTO sourceSystem = new CreateSourceSystemDTO(1L, "toEdit", null, null, null, null, null, null);
//
//        sourceSystemService.createSourceSystem(sourceSystem);
//
//        CreateSourceSystemDTO edited = new CreateSourceSystemDTO(1L, "edited", null, null, null, null, null, null);
//        sourceSystemService.updateSourceSystem(edited);
//
//        Optional<SourceSystem> updated = sourceSystemService.findSourceSystemById(edited.id());
//        assertTrue(updated.isPresent(), "Updated SourceSystem should be found");
//        assertEquals("edited", updated.get().name, "Name should be updated");
//    }

    //TODO: Fix test
//    @Test
//    public void testDeleteSs() {
//        CreateSourceSystemDTO sourceSystem = new CreateSourceSystemDTO(1L, "toDelete", null, null, null, null, null, null);
//        sourceSystemService.createSourceSystem(sourceSystem);
//
//        // Verify that the object exists before deleting
//        assertNotNull(sourceSystemService.findSourceSystemById(sourceSystem.id()),
//                "SourceSystem should exist before delete");
//
//        sourceSystemService.deleteSourceSystemById(sourceSystem.id());
//
//        // Verify the deletion of the Object
//        assertEquals(0, sourceSystemService.findAllSourceSystems().size(), "Database should be empty after deletion");
//    }

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
