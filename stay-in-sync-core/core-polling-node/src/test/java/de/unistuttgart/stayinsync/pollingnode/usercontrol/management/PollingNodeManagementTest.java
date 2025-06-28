package de.unistuttgart.stayinsync.pollingnode.usercontrol.management;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySyncJobException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingJobNotFoundException;
import de.unistuttgart.stayinsync.pollingnode.execution.controller.PollingJobExecutionController;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.configuration.PollingJobConfigurator;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.management.PollingJobManagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PollingJobManagementTest {

    private PollingJobConfigurator pollingJobConfigurator;
    private PollingJobExecutionController pollingJobExecutionController;
    private PollingJobManagement pollingJobManagement;

    @BeforeEach
    void setUp() {
        pollingJobConfigurator = mock(PollingJobConfigurator.class);
        pollingJobExecutionController = mock(PollingJobExecutionController.class);
        pollingJobManagement = new PollingJobManagement(pollingJobConfigurator, pollingJobExecutionController);
    }

    @Test
    void testBeginSupportOfSyncJobWithValidSyncJob() throws FaultySyncJobException {
        SyncJob syncJob = mock(SyncJob.class);
        PollingJob pollingJob = mock(PollingJob.class);

        when(pollingJobConfigurator.createPollingJob(syncJob)).thenReturn(pollingJob);

        pollingJobManagement.beginSupportOfSyncJob(syncJob);

        verify(pollingJobConfigurator).createPollingJob(syncJob);
        verify(pollingJobExecutionController).startPollingJobExecution(pollingJob);
    }

    @Test
    void testBeginSupportOfSyncJobWithFaultySyncJob() throws FaultySyncJobException {
        SyncJob syncJob = mock(SyncJob.class);
        when(pollingJobConfigurator.createPollingJob(syncJob)).thenThrow(new FaultySyncJobException("Test"));

        pollingJobManagement.beginSupportOfSyncJob(syncJob);

        verify(pollingJobConfigurator).createPollingJob(syncJob);
        verifyNoInteractions(pollingJobExecutionController);
    }

    @Test
    void testEndSupportOfSyncJobWithExistingPollingJob() throws PollingJobNotFoundException {
        SyncJob syncJob = mock(SyncJob.class);
        String apiAddress = "https://example.com";
        when(syncJob.getApiAddress()).thenReturn(apiAddress);
        when(pollingJobExecutionController.pollingJobExists(apiAddress)).thenReturn(true);

        pollingJobManagement.endSupportOfSyncJob(syncJob);

        verify(pollingJobExecutionController).pollingJobExists(apiAddress);
        verify(pollingJobExecutionController).stopPollingJobExecution(apiAddress);
    }

    @Test
    void testEndSupportOfSyncJobWithNonExistingPollingJob() throws PollingJobNotFoundException {
        SyncJob syncJob = mock(SyncJob.class);
        String apiAddress = "https://example.com";
        when(syncJob.getApiAddress()).thenReturn(apiAddress);
        when(pollingJobExecutionController.pollingJobExists(apiAddress)).thenReturn(false);

        pollingJobManagement.endSupportOfSyncJob(syncJob);

        verify(pollingJobExecutionController).pollingJobExists(apiAddress);
        verify(pollingJobExecutionController, never()).stopPollingJobExecution(anyString());
    }
}