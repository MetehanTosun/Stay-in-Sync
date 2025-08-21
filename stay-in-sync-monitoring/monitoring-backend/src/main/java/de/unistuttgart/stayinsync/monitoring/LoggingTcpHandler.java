package de.unistuttgart.stayinsync.monitoring;

import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.LogRecord;

@ApplicationScoped
public class LoggingTcpHandler extends java.util.logging.Handler {

    private PrintWriter writer;

        @PostConstruct
        void init() throws IOException {
            Socket socket = new Socket("localhost", 5170);
            writer = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void publish(LogRecord record) {
            String json = new JsonObject()
                    .put("level", record.getLevel().toString())
                    .put("message", record.getMessage())
                    .toString();
            writer.println(json);
        }

        @Override public void flush() {}
        @Override public void close() {}


}
