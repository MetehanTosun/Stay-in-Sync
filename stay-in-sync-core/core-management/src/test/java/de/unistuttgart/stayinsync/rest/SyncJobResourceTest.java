package de.unistuttgart.stayinsync.rest;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SyncJobService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@QuarkusTest
public class SyncJobResourceTest {

    private static final String DEFAULT_NAME = "Sync Produktion A";
    private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
    private static final long DEFAULT_ID = 1;
    private static final String DEFAULT_DESCRIPTION = "Sync Produktion A";

    @InjectMock
    SyncJobService syncJobService;

    @Inject
    SyncJobFullUpdateMapper mapper;

    @BeforeAll
    static void beforeAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldFullyUpdateAnItem() {
        var syncJob = createFullyUpdatedSyncJob();
        ArgumentMatcher<SyncJob> syncJobArgumentMatcher = v ->
                (v.id == DEFAULT_ID) &&
                        v.name.equals(UPDATED_NAME);

        when(this.syncJobService.replaceSyncJob(argThat(syncJobArgumentMatcher)))
                .thenReturn(Optional.of(mapper.mapToEntity(syncJob)));

        given()
                .when()
                .body(syncJob)
                .contentType(JSON)
                .accept(JSON)
                .put("/api/config/sync-job/{id}", syncJob.id())
                .then()
                .statusCode(NO_CONTENT.getStatusCode())
                .body(blankOrNullString());

        verify(this.syncJobService).replaceSyncJob(argThat(syncJobArgumentMatcher));
        verifyNoMoreInteractions(this.syncJobService);
    }

    @Test
    void shouldGetItemsWithNameFilter() {
        when(this.syncJobService.findAllSyncJobsHavingName("Sync Produktion A"))
                .thenReturn(List.of(mapper.mapToEntity(createDefaultSyncJob())));

        var defaultSyncJob = new SyncJob();
        defaultSyncJob.id = DEFAULT_ID;
        defaultSyncJob.name = DEFAULT_NAME;

        var syncJobs = given()
                .when()
                .queryParam("name_filter", "Sync Produktion A")
                .get("/api/config/sync-job")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .extract().body()
                .jsonPath().getList(".", SyncJobDTO.class);

        assertThat(syncJobs)
                .singleElement()
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
                .isEqualTo(mapper.mapToDTO(defaultSyncJob));

        verify(this.syncJobService).findAllSyncJobsHavingName("Sync Produktion A");
        verifyNoMoreInteractions(this.syncJobService);
    }

    public static SyncJobDTO createFullyUpdatedSyncJob() {
        var syncJob = new SyncJobDTO(DEFAULT_ID, UPDATED_NAME, false, DEFAULT_DESCRIPTION, true);

        return syncJob;
    }

    private static SyncJobDTO createDefaultSyncJob() {
        var syncJob = new SyncJobDTO(DEFAULT_ID, DEFAULT_NAME, false, DEFAULT_DESCRIPTION, false);

        return syncJob;
    }
}
