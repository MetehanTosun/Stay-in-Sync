package de.unistuttgart.stayinsync.monitoring.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * gRPC client to send log entries to remote logging service.
 */
@ApplicationScoped
public class LogServiceClient {

    private final GrpcLoggingConfig config;
    private ManagedChannel channel;
    private LogServiceGrpc.LogServiceBlockingStub blockingStub;

    public LogServiceClient(GrpcLoggingConfig config) {
        this.config = config;
    }

    @PostConstruct
    void init() {
        // Build gRPC channel using host and port from application.properties
        channel = ManagedChannelBuilder.forAddress(config.host(), config.port())
                .usePlaintext()
                .build();

        // Create blocking stub to send log entries
        blockingStub = LogServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Sends a log entry to the remote gRPC logging service.
     */
    public void sendLog(LogEntry entry) {
        blockingStub.sendLog(entry);
    }

    @PreDestroy
    void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}


