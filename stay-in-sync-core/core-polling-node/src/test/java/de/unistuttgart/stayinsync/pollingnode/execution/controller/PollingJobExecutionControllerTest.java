//package de.unistuttgart.stayinsync.pollingnode.execution.controller;
//
//import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
//import de.unistuttgart.stayinsync.pollingnode.execution.controller.PollingJobExecutionController;
//import de.unistuttgart.stayinsync.pollingnode.rabbitmq.ProducerSendPolledData;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class PollingJobExecutionControllerTest {
//
//    private ProducerSendPolledData producer;
//    private PollingJobExecutionController controller;
//
//    @BeforeEach
//    void setUp() {
//        controller = new PollingJobExecutionController(producer);
//    }
//
//    @Test
//    void testStartPollingJobExecutionAddsJobAndSchedulesTask() {
//        PollingJob job = new PollingJob("https://example.com");
//
//        controller.startPollingJobExecution(job);
//
//        assertTrue(controller.pollingJobExists(job.getApiAddress()));
//    }
//
//    @Test
//    void testStopPollingJobExecutionRemovesJobAndCancelsTask() {
//        PollingJob job = new PollingJob("https://example.com");
//
//        controller.startPollingJobExecution(job);
//        assertTrue(controller.pollingJobExists(job.getApiAddress()));
//
//        controller.stopPollingJobExecution(job.getApiAddress());
//        assertFalse(controller.pollingJobExists(job.getApiAddress()));
//    }
//
//    @Test
//    void testPollingJobExistsReturnsCorrectValue() {
//        PollingJob job = new PollingJob("https://example.com");
//
//        assertFalse(controller.pollingJobExists(job.getApiAddress()));
//        controller.startPollingJobExecution(job);
//        assertTrue(controller.pollingJobExists(job.getApiAddress()));
//    }
//}
