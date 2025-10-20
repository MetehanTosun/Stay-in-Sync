package de.unistuttgart.stayinsync.core.monitoring.core.configuration.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay.ReplayExecuteRequestDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay.ReplayExecuteResponseDTO;
import de.unistuttgart.stayinsync.monitoring.core.configuration.clients.SnapshotClient;
import de.unistuttgart.stayinsync.monitoring.core.configuration.clients.TransformationScriptClient;
import de.unistuttgart.stayinsync.monitoring.core.configuration.rest.ReplayResource;
import de.unistuttgart.stayinsync.monitoring.core.configuration.service.ReplayExecutor;
import jakarta.ws.rs.core.Response;

/**
 * Unit tests for {@link ReplayResource}.
 * <p>
 * These tests validate that the resource correctly delegates to
 * {@link ReplayExecutor},
 * builds proper {@link Response} objects, and handles input parameters as
 * expected.
 * Dependencies are mocked to isolate the REST resource logic.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
public class ReplayResourceTest {

    @Mock
    private ReplayExecutor executor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SnapshotClient snapshotClient;

    @Mock
    private TransformationScriptClient transformationScriptClient;

    @InjectMocks
    private ReplayResource resource;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("execute(): delegates correctly to ReplayExecutor and wraps response")
    void execute_delegatesToExecutor_returnsReplayResponse() {
        // arrange
        var dummyResult = new ReplayExecutor.Result(Map.of("out", 123), Map.of("varA", 456), null);
        when(executor.execute(eq("myScript.js"), anyString(), any(), anyString())).thenReturn(dummyResult);

        var dto = new ReplayExecuteRequestDTO("myScript.js", "function transform(){return 123;}", null, "sdk");

        // act
        Response resp = resource.execute(dto);

        // assert
        assertEquals(200, resp.getStatus(), "Should return HTTP 200 OK");

        ReplayExecuteResponseDTO body = (ReplayExecuteResponseDTO) resp.getEntity();
        assertNotNull(body);
        assertEquals(dummyResult.outputData(), body.outputData(), "Output data should match executor result");
        assertEquals(dummyResult.variables(), body.variables(), "Captured vars should match executor result");
        assertNull(body.errorInfo(), "Error info should be null for success path");

        verify(executor, times(1)).execute(eq("myScript.js"), anyString(), any(), eq("sdk"));
    }

    @Test
    @DisplayName("execute(): assigns default script name when null and handles errorInfo")
    void execute_defaultNameAndErrorInfo() {
        // arrange
        var dummyResult = new ReplayExecutor.Result(null, Map.of(), "someError");
        when(executor.execute(eq("replay.js"), anyString(), any(), isNull())).thenReturn(dummyResult);

        var dto = new ReplayExecuteRequestDTO(null, "function transform(){throw 'err';}", null, null);

        // act
        Response resp = resource.execute(dto);

        // assert
        assertEquals(200, resp.getStatus(), "Should still return HTTP 200 OK");

        ReplayExecuteResponseDTO body = (ReplayExecuteResponseDTO) resp.getEntity();
        assertEquals("someError", body.errorInfo());
        assertTrue(body.variables().isEmpty());
        verify(executor).execute(eq("replay.js"), anyString(), any(), isNull());
    }
}
