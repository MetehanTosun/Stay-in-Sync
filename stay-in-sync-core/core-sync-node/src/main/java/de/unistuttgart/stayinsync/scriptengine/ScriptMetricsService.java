package de.unistuttgart.stayinsync.scriptengine;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class ScriptMetricsService {

    @Inject
    MeterRegistry registry;

    private final ConcurrentMap<Long, Counter> executionCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Timer> executionTimers = new ConcurrentHashMap<>();

    public void recordExecution(Long transformationId, Runnable runnable) {
        Timer timer = executionTimers.computeIfAbsent(transformationId,
                id -> Timer.builder("stayinsync.script.execution.time")
                        .description("Dauer der Skriptausführung")
                        .tag("transformationId", id.toString())
                        .register(registry));

        Counter counter = executionCounters.computeIfAbsent(transformationId,
                id -> Counter.builder("stayinsync.script.executions")
                        .description("Amount of script executions")
                        .tag("transformationId", id.toString())
                        .register(registry));

        timer.record(runnable);
        counter.increment();
    }
}