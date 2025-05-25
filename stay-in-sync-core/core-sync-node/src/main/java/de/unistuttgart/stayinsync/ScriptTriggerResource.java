package de.unistuttgart.stayinsync;

import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.SyncJobFactory;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Path("/api/scripts")
public class ScriptTriggerResource {

    private static final Logger LOG = Logger.getLogger(ScriptTriggerResource.class);

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
    public CompletionStage<Response> triggerScriptExecutionJob() {
        LOG.info("Received request for /trigger-job");

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
                .thenApply(transformationResult -> {
                    if (transformationResult.isValidExecution()) {
                        LOG.infof("Async Transformation successful for job %s. Output: %s", jsonMockJob.jobId(), transformationResult.getOutputData());
                        return Response.ok(transformationResult.getOutputData())
                                .type(MediaType.APPLICATION_JSON_TYPE)
                                .build();
                    } else {
                        LOG.warnf("Async Transformation failed for job %s: %s", jsonMockJob.jobId(), transformationResult.getErrorInfo());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Transformation failed: " + transformationResult.getErrorInfo())
                                .type(MediaType.TEXT_PLAIN_TYPE)
                                .build();
                    }
                })
                .exceptionally(ex -> {
                    LOG.errorf(ex, "Exception during async transformation for job %s", jsonMockJob.jobId());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("An unexpected error occurred: " + ex.getMessage())
                            .type(MediaType.TEXT_PLAIN_TYPE)
                            .build();
                });
    }
}