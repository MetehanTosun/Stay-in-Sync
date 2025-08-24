package de.unistuttgart.stayinsync.core.configuration.util;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AfterSeedRunner {

    @Inject
    EntityManager em;

    @PostConstruct
    @Transactional
    public void runAfterSeed() {
        em.createNativeQuery(""" 
            UPDATE sourcesystemapirequestconfiguration
            SET workerPodName = 'PollingNode'                     
            """).executeUpdate();

//        em.createNativeQuery("""
//            INSERT IGNORE INTO transformation_sourceapirequestconfiguration (transformation_id, source_system_api_request_configuration_id)
//            VALUES (1, 1)
//            """).executeUpdate();
    }
}
