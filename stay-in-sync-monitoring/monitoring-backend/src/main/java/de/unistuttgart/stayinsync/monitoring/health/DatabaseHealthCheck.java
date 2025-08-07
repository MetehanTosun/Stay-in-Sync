package de.unistuttgart.stayinsync.monitoring.health;

import org.eclipse.microprofile.health.*;
import jakarta.enterprise.context.ApplicationScoped;
import io.micrometer.core.instrument.Metrics;
import java.util.concurrent.atomic.AtomicInteger;

@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    private final AtomicInteger readinessGauge = Metrics.gauge("custom_app_ready", new AtomicInteger(1));

    @Override
    public HealthCheckResponse call() {
        boolean healthy = true;

        if (healthy) {
            readinessGauge.set(1);
            return HealthCheckResponse.up("Database");
        } else {
            readinessGauge.set(0);
            return HealthCheckResponse.down("Database");
        }
    }
}
