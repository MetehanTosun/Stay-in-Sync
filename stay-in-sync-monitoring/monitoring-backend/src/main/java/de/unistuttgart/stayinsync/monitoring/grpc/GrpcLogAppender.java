package de.unistuttgart.stayinsync.monitoring.grpc;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.grpc.StatusRuntimeException;
import jakarta.inject.Inject;

public class GrpcLogAppender extends AppenderBase<ILoggingEvent> {

    @Inject
    LogServiceClient logServiceClient;

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

            logServiceClient.sendLog(entry);

        } catch (StatusRuntimeException e) {
            addError("Failed to send log via gRPC", e);
        }
    }
}


