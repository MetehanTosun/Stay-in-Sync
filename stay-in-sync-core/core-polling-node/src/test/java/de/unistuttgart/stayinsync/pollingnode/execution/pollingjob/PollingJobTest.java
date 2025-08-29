package de.unistuttgart.stayinsync.pollingnode.execution.pollingjob;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.RequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerPublishDataException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerSetUpStreamException;
import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RequestBuilder;
import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;
import de.unistuttgart.stayinsync.pollingnode.execution.pollingjob.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.SyncDataProducer;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-Test Class for PollingJob, a class that uses the Quarz Framework and contains a single public method called execute().
 * In this class execute() is testet with all testMethods.
 */
@ExtendWith(MockitoExtension.class)
class PollingJobTest {
/**
    @Mock
    private RestClient restClient;

    @Mock
    private RequestBuilder requestBuilder;

    @Mock
    private SyncDataProducer syncDataProducer;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private JobDataMap jobDataMap;

    @InjectMocks
    private PollingJob pollingJob;

    private PollingJobDetails pollingJobDetails;
    private RequestBuildingDetails requestBuildingDetails;
    private JsonObject expectedJsonResponse;

    @BeforeEach
    void setUp() {
        // Arrange - Setup test data
        requestBuildingDetails = mock(RequestBuildingDetails.class);
        pollingJobDetails = new PollingJobDetails(
                "testJob",
                1L,
                100,
                true,
                requestBuildingDetails
        );

        expectedJsonResponse = new JsonObject()
                .put("data", "test response")
                .put("timestamp", "2025-01-01T10:00:00Z");

        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDataMap.get("requestConfiguration")).thenReturn(pollingJobDetails);
    }

    @Test
    void successfulExecutionShouldCallAllDependenciesInCorrectOrder() throws Exception {
        // Arrange
        HttpRequest<Buffer> mockRequest = new mock {
        }; // Placeholder für das Request-Objekt
        when(requestBuilder.buildRequest(requestBuildingDetails)).thenReturn(mockRequest);
        when(restClient.pollJsonObjectFromApi(mockRequest)).thenReturn(expectedJsonResponse);

        // Act
        pollingJob.execute(context);

        // Assert - Verify all interactions happened
        verify(requestBuilder).buildRequest(requestBuildingDetails);
        verify(restClient).pollJsonObjectFromApi(mockRequest);
        verify(syncDataProducer).setupRequestConfigurationStream(pollingJobDetails);

        // Capture and verify the published data
        ArgumentCaptor<SyncDataMessageDTO> messageCaptor = ArgumentCaptor.forClass(SyncDataMessageDTO.class);
        verify(syncDataProducer).publishSyncData(messageCaptor.capture());

        SyncDataMessageDTO capturedMessage = messageCaptor.getValue();
        assertEquals(1L, capturedMessage.configId());
        assertTrue(capturedMessage.data().containsKey("testJob"));
        assertEquals(expectedJsonResponse.getMap(), capturedMessage.data().get("testJob"));

        // Verify interaction order
        verifyNoMoreInteractions(requestBuilder, restClient, syncDataProducer);
    }

    @Test
    void execute_missingPollingJobDetails_shouldThrowJobExecutionException() {
        // Arrange
        when(jobDataMap.get("requestConfiguration")).thenReturn(null);

        // Act & Assert
        JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                () -> pollingJob.execute(context)
        );

        assertTrue(exception.getMessage().contains("Configuration not found for ID: null"));

        // Verify no interactions with dependencies
        verifyNoInteractions(requestBuilder, restClient, syncDataProducer);
    }

    @Test
    void execute_requestBuildingException_shouldWrapAndRethrow() throws Exception {
        // Arrange
        RequestBuildingException cause = new RequestBuildingException("Request building failed");
        when(requestBuilder.buildRequest(requestBuildingDetails)).thenThrow(cause);

        // Act & Assert
        JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                () -> pollingJob.execute(context)
        );

        assertTrue(exception.getMessage().contains("Error during polling execution for configId 1"));
        assertTrue(exception.getMessage().contains("Request building failed"));
        assertEquals(cause, exception.getCause());

        // Verify only requestBuilder was called
        verify(requestBuilder).buildRequest(requestBuildingDetails);
        verifyNoInteractions(restClient, syncDataProducer);
    }

    @Test
    void execute_restClientException_shouldWrapAndRethrow() throws Exception {
        // Arrange
        Object mockRequest = new Object();
        RequestExecutionException cause = new RequestExecutionException("REST call failed");

        when(requestBuilder.buildRequest(requestBuildingDetails)).thenReturn(mockRequest);
        when(restClient.pollJsonObjectFromApi(mockRequest)).thenThrow(cause);

        // Act & Assert
        JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                () -> pollingJob.execute(context)
        );

        assertTrue(exception.getMessage().contains("Error during polling execution for configId 1"));
        assertTrue(exception.getMessage().contains("REST call failed"));
        assertEquals(cause, exception.getCause());

        // Verify requestBuilder and restClient were called, but not syncDataProducer
        verify(requestBuilder).buildRequest(requestBuildingDetails);
        verify(restClient).pollJsonObjectFromApi(mockRequest);
        verifyNoInteractions(syncDataProducer);
    }

    @Test
    void execute_producerSetupException_shouldWrapAndRethrow() throws Exception {
        // Arrange
        Object mockRequest = new Object();
        ProducerSetUpStreamException cause = new ProducerSetUpStreamException("Stream setup failed");

        when(requestBuilder.buildRequest(requestBuildingDetails)).thenReturn(mockRequest);
        when(restClient.pollJsonObjectFromApi(mockRequest)).thenReturn(expectedJsonResponse);
        doThrow(cause).when(syncDataProducer).setupRequestConfigurationStream(pollingJobDetails);

        // Act & Assert
        JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                () -> pollingJob.execute(context)
        );

        assertTrue(exception.getMessage().contains("Error during polling execution for configId 1"));
        assertTrue(exception.getMessage().contains("Stream setup failed"));
        assertEquals(cause, exception.getCause());

        // Verify all steps up to the failing one were executed
        verify(requestBuilder).buildRequest(requestBuildingDetails);
        verify(restClient).pollJsonObjectFromApi(mockRequest);
        verify(syncDataProducer).setupRequestConfigurationStream(pollingJobDetails);
        verify(syncDataProducer, never()).publishSyncData(any());
    }

    @Test
    void execute_producerPublishException_shouldWrapAndRethrow() throws Exception {
        // Arrange
        Object mockRequest = new Object();
        ProducerPublishDataException cause = new ProducerPublishDataException("Publish failed");

        when(requestBuilder.buildRequest(requestBuildingDetails)).thenReturn(mockRequest);
        when(restClient.pollJsonObjectFromApi(mockRequest)).thenReturn(expectedJsonResponse);
        doThrow(cause).when(syncDataProducer).publishSyncData(any(SyncDataMessageDTO.class));

        // Act & Assert
        JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                () -> pollingJob.execute(context)
        );

        assertTrue(exception.getMessage().contains("Error during polling execution for configId 1"));
        assertTrue(exception.getMessage().contains("Publish failed"));
        assertEquals(cause, exception.getCause());

        // Verify all steps were attempted
        verify(requestBuilder).buildRequest(requestBuildingDetails);
        verify(restClient).pollJsonObjectFromApi(mockRequest);
        verify(syncDataProducer).setupRequestConfigurationStream(pollingJobDetails);
        verify(syncDataProducer).publishSyncData(any(SyncDataMessageDTO.class));
    }

    @Test
    void convertJsonObjectToSyncDataMessageDTO_shouldCreateCorrectDTO() throws Exception {
        // This tests the private method indirectly through the execute method
        // Arrange
        Object mockRequest = new Object();
        JsonObject testJson = new JsonObject()
                .put("key1", "value1")
                .put("key2", 42);

        when(requestBuilder.buildRequest(requestBuildingDetails)).thenReturn(mockRequest);
        when(restClient.pollJsonObjectFromApi(mockRequest)).thenReturn(testJson);

        // Act
        pollingJob.execute(context);

        // Assert
        ArgumentCaptor<SyncDataMessageDTO> messageCaptor = ArgumentCaptor.forClass(SyncDataMessageDTO.class);
        verify(syncDataProducer).publishSyncData(messageCaptor.capture());

        SyncDataMessageDTO result = messageCaptor.getValue();
        assertEquals(1L, result.configId());
        assertEquals(1, result.data().size());
        assertTrue(result.data().containsKey("testJob"));
        assertEquals(testJson.getMap(), result.data().get("testJob"));
    }
    */
}