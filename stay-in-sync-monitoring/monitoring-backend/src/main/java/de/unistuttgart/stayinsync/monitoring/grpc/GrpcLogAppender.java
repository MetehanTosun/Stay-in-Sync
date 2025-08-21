package de.unistuttgart.stayinsync.monitoring.grpc;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class GrpcLogAppender extends AppenderBase<ILoggingEvent> {

    private ManagedChannel channel;
    private LogServiceGrpc.LogServiceBlockingStub blockingStub;

    private String host = "localhost"; //
    private int port = 9090;           

    @Override
    public void start() {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = LogServiceGrpc.newBlockingStub(channel);
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        try {
            LogEntry entry = LogEntry.newBuilder()
                    .setLevel(eventObject.getLevel().toString())
                    .setLogger(eventObject.getLoggerName())
                    .setMessage(eventObject.getFormattedMessage())
                    .setThread(eventObject.getThreadName())
                    .setTimestamp(eventObject.getTimeStamp())
                    .build();

            blockingStub.sendLog(entry);

        } catch (StatusRuntimeException e) {
            addError("Failed to send log via gRPC", e);
        }
    }

    @Override
    public void stop() {
        if (channel != null) {
            channel.shutdown();
        }
        super.stop();
    }
}

