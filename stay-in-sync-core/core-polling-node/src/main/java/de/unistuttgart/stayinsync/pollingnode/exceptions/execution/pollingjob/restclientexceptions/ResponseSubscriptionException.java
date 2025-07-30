package de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.PollingJobException;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

public class ResponseSubscriptionException extends PollingJobException {

    final HttpRequest<Buffer> request;

    public ResponseSubscriptionException(String message, Throwable cause, final HttpRequest<Buffer> request){
        super(message,cause);
        this.request = request;
    }


}
