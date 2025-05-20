package de.unistuttgart.stayinsync.monitoring.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Provider
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {

    private static final Logger LOGGER = Logger.getLogger(ServiceExceptionMapper.class);

    @Override
    public Response toResponse(ServiceException exception) {
        LOGGER.errorf("ServiceException occurred: %s [%s]", exception.getMessage(), exception.getErrorType());

        //will be edited soon
        Map<String, String> error = new HashMap<>();
        error.put("message", exception.getMessage());
        error.put("type", exception.getErrorType().name());

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
    }
}
