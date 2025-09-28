package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ClientLogger {

    @Inject
    @ConfigProperty(name = "backend-api/mp-rest/url")
    String backendApiUrl;

    public void logUrl() {
        Log.info("Backend API URL: " + backendApiUrl);
    }
}

