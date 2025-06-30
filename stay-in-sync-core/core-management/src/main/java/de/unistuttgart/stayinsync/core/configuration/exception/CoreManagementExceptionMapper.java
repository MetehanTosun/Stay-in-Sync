package de.unistuttgart.stayinsync.core.configuration.exception;

import jakarta.ws.rs.core.MediaType;
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
        return Response //
                .status(Response.Status.INTERNAL_SERVER_ERROR) //
                .entity(new ErrorResponse(exception.getTitle(), exception.getMessage())) //
                .type(MediaType.APPLICATION_JSON) //
                .build();
    }

    private record ErrorResponse(String errorTitle, String errorMessage) {
    }

}
