package de.unistuttgart.stayinsync.monitoring.grpc;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.inject.Inject;

/**
 * Logback appender that sends log events to gRPC logging service.
 */
public class GrpcLogAppender extends AppenderBase<ILoggingEvent> {

    @Inject
    LogServiceClient logServiceClient;

    @Override
    protected void append(ILoggingEvent eventObject) {
        try {
            // Build a LogEntry from the ILoggingEvent
            LogEntry entry = LogEntry.newBuilder()
                    .setLevel(eventObject.getLevel().toString())
                    .setLogger(eventObject.getLoggerName())
                    .setMessage(eventObject.getFormattedMessage())
                    .setThread(eventObject.getThreadName())
                    .setTimestamp(eventObject.getTimeStamp())
                    .build();


            logServiceClient.sendLog(entry);

        } catch (Exception e) {
            addError("Failed to send log via gRPC", e);
        }
    }
}



