package de.unistuttgart.stayinsync.monitoring.grpc;

import io.smallrye.config.ConfigMapping;
import io.quarkus.runtime.Startup;

@ConfigMapping(prefix = "logging.grpc")
public interface GrpcLoggingConfig {
    String host();
    int port();
}

@Startup
@jakarta.enterprise.context.ApplicationScoped
class GrpcLoggingConfigValidator {

    private final GrpcLoggingConfig config;

    GrpcLoggingConfigValidator(GrpcLoggingConfig config) {
        this.config = config;
        if (config.host() == null || config.host().isBlank()) {
            throw new IllegalStateException("logging.grpc.host must not be empty");
        }
        if (config.port() <= 0) {
            throw new IllegalStateException("logging.grpc.port must be > 0");
        }
    }

    public GrpcLoggingConfig getConfig() {
        return config;
    }
}
