package de.unistuttgart.stayinsync.core.monitoring.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Unhandled exception caught by GlobalExceptionMapper", exception);

        String jobId = "unknown";
        String scriptId = "unknown";


        MonitoringUtils.logUnexpectedScriptEngineException(jobId, scriptId, exception);


        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(
                        "Internal Server Error",
                        exception.getMessage(),
                        ErrorType.UNKNOWN_ERROR,
                        null,
                        Response.Status.INTERNAL_SERVER_ERROR
                ))
                .build();
    }
}