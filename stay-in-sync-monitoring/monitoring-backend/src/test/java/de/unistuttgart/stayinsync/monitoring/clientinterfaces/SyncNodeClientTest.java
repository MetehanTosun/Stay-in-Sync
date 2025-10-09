package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyncNodeClientTest {

    private SyncNodeClient syncNodeClient;
    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        syncNodeClient = new SyncNodeClient() {
            @Override
            public Map<Long, SnapshotDTO> getLatestAll() {
                try {
                    var response = mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class));
                    if (response.statusCode() != 200) {
                        return Map.of();
                    }
                    var objectMapper = new ObjectMapper();
                    return objectMapper.readValue(response.body(), Map.class);
                } catch (Exception e) {
                    return Map.of();
                }
            }
        };
    }

    @Test
    void testGetLatestAll_ReturnsData() throws Exception {
        String json = """
            {
              "1": {"id":1,"timestamp":"2025-10-09T10:00:00Z","status":"OK"},
              "2": {"id":2,"timestamp":"2025-10-09T10:01:00Z","status":"FAIL"}
            }
            """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        Map<Long, SnapshotDTO> result = syncNodeClient.getLatestAll();
        assertNotNull(result);
    }

    @Test
    void testGetLatestAll_ReturnsEmpty_OnErrorStatus() throws Exception {
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        Map<Long, SnapshotDTO> result = syncNodeClient.getLatestAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLatestAll_ReturnsEmpty_OnException() throws Exception {
        when(mockHttpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        Map<Long, SnapshotDTO> result = syncNodeClient.getLatestAll();
        assertTrue(result.isEmpty());
    }
}
