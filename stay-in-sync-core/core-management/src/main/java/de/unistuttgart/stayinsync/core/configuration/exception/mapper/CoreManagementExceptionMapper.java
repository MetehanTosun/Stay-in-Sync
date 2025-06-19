package de.unistuttgart.stayinsync.core.configuration.exception.mapper;

import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class CoreManagementExceptionMapper {

    @ServerExceptionMapper
    public Response mapException(CoreManagementException x) {
        return Response //
                .status(Response.Status.INTERNAL_SERVER_ERROR) //
                .entity(new ErrorResponse(x.getTitle(), x.getMessage())) //
                .type(MediaType.APPLICATION_JSON) //
                .build();
    }

    private record ErrorResponse(String errorTitle, String errorMessage) {
    }

}
