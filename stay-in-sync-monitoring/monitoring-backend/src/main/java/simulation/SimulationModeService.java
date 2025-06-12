package simulation;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SimulationModeService {

    @ConfigProperty(name = "app.simulation-mode", defaultValue = "false")
    boolean simulation;

    public boolean isSimulation() {
        return simulation;
    }

    public String getModeName() {
        return simulation ? "SIMULATION_MODE" : "REAL_MODE";
    }
}