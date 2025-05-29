package de.unistuttgart.stayinsync;

import de.unistuttgart.stayinsync.exception.ScriptEngineException;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.SyncJobFactory;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/scripts")
public class ScriptTriggerResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @Inject
    ScriptEngineService scriptEngineService;

    @GET
    @Path("/trigger-job")
    @Produces(MediaType.APPLICATION_JSON)
    @NonBlocking
    public Uni<Response> triggerScriptExecutionJob() {
        Log.info("Received request for /trigger-job");

        Map<String, Object> mgmtData = new HashMap<>();
        mgmtData.put("facilityName", "Leipzig Plant");
        mgmtData.put("operatorId", "OP-007");
        mgmtData.put("notes", "Scheduled maintenance next week.");
        mgmtData.put("tags", List.of("high-priority", "europe"));

        Map<String, Object> mfgData = new HashMap<>();
        mfgData.put("productId", "MOTOR-V8-TURBO");
        mfgData.put("batchSize", 75);
        mfgData.put("criticalValue", 1.21);
        mfgData.put("status", "In Production");
        Map<String, String> subAssembly = new HashMap<>();
        subAssembly.put("partA", "A-123");
        subAssembly.put("partB", "B-456");
        mfgData.put("subAssemblyParts", subAssembly);

        TransformJob jsonMockJob = SyncJobFactory.getJSONMockTransformationJobTwoNamespaces(
                "aasTransform_01",
                "hash123",
                mgmtData,
                mfgData
        );

        return scriptEngineService.transformAsync(jsonMockJob)
                .onItem().transform(transformationResult -> {
                    if (transformationResult.isValidExecution()) {
                        Log.infof("Async Transformation successful for job %s. Output: %s", jsonMockJob.jobId(), transformationResult.getOutputData());
                        return Response.ok(transformationResult.getOutputData())
                                .type(MediaType.APPLICATION_JSON_TYPE)
                                .build();
                    } else {
                        Log.warnf("Async Transformation failed for job %s: %s", jsonMockJob.jobId(), transformationResult.getErrorInfo());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Transformation failed: " + transformationResult.getErrorInfo())
                                .type(MediaType.TEXT_PLAIN_TYPE)
                                .build();
                    }
                })
                .onFailure().recoverWithItem(ex -> {
                    Log.errorf(ex, "Exception during async transformation for job %s", jsonMockJob.jobId());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("An unexpected error occurred: " + ex.getMessage())
                            .type(MediaType.TEXT_PLAIN_TYPE)
                            .build();
                });
    }
}