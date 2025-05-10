package de.unistuttgart.stayinsync.monitoring.error;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

//this class should be a global error handler

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErrorMapper.class);

    @Override
    public Response toResponse(final Throwable exception) {
        if (exception instanceof ServiceException se) {
            LOG.error("Handled ServiceException", se);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Service Error", se.getMessage(), se.getErrorType()))
                    .build();
        }

        LOG.error("Unhandled exception", exception);
        return Response.serverError()
                .entity(new ErrorResponse("Unknown Error", exception.getMessage(), ErrorType.UNKNOWN_ERROR))
                .build();
    }
}