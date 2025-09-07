package de.unistuttgart.stayinsync.service.transformationrule;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.LogicGraphEntity;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.service.transformationrule.GraphStorageService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@QuarkusTest
public class GraphStorageServiceTest {

    @Inject
    GraphStorageService storageService;

    /**
     * Clean up the database before each test to ensure test isolation.
     */
    @BeforeEach
    @Transactional
    void cleanUpDatabase() {
        TransformationRule.deleteAll();
    }

    @Test
    @Transactional
    void testPersistAndFindById() {
        // ARRANGE
        TransformationRule newRule = createTestRule("Test Rule 1");

        // ACT
        storageService.persistRule(newRule);

        // ASSERT
        assertNotNull(newRule.id, "ID should be generated after persisting.");
        Optional<TransformationRule> foundRuleOpt = storageService.findRuleById(newRule.id);

        assertTrue(foundRuleOpt.isPresent(), "Rule should be found by its new ID.");
        assertEquals("Test Rule 1", foundRuleOpt.get().name);
    }

    @Test
    @Transactional
    void testFindRuleByName() {
        // ARRANGE
        storageService.persistRule(createTestRule("UniqueName"));

        // ACT
        Optional<TransformationRule> foundRuleOpt = storageService.findRuleByName("UniqueName");
        Optional<TransformationRule> notFoundRuleOpt = storageService.findRuleByName("NonExistentName");

        // ASSERT
        assertTrue(foundRuleOpt.isPresent(), "Rule should be found by its unique name.");
        assertTrue(notFoundRuleOpt.isEmpty(), "Rule should not be found with a non-existent name.");
    }

    @Test
    @Transactional
    void testDeleteRuleById() {
        // ARRANGE
        TransformationRule ruleToDelete = createTestRule("ToDelete");
        storageService.persistRule(ruleToDelete);
        Long id = ruleToDelete.id;

        assertTrue(storageService.findRuleById(id).isPresent(), "Rule should exist before deletion.");

        // ACT
        boolean wasDeleted = storageService.deleteRuleById(id);

        // ASSERT
        assertTrue(wasDeleted);
        assertTrue(storageService.findRuleById(id).isEmpty(), "Rule should be gone after deletion.");
    }

    @Test
    @Transactional
    void testFindAllRules() {
        // ARRANGE
        assertEquals(0, storageService.findAllRules().size(), "Database should be empty initially.");
        storageService.persistRule(createTestRule("Rule A"));
        storageService.persistRule(createTestRule("Rule B"));

        // ACT
        List<TransformationRule> allRules = storageService.findAllRules();

        // ASSERT
        assertEquals(2, allRules.size(), "Should find exactly two rules.");
    }

    /**
     * Helper method to create a valid TransformationRule entity for testing.
     */
    private TransformationRule createTestRule(String name) {
        TransformationRule rule = new TransformationRule();
        rule.name = name;
        rule.description = "A test description.";

        LogicGraphEntity graph = new LogicGraphEntity();
        graph.graphDefinitionJson = "{\"nodes\":[]}"; // Minimal valid JSON

        // Persist the graph entity first to avoid cascade issues
        graph.persist();
        rule.graph = graph;

        return rule;
    }
}