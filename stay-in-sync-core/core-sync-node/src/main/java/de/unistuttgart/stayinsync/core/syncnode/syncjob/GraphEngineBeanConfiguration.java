package de.unistuttgart.stayinsync.core.syncnode.syncjob;

import de.unistuttgart.graphengine.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.graphengine.util.GraphMapper;
import de.unistuttgart.graphengine.util.GraphTopologicalSorter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class GraphEngineBeanConfiguration {

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
