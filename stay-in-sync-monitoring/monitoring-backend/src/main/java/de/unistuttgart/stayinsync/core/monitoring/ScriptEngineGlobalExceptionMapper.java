package de.unistuttgart.stayinsync.core.monitoring;

import de.unistuttgart.stayinsync.core.monitoring.error.MonitoringUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ScriptEngineGlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {

        String jobId = "unknown";
        String scriptId = "unknown";



        MonitoringUtils.logUnexpectedScriptEngineException(jobId, scriptId, exception);


        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Internal Server Error\"}")
                .build();
    }
}