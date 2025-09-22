package de.unistuttgart.stayinsync.core.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.service.SyncJobService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class SyncJobServiceTest {

    private static final Long DEFAULT_ID = 1L;
    private static final String DEFAULT_NAME = "Sync Produktion A";

    @Inject
    @InjectMocks
    SyncJobService syncJobService;



    @Inject
    SyncJobFullUpdateMapper mapper;

    @Test
    void findAllSyncJobsByIdNotFound() {
        PanacheMock.mock(SyncJob.class);
        when(SyncJob.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.syncJobService.findSyncJobById(DEFAULT_ID))
                .isInstanceOf(CoreManagementException.class)
                .hasMessage("There is no sync-job with id: 1");

        PanacheMock.verify(SyncJob.class).findByIdOptional(eq(DEFAULT_ID));
        PanacheMock.verifyNoMoreInteractions(SyncJob.class);
    }

    @Test
    void deleteSyncJob_found() {
        // Arrange
        PanacheMock.mock(SyncJob.class);

        SyncJob mockJob = mock(SyncJob.class);
        when(SyncJob.findById(eq(DEFAULT_ID))).thenReturn(mockJob);

        // Act
        syncJobService.deleteSyncJob(DEFAULT_ID);

        // Assert
        verify(mockJob).delete();
        PanacheMock.verify(SyncJob.class).findById(eq(DEFAULT_ID));
        PanacheMock.verifyNoMoreInteractions(SyncJob.class);
    }

    @Test
    void persistInvalidSyncJob() {
        PanacheMock.mock(SyncJob.class);
        var syncJob = createDefaultSyncJob();
        syncJob.name = "a";
//TODO: Fix test

//        var cve = catchThrowableOfType(ArcUndeclaredThrowableException.class, () -> this.syncJobService.persistSyncJob(mapper.mapToDTO(syncJob)));
//
//        assertThat(cve)
//                .isNotNull();

//        var violations = cve.getConstraintViolations();
//
//        assertThat(violations)
//                .isNotNull()
//                .hasSize(1);
//
//        assertThat(violations.stream().findFirst())
//                .isNotNull()
//                .isPresent()
//                .get()
//                .extracting(
//                        ConstraintViolation::getInvalidValue,
//                        ConstraintViolation::getMessage
//                )
//                .containsExactly(
//                        "a",
//                        "size must be between 2 and 50"
//                );

        PanacheMock.verifyNoInteractions(SyncJob.class);
    }

    private static SyncJob createDefaultSyncJob() {
        SyncJob syncJob = new SyncJob();
        syncJob.id = DEFAULT_ID;
        syncJob.name = DEFAULT_NAME;

        return syncJob;
    }

}
