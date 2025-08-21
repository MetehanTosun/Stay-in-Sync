package de.unistuttgart.stayinsync.monitoring.grpc;


import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "logging.grpc")
public interface GrpcLoggingConfig {
    String host();
    int port();
}
