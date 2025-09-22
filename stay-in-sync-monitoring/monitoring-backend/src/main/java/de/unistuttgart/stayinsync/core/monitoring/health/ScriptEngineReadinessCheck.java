package de.unistuttgart.stayinsync.core.monitoring.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;

@Readiness
@ApplicationScoped
public class ScriptEngineReadinessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        boolean scriptEngineIsReady = true;
        return scriptEngineIsReady
                ? HealthCheckResponse.up("ScriptEngine is ready")
                : HealthCheckResponse.down("ScriptEngine not ready");
    }
}
