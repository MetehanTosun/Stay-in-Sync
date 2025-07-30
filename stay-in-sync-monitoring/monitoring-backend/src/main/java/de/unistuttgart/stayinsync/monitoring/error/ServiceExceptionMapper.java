package de.unistuttgart.stayinsync.monitoring.error;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {

    private static final Logger LOGGER = Logger.getLogger(ServiceExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ServiceException exception) {
        String path = exception.getPath() != null ? exception.getPath() : uriInfo.getPath();
        Response.Status status = ErrorMapperUtils.resolveHttpStatus(exception.getErrorType());

        LOGGER.errorf("ServiceException occurred at %s: %s [%s]", path, exception.getMessage(), exception.getErrorType());

        ErrorResponse errorResponse = new ErrorResponse(
                "Service Error",
                exception.getMessage(),
                exception.getErrorType(),
                path,
                status
        );

        return Response
                .status(status)
                .entity(errorResponse)
                .build();
    }
}
