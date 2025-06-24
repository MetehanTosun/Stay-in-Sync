package de.unistuttgart.stayinsync.pollingnode.usercontrol.configuration;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySyncJobException;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PollingJobConfiguratorTest {

        private final PollingJobConfigurator configurator = new PollingJobConfigurator();

        @Test
        void testCreatePollingJobWithValidSyncJob() throws FaultySyncJobException {
            SyncJob syncJob = mock(SyncJob.class);
            when(syncJob.getApiAddress()).thenReturn("https://example.com");

            PollingJob pollingJob = configurator.createPollingJob(syncJob);

            assertNotNull(pollingJob);
            assertEquals("https://example.com", pollingJob.getApiAddress());
        }

        @Test
        void testCreatePollingJobWithNullApiAddress() {
            SyncJob syncJob = mock(SyncJob.class);
            when(syncJob.getApiAddress()).thenReturn(null);

            assertThrows(FaultySyncJobException.class, () -> configurator.createPollingJob(syncJob));
        }

        @Test
        void testCreatePollingJobWithEmptyApiAddress() {
            SyncJob syncJob = mock(SyncJob.class);
            when(syncJob.getApiAddress()).thenReturn("");

            assertThrows(FaultySyncJobException.class, () -> configurator.createPollingJob(syncJob));
        }
}

