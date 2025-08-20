package de.unistuttgart.stayinsync.core.configuration.service.aas;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AasStructureSnapshotService {

    public void buildInitialSnapshot(Long sourceSystemId) {
        Log.infof("Building initial AAS snapshot for sourceSystemId=%d", sourceSystemId);
    }

    public void refreshSnapshot(Long sourceSystemId) {
        Log.infof("Refreshing AAS snapshot for sourceSystemId=%d", sourceSystemId);
    }
}


