package de.unistuttgart.stayinsync.monitoring.core.configuration.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.graalvm.polyglot.Engine;

/**
 * CDI Producer for creating a shared, application-scoped GraalVM Engine.
 */
@ApplicationScoped
public class GraalVmEngineProvider {

    private Engine engine;

    @PostConstruct
    void initialize() {
        this.engine = Engine.create();
    }

    @Produces
    public Engine getEngine() {
        return this.engine;
    }

    @PreDestroy
    void cleanup() {
        if (this.engine != null) {
            this.engine.close();
        }
    }
}
