package de.unistuttgart.stayinsync.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobPersistedEvent;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.service.SyncJobService;
import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SyncJobServiceTest {

    private static final Long DEFAULT_ID = 1L;
    private static final String DEFAULT_NAME = "Sync Produktion A";

    @Inject
    @InjectMocks
    SyncJobService syncJobService;

    @Mock
    private Event<SyncJobPersistedEvent> syncJobPersistedEvent;

    @Inject
    SyncJobFullUpdateMapper mapper;

    @Test
    void findAllSyncJobsByIdNotFound() {
        PanacheMock.mock(SyncJob.class);
        when(SyncJob.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

        assertThat(this.syncJobService.findSyncJobById(DEFAULT_ID))
                .isNotNull()
                .isNotPresent();

        PanacheMock.verify(SyncJob.class).findByIdOptional(eq(DEFAULT_ID));
        PanacheMock.verifyNoMoreInteractions(SyncJob.class);
    }

    @Test
    void deleteSyncJob() {
        PanacheMock.mock(SyncJob.class);
        when(SyncJob.deleteById(eq(DEFAULT_ID))).thenReturn(true);

        this.syncJobService.deleteSyncJob(DEFAULT_ID);

        PanacheMock.verify(SyncJob.class).deleteById(eq(DEFAULT_ID));
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
