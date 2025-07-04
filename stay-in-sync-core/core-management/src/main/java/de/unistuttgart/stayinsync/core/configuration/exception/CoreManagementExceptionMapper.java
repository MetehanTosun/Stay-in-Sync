package de.unistuttgart.stayinsync.core.configuration.exception;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class CoreManagementExceptionMapper {
    /**
     * Maps {@link CoreManagementException} to {@link Response]
     *
     * @param exception
     * @return response with status 500 containing error data
     */
    @ServerExceptionMapper
    public Response mapException(CoreManagementException exception) {
        return exception.buildResponse();
    }

    private record ErrorResponse(String errorTitle, String errorMessage) {
    }

}
