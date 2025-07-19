package de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

import java.util.Optional;

public class RequestExecutionException extends PollingJobException {

    final HttpRequest<Buffer> request;


    public RequestExecutionException(String message, Throwable cause, final HttpRequest<Buffer> request) {
        super(message, cause);
        this.request = request;
    }

}
