package de.unistuttgart.stayinsync.core.persistence.entities;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncJob;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.*;

@QuarkusTest
@TestTransaction
public class SyncJobTest {

    private static final String DEFAULT_NAME = "Sync Produktion A";


    @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
    @ValueSource(strings = {DEFAULT_NAME, "A", "a", "Sync", "c P"})
    @EmptySource
    public void findAllWhereNameLikeFound(String name) {
        var syncJob = new SyncJob();
        syncJob.name = DEFAULT_NAME;

        SyncJob.deleteAll();
        SyncJob.persist(syncJob);

        Assertions.assertThat(SyncJob.count())
                .isEqualTo(1L);

        Assertions.assertThat(SyncJob.listAllWhereNameLike(name))
                .isNotNull()
                .hasSize(1)
                .first()
                .usingRecursiveComparison()
                .isEqualTo(syncJob);
    }

    @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
    @ValueSource(strings = {"b", "support", "test"})
    @NullSource
    public void findAllWhereNameLikeNotFound(String name) {
        var syncJob = new SyncJob();
        syncJob.name = DEFAULT_NAME;

        SyncJob.deleteAll();
        SyncJob.persist(syncJob);

        assertThat(SyncJob.count())
                .isEqualTo(1L);

        assertThat(SyncJob.listAllWhereNameLike(name))
                .isNotNull()
                .isEmpty();
    }
}
