package de.unistuttgart.stayinsync;

import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/scripts")
public class ScriptTriggerResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @Inject
    ScriptEngineService scriptEngineService; // Inject your service

    @GET
    @Path("/trigger-job") // Specific path for this endpoint
    @Produces(MediaType.TEXT_PLAIN)
    public Response triggerScriptExecutionJob() {
        try {
            String resultMessage = scriptEngineService.triggerBackgroundJob();
            // You might want to log success here using a proper logger
            System.out.println("HTTP Trigger successful: " + resultMessage);
            return Response.ok(resultMessage).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interruption status
            // Log error
            System.err.println("HTTP Trigger failed - interrupted: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Job submission interrupted: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            // Log error
            System.err.println("HTTP Trigger failed - general error: " + e.getMessage());
            e.printStackTrace(); // For debugging, ideally use a logger
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error triggering script job: " + e.getMessage())
                    .build();
        }
    }
}
