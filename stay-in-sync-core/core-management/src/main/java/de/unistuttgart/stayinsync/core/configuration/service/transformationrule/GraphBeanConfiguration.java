package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import de.unistuttgart.graphengine.util.GraphMapper;
import de.unistuttgart.graphengine.util.GraphTopologicalSorter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class GraphBeanConfiguration {

    @Produces
    @ApplicationScoped
    public GraphMapper graphMapper() {
        return new GraphMapper();
    }

    @Produces
    @ApplicationScoped
    public GraphTopologicalSorter graphTopologicalSorter() {
        return new GraphTopologicalSorter();
    }
}