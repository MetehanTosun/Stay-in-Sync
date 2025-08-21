package de.unistuttgart.stayinsync.monitoring.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

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
        channel = ManagedChannelBuilder
                .forAddress(config.host(), config.port())
                .usePlaintext()
                .build();

        blockingStub = LogServiceGrpc.newBlockingStub(channel);
    }

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
