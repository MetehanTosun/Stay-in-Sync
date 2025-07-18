package de.unistuttgart.stayinsync.monitoring.error;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErrorMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(final Throwable exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";
        Response.Status status;
        ErrorResponse errorResponse;

        if (exception instanceof ServiceException se) {
            LOG.error("Handled ServiceException", se);
            status = ErrorMapperUtils.resolveHttpStatus(se.getErrorType());
            errorResponse = new ErrorResponse(
                    "Service Error",
                    se.getMessage(),
                    se.getErrorType(),
                    se.getPath() != null ? se.getPath() : path,
                    status
            );
        } else {
            LOG.error("Unhandled exception", exception);
            status = Response.Status.fromStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            errorResponse = new ErrorResponse(
                    "Unknown Error",
                    exception.getMessage() != null ? exception.getMessage() : "No message available",
                    ErrorType.UNKNOWN_ERROR,
                    path,
                    status
            );
        }

        return Response.status(status)
                .entity(errorResponse)
                .build();
    }
}
