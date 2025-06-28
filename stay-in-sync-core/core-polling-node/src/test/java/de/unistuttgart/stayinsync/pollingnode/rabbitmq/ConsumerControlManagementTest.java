package java.de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import java.de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import java.de.unistuttgart.stayinsync.pollingnode.usercontrol.management.PollingJobManagement;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ConsumerControlManagementTest {

    @InjectMock
    PollingJobManagement pollingNodeManagement;

    @Inject
    @Channel("syncJobCreated")
    Emitter<SyncJob> createdEmitter;


    @Inject
    @Channel("syncJobDeleted")
    Emitter<SyncJob> deletedEmitter;


    private SyncJob properSyncJob;
    private SyncJob faultySyncJobApiAddressIsNull;
    private SyncJob faultySyncJobEmptyApiAddress;

    @BeforeEach
    public void initSyncJobs(){
        this.  properSyncJob = new SyncJob("validApiAddress");
        this.faultySyncJobApiAddressIsNull = new SyncJob(null);
        this.faultySyncJobEmptyApiAddress = new SyncJob("");
    }

    @Test
    public void testStartSyncJobSupportSuccessful() {
        createdEmitter.send(properSyncJob);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            fail("Test wurde unterbrochen", e);
        }

        verify(pollingNodeManagement, times(1)).beginSupportOfSyncJob(properSyncJob);
    }

    @Test
    public void testSupportedSyncNodeDeletionRequest() {
        deletedEmitter.send(properSyncJob);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verify(pollingNodeManagement, times(1)).endSupportOfSyncJob(properSyncJob);
    }


    @Test
    public void testFaultySyncJobApiAddressIsNull() {
        createdEmitter.send(faultySyncJobApiAddressIsNull);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test was cancelled", e);
        }

        verifyNoPollingNodeInteraction();
    }

    @Test
    public void testFaultySyncJobEmptyApiAddress() {
        createdEmitter.send(faultySyncJobEmptyApiAddress);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verifyNoPollingNodeInteraction();
    }


    private void verifyNoPollingNodeInteraction() {
        verify(pollingNodeManagement, never()).beginSupportOfSyncJob(any());
        verify(pollingNodeManagement, never()).endSupportOfSyncJob(any());
    }
}
