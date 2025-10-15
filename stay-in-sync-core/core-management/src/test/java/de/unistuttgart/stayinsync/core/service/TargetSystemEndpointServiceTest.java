package de.unistuttgart.stayinsync.core.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import de.unistuttgart.stayinsync.core.configuration.mapping.TargetSystemEndpointFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateTargetSystemEndpointDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem.TargetSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.service.TargetSystemService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TargetSystemEndpointServiceTest {

  @Inject
  TargetSystemService targetSystemService;

  @Inject
  TargetSystemEndpointService endpointService;

  @Inject
  TargetSystemEndpointFullUpdateMapper mapper;

  @Inject
  EntityManager em;

  Long targetSystemId;

  @BeforeEach
  @Transactional
  public void setup() {
    // FK-safe cleanup
    // Disable foreign key checks, truncate involved tables, then re-enable
    // Note: MariaDB/MySQL syntax
    try {
      em.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
      em.createNativeQuery("TRUNCATE TABLE Transformation").executeUpdate();
      em.createNativeQuery("TRUNCATE TABLE TargetSystemEndpoint").executeUpdate();
      em.createNativeQuery("TRUNCATE TABLE TargetSystem").executeUpdate();
      em.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
    } catch (Exception ignored) { }
    
    // ensure clean state across tests
    Transformation.deleteAll();
    TargetSystemEndpoint.deleteAll();
    TargetSystem.deleteAll();
    // create a minimal TargetSystem to attach endpoints to
    TargetSystemDTO dto = new TargetSystemDTO(null, "TS-A", "http://ts", null, "REST", null, null, java.util.Set.of());
    var created = targetSystemService.createTargetSystem(dto);
    targetSystemId = created.id();
  }

  @Test
  public void createAndListEndpoints() {
    assertTrue(endpointService.findAllEndpointsWithTargetSystemIdLike(targetSystemId).isEmpty());

    var e1 = new CreateTargetSystemEndpointDTO("/a", "GET", null, null);
    var e2 = new CreateTargetSystemEndpointDTO("/b", "POST", null, null);

    List<TargetSystemEndpoint> saved = endpointService.persistTargetSystemEndpointList(List.of(e1, e2), targetSystemId);
    assertEquals(2, saved.size());

    var all = endpointService.findAllEndpointsWithTargetSystemIdLike(targetSystemId);
    assertEquals(2, all.size());
  }

  @Test
  public void getReplaceDeleteEndpoint() {
    var e1 = new CreateTargetSystemEndpointDTO("/x", "GET", null, null);
    TargetSystemEndpoint saved = endpointService.persistTargetSystemEndpoint(e1, targetSystemId);

    Optional<TargetSystemEndpoint> found = endpointService.findTargetSystemEndpointById(saved.id);
    assertTrue(found.isPresent());

    // replace: change method by updating the managed entity
    saved.httpRequestType = "POST";
    endpointService.replaceTargetSystemEndpoint(saved);
    em.clear();
    var after = endpointService.findTargetSystemEndpointById(saved.id);
    assertTrue(after.isPresent());
    assertEquals("POST", after.get().httpRequestType);

    // delete
    endpointService.deleteTargetSystemEndpointById(saved.id);
    em.clear();
    assertTrue(endpointService.findTargetSystemEndpointById(saved.id).isEmpty());
  }
}


