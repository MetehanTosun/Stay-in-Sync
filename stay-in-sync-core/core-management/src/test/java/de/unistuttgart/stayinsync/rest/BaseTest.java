package de.unistuttgart.stayinsync.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    public void cleanDatabase() {
        // Clean up all test data before each test
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        
        // Delete in reverse order of dependencies - use correct table names
        try {
            entityManager.createNativeQuery("DELETE FROM transformation_sourceApiRequestConfiguration").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM transformation_sourceVariable").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM Transformation").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM TargetSystemEndpoint").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM SourceSystemEndpoint").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM SourceSystem").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM TargetSystem").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM SourceSystem").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM TargetSystem").executeUpdate();
        } catch (Exception e) {
            // Table might not exist, ignore
        }
        
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        entityManager.flush();
    }
}
