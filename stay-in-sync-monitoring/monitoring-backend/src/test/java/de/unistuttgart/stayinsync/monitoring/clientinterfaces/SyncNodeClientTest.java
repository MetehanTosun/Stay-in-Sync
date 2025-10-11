package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SyncNodeClient using Mockito to mock HTTP requests.
 * This version does not require TestUtils and uses constructor injection
 * for mocks where needed.
 */
class SyncNodeClientTest {

    private SyncNodeClient client;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private ObjectMapper mapper;

    private final String baseUrl = "http://localhost:9999";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a real ObjectMapper for JSON serialization
        mapper = new ObjectMapper();

        // Use a small helper constructor for testing to inject mocks
        client = new SyncNodeClient(mockHttpClient, mapper, "http://localhost:9999");

    }

    // === getLatestAll ===
    @Test
    void testGetLatestAll_success() throws Exception {
        Map<Long, SnapshotDTO> map = Map.of(1L, new SnapshotDTO());
        String json = mapper.writeValueAsString(map);

        when(mockHttpClient.<String>send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);

        Map<Long, SnapshotDTO> result = client.getLatestAll();
        assertEquals(1, result.size());
    }

    @Test
    void testGetLatestAll_errorStatus() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(500);

        Map<Long, SnapshotDTO> result = client.getLatestAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLatestAll_exception() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenThrow(new RuntimeException("Error"));

        Map<Long, SnapshotDTO> result = client.getLatestAll();
        assertTrue(result.isEmpty());
    }

    // === getLatest ===
    @Test
    void testGetLatest_success() throws Exception {
        SnapshotDTO dto = new SnapshotDTO();
        String json = mapper.writeValueAsString(dto);

        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);

        SnapshotDTO result = client.getLatest(42L);
        assertNotNull(result);
    }

    @Test
    void testGetLatest_errorStatus() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(404);

        SnapshotDTO result = client.getLatest(42L);
        assertNull(result);
    }

    @Test
    void testGetLatest_exception() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenThrow(new RuntimeException("Error"));

        SnapshotDTO result = client.getLatest(42L);
        assertNull(result);
    }

    // === getLastFive ===
    @Test
    void testGetLastFive_success() throws Exception {
        List<SnapshotDTO> list = List.of(new SnapshotDTO(), new SnapshotDTO());
        String json = mapper.writeValueAsString(list);

        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);

        List<SnapshotDTO> result = client.getLastFive(7L);
        assertEquals(2, result.size());
    }

    @Test
    void testGetLastFive_errorStatus() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(500);

        List<SnapshotDTO> result = client.getLastFive(7L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLastFive_exception() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenThrow(new RuntimeException("Crash"));

        List<SnapshotDTO> result = client.getLastFive(7L);
        assertTrue(result.isEmpty());
    }

    // === getById ===
    @Test
    void testGetById_success() throws Exception {
        SnapshotDTO dto = new SnapshotDTO();
        String json = mapper.writeValueAsString(dto);

        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);

        SnapshotDTO result = client.getById(99L);
        assertNotNull(result);
    }

    @Test
    void testGetById_errorStatus() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(400);

        SnapshotDTO result = client.getById(99L);
        assertNull(result);
    }

    @Test
    void testGetById_exception() throws Exception {
        when(mockHttpClient.<String>send(any(), any())).thenThrow(new RuntimeException("Fail"));

        SnapshotDTO result = client.getById(99L);
        assertNull(result);
    }
}



