package de.unistuttgart.stayinsync.persistence.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

@QuarkusTest
@TestTransaction
public class SourceSystemEndpointTest {

    @Test
    public void listAllWherePollingIsActiveAndUnused() {

        SyncJob.deleteAll();
        SourceSystemEndpoint.deleteAll();
        var inactiveSyncJob = inactiveSyncJob();
        var activeSyncJob = activeSyncJob();

        SyncJob.persist(inactiveSyncJob);
        SyncJob.persist(activeSyncJob);


        Assertions.assertThat(SourceSystemEndpoint.count())
                .isEqualTo(4L);

        Assertions.assertThat(Transformation.count())
                .isEqualTo(2L);


        Assertions.assertThat(SourceSystemEndpoint.listAllWherePollingIsActiveAndUnused())
                .isNotNull()
                .hasSize(1).first()
                .extracting("endpointPath")
                .isEqualTo("/found");
    }

    public SyncJob activeSyncJob() {


        SyncJob syncJob = new SyncJob();
        syncJob.name = "test";
        syncJob.deployed = true;

        SourceSystemEndpoint sourceSystemEndpoint = new SourceSystemEndpoint();
        sourceSystemEndpoint.pollingActive = true;
        sourceSystemEndpoint.endpointPath = "/active";

        SourceSystemEndpoint sourceSystemEndpointNoPolling = new SourceSystemEndpoint();
        sourceSystemEndpointNoPolling.pollingActive = false;
        sourceSystemEndpointNoPolling.endpointPath = "/inActive";

        Transformation transformation = new Transformation();
        transformation.syncJob = syncJob;
        transformation.sourceSystemEndpoints = new HashSet<>();
        transformation.sourceSystemEndpoints.add(sourceSystemEndpoint);
        transformation.sourceSystemEndpoints.add(sourceSystemEndpointNoPolling);

        syncJob.transformations = new HashSet<>();
        syncJob.transformations.add(transformation);

        return syncJob;
    }

    public SyncJob inactiveSyncJob() {


        SyncJob syncJob = new SyncJob();
        syncJob.name = "test";
        syncJob.deployed = false;

        SourceSystemEndpoint sourceSystemEndpoint = new SourceSystemEndpoint();
        sourceSystemEndpoint.pollingActive = true;
        sourceSystemEndpoint.endpointPath = "/found";

        SourceSystemEndpoint sourceSystemEndpointNoPolling = new SourceSystemEndpoint();
        sourceSystemEndpointNoPolling.pollingActive = false;
        sourceSystemEndpointNoPolling.endpointPath = "/inActive";

        Transformation transformation = new Transformation();
        transformation.syncJob = syncJob;
        transformation.sourceSystemEndpoints = new HashSet<>();
        transformation.sourceSystemEndpoints.add(sourceSystemEndpoint);
        transformation.sourceSystemEndpoints.add(sourceSystemEndpointNoPolling);

        syncJob.transformations = new HashSet<>();
        syncJob.transformations.add(transformation);

        return syncJob;
    }

}
