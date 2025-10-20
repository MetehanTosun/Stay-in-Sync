package de.unistuttgart.stayinsync.core.syncnode.SnapshotManagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotFactory;
import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotResource;
import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotStore;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.ws.rs.core.Response;

/**
 * Unit tests for {@link SnapshotResource}.
 *
 * Covers all endpoints: create from result, byId, latest, list, latestAll.
 * SnapshotStore and SnapshotFactory are mocked to isolate the resource logic.
 *
 * @author Mohammed-Ammar Hassnou
 */
public class SnapshotResourceTest {

    @Mock
    private SnapshotStore store;

    @Mock
    private ObjectMapper om;

    @InjectMocks
    private SnapshotResource resource;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // --- createFromResult ---

    @Test
    @DisplayName("createFromResult(): 400 when body is null")
    void createFromResult_nullBody_returns400() {
        Response r = resource.createFromResult(null);
        assertEquals(400, r.getStatus());
    }

    @Test
    @DisplayName("createFromResult(): 201 + persists snapshot when valid body")
    void createFromResult_validBody_persistsAndReturns201() {
        TransformationResult tr = mock(TransformationResult.class);
        SnapshotDTO dto = new SnapshotDTO();
        try (MockedStatic<SnapshotFactory> sf = Mockito.mockStatic(SnapshotFactory.class)) {
            sf.when(() -> SnapshotFactory.fromTransformationResult(tr, om)).thenReturn(dto);

            Response r = resource.createFromResult(tr);

            assertEquals(201, r.getStatus());
            assertSame(dto, r.getEntity());
            verify(store).put(dto);
        }
    }

    // --- byId ---

    @Test
    @DisplayName("byId(): 200 with entity when found")
    void byId_found_returns200() {
        SnapshotDTO dto = new SnapshotDTO();
        when(store.getBySnapshotId("s1")).thenReturn(Optional.of(dto));

        Response r = resource.byId("s1");
        assertEquals(200, r.getStatus());
        assertSame(dto, r.getEntity());
    }

    @Test
    @DisplayName("byId(): 404 when not found")
    void byId_notFound_returns404() {
        when(store.getBySnapshotId("missing")).thenReturn(Optional.empty());
        Response r = resource.byId("missing");
        assertEquals(404, r.getStatus());
    }

    // --- latest ---

    @Test
    @DisplayName("latest(): 400 when transformationId is missing")
    void latest_missingQueryParam_returns400() {
        Response r = resource.latest(null);
        assertEquals(400, r.getStatus());
    }

    @Test
    @DisplayName("latest(): 200 when snapshot exists")
    void latest_found_returns200() {
        SnapshotDTO dto = new SnapshotDTO();
        when(store.getLatestByTransformationId(42L)).thenReturn(Optional.of(dto));
        Response r = resource.latest(42L);
        assertEquals(200, r.getStatus());
        assertSame(dto, r.getEntity());
    }

    @Test
    @DisplayName("latest(): 404 when snapshot missing")
    void latest_notFound_returns404() {
        when(store.getLatestByTransformationId(43L)).thenReturn(Optional.empty());
        Response r = resource.latest(43L);
        assertEquals(404, r.getStatus());
    }

    // --- list ---

    @Test
    @DisplayName("list(): 400 when transformationId missing")
    void list_missingParam_returns400() {
        Response r = resource.list(null, 5);
        assertEquals(400, r.getStatus());
    }

    @Test
    @DisplayName("list(): normalizes non-positive limit and returns 200 with list")
    void list_normalizesLimit_andReturns200() {
        SnapshotDTO a = new SnapshotDTO();
        SnapshotDTO b = new SnapshotDTO();
        when(store.listByTransformationId(7L, 1)).thenReturn(List.of(a, b));

        // pass 0 to trigger Math.max(1, limit)
        Response r = resource.list(7L, 0);
        assertEquals(200, r.getStatus());
        @SuppressWarnings("unchecked")
        List<SnapshotDTO> list = (List<SnapshotDTO>) r.getEntity();
        assertEquals(2, list.size());
        verify(store).listByTransformationId(7L, 1);
    }

    // --- latestAll ---

    @Test
    @DisplayName("latestAll(): 200 with map payload")
    void latestAll_returnsMap200() {
        SnapshotDTO s1 = new SnapshotDTO();
        when(store.getLatestByAllTransformationIds()).thenReturn(Map.of(1L, s1));
        Response r = resource.latestAll();
        assertEquals(200, r.getStatus());
        @SuppressWarnings("unchecked")
        Map<Long, SnapshotDTO> body = (Map<Long, SnapshotDTO>) r.getEntity();
        assertEquals(1, body.size());
        assertSame(s1, body.get(1L));
    }
}
