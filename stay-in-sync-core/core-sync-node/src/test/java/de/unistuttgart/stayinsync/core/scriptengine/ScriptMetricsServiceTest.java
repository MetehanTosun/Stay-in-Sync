package de.unistuttgart.stayinsync.core.scriptengine;


import de.unistuttgart.stayinsync.scriptengine.ScriptMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptMetricsServiceTest {

    private SimpleMeterRegistry meterRegistry;
    private ScriptMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new ScriptMetricsService(meterRegistry); // Konstruktor-Injektion
    }

    @Test
    @DisplayName("should record single execution with timer and counter")
    void shouldRecordExecution() {
        Long transformationId = 1L;

        metricsService.recordExecution(transformationId, () -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {}
        });

        Counter counter = meterRegistry.find("stayinsync.script.executions")
                .tag("transformationId", transformationId.toString())
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);

        Timer timer = meterRegistry.find("stayinsync.script.execution.time")
                .tag("transformationId", transformationId.toString())
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("should increment counter and timer on multiple executions")
    void shouldIncrementOnMultipleExecutions() {
        Long transformationId = 42L;

        metricsService.recordExecution(transformationId, () -> {});
        metricsService.recordExecution(transformationId, () -> {});

        Counter counter = meterRegistry.find("stayinsync.script.executions")
                .tag("transformationId", transformationId.toString())
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2);

        Timer timer = meterRegistry.find("stayinsync.script.execution.time")
                .tag("transformationId", transformationId.toString())
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
    }
}


