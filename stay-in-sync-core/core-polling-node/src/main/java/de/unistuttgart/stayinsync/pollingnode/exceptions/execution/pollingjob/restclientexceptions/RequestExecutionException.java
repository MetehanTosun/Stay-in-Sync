package de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.PollingJobException;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

public class RequestExecutionException extends PollingJobException {

    public RequestExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
