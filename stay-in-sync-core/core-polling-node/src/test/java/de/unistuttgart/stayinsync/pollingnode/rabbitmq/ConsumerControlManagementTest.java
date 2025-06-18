package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.management.PollingNodeManagement;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ConsumerControlManagementIT {

    @InjectMock
    PollingNodeManagement pollingNodeManagement;

    @Inject
    @Channel("syncJobCreated")
    Emitter<SyncJob> createdEmitter;

    @Inject
    @Channel("syncJobEdited")
    Emitter<SyncJob> editedEmitter;

    @Inject
    @Channel("syncJobDeleted")
    Emitter<SyncJob> deletedEmitter;

    private SyncJob syncJobNoId;
    private SyncJob syncJobNoApiAddressesEmpty;
    private SyncJob syncJobNoApiAddressesSet;
    private SyncJob syncJobNoTiming;
    private SyncJob syncJobNoScript;
    private SyncJob syncJobNoActiveState;
    private SyncJob properSyncJob;

    @BeforeEach
    public void initSyncJobs(){
        Set<String> correctApiAddressesSet = new HashSet<>();
        correctApiAddressesSet.add("apiAddress");
        syncJobNoId = new SyncJob(null, correctApiAddressesSet, 10, "script", true);
        syncJobNoApiAddressesEmpty = new SyncJob(1L, new HashSet<>(), 10, "script", true);
        syncJobNoApiAddressesSet = new SyncJob(1L, null, 10, "script", false);;
        syncJobNoTiming = new SyncJob(1L, correctApiAddressesSet, null, "script", true);;
        syncJobNoScript = new SyncJob(1L, correctApiAddressesSet, 10, null, true);;
        syncJobNoActiveState = new SyncJob(1L, correctApiAddressesSet, 10, "script", null);
        properSyncJob = new SyncJob(1L, correctApiAddressesSet, 10, "script", true);
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
    public void testSupportedSyncJobUpdateRequestSuccessful()  {
        editedEmitter.send(properSyncJob);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verify(pollingNodeManagement, times(1)).editSupportedSyncJob(properSyncJob);
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
    public void testFaultySyncJobNoIdBlocked() {
        createdEmitter.send(syncJobNoId);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verifyNoPollingNodeInteraction();
    }

    @Test
    public void testFaultySyncJobEmptyApiAddressesBlocked() {
        createdEmitter.send(syncJobNoApiAddressesEmpty);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verifyNoPollingNodeInteraction();
    }

    @Test
    public void testFaultySyncJobNullApiAddressesBlocked() {
        createdEmitter.send(syncJobNoApiAddressesSet);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verifyNoPollingNodeInteraction();
    }

    @Test
    public void testFaultySyncJobNoTimingBlocked() {
        createdEmitter.send(syncJobNoTiming);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verifyNoPollingNodeInteraction();
    }

    @Test
    public void testFaultySyncJobNoScriptBlocked() {
        createdEmitter.send(syncJobNoScript);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test wurde unterbrochen", e);
        }

        verifyNoPollingNodeInteraction();
    }

    @Test
    public void testFaultySyncJobNoActiveStateBlocked() {
        createdEmitter.send(syncJobNoActiveState);

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
        verify(pollingNodeManagement, never()).editSupportedSyncJob(any());
        verify(pollingNodeManagement, never()).endSupportOfSyncJob(any());
    }
}
