package de.unistuttgart.stayinsync.monitoring.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;


/**Basic Liveness check,
 *    Showcases bacis up status of services.  Services like script engine, polling node progress
 *    databse status, script progression
 *    Connection to prometheus/prometheus.yml  | get data 
 */
@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("basic-liveness-check");
    }
}
