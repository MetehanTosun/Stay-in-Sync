package de.unistuttgart.stayinsync.core.configuration.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class ScriptMetricsService {

    private final MeterRegistry registry;
    private final ConcurrentMap<Long, Counter> executionCounters = new ConcurrentHashMap<Long, Counter>();
    private final ConcurrentMap<Long, Timer> executionTimers = new ConcurrentHashMap<Long, Timer>();

    @Inject
    public ScriptMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordExecution(Long transformationId, Runnable runnable) {
        Timer timer = executionTimers.computeIfAbsent(transformationId,
                id -> Timer.builder("stayinsync.script.execution.time")
                        .description("Dauer der Skriptausführung")
                        .tag("transformationId", id.toString())
                        .register(registry));

        Counter counter = executionCounters.computeIfAbsent(transformationId,
                id -> Counter.builder("stayinsync.script.executions")
                        .description("Anzahl der Skriptausführungen")
                        .tag("transformationId", id.toString())
                        .register(registry));

        timer.record(runnable);
        counter.increment();
    }
}
