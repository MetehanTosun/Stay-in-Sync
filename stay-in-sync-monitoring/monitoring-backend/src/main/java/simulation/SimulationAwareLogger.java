package simulation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SimulationAwareLogger {

    @Inject
    SimulationModeService simulationModeService;

    private static final Logger LOG = Logger.getLogger(SimulationAwareLogger.class);

    public void logWriteAction(String actionDescription) {
        if (simulationModeService.isSimulation()) {
            LOG.infof("[SIMULATION] Would perform: %s", actionDescription);
        } else {
            LOG.infof("Performing: %s", actionDescription);
        }
    }

    public void logDebug(String msg, Object... params) {
        if (simulationModeService.isSimulation()) {
            LOG.debugf("[SIMULATION] " + msg, params);
        } else {
            LOG.debugf(msg, params);
        }
    }

    public void logInfo(String msg, Object... params) {
        if (simulationModeService.isSimulation()) {
            LOG.infof("[SIMULATION] " + msg, params);
        } else {
            LOG.infof(msg, params);
        }
    }
}