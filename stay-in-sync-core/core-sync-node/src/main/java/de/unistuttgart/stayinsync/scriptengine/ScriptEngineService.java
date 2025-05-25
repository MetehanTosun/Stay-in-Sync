package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.scriptengine.message.ConditionResult;
import de.unistuttgart.stayinsync.scriptengine.message.IntegrityResult;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.scriptengine.message.ValidationResult;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import io.quarkus.runtime.Quarkus;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.spi.launcher.ExecutionContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.graalvm.polyglot.*;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Facade ScriptEngineService that provides easy and well-defined entrypoints to interact with the script-engine.
 * Receives Data and scripts in order to run a transformation and return the respective results in addition to Log
 * information.
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
@ApplicationScoped
public class ScriptEngineService {
    private static final Logger LOG = Logger.getLogger(ScriptEngineService.class);
    private static final String SCRIPT_API_BINDING_NAME = "stayinsync";

    private final ScriptCache scriptCache;
    private final ContextPoolFactory contextPoolFactory;
    private final ManagedExecutor managedExecutor;

    @Inject
    public ScriptEngineService(ScriptCache scriptCache, ContextPoolFactory contextPoolFactory, ManagedExecutor managedExecutor) {
        this.scriptCache = scriptCache;
        this.contextPoolFactory = contextPoolFactory;
        this.managedExecutor = managedExecutor;
    }

    public CompletionStage<TransformationResult> transformAsync(TransformJob job) {
        return CompletableFuture.supplyAsync(()-> {
            try{
                MDC.put("jobId", job.jobId());
                MDC.put("scriptId", job.scriptId());
                LOG.infof("Starting async transformation of job: %s, script: %s", job.jobId(), job.scriptId());
                return transformInternal(job);
            } finally {
                LOG.infof("finished async transformation of job: %s, script: %s", job.jobId(), job.scriptId());
                MDC.clear();
            }
        }, managedExecutor);
    }

    public static void main(String[] args){
        Quarkus.run(args);
    }

    // TODO: Handle proper validity setting of a job inside the VM
    // TODO: Assign proper input and output data formats
    private TransformationResult transformInternal(TransformJob transformJob) {
        TransformationResult result = new TransformationResult(transformJob.jobId(), transformJob.scriptId());
        ContextPool contextPool = contextPoolFactory.getPool("js"); // TODO: extension point for more languages
        Context context = null;

        try {
            context = contextPool.borrowContext();
            LOG.debugf("Borrowed context for job %s (language: %s)", transformJob.jobId(), transformJob.scriptLanguage());

            if (!scriptCache.containsScript(transformJob.scriptId(), transformJob.expectedHash())) { // TODO: add scriptLanguage
                LOG.infof("Script %s (hash: %s, lang: %s) not in cache. Compiling...",
                        transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptLanguage());
                scriptCache.putScript(transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptCode()); // TODO: add scriptLanguage
            }
            Source source = scriptCache.getScript(transformJob.scriptId(), transformJob.expectedHash());
            if (source == null) {
                result.setValidExecution(false);
                result.setErrorInfo("Script source not found in cache for: " + transformJob.scriptId() + " (lang: " + transformJob.scriptLanguage() + ")");
                LOG.error(result.getErrorInfo());
                return result;
            }

            ScriptApi scriptApi = new ScriptApi(transformJob.sourceData(), transformJob.jobId());
            context.getBindings(transformJob.scriptLanguage()).putMember(SCRIPT_API_BINDING_NAME, scriptApi);

            if ("js".equalsIgnoreCase(transformJob.scriptLanguage())) {
                if (transformJob.sourceData() instanceof Map) {
                    Map<String, Object> namespacedData = (Map<String, Object>) transformJob.sourceData();
                    for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                        context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                    }
                } else {
                    LOG.warnf("Input data for JS script %s is not a Map. It won't be directly available as global vars.", transformJob.scriptId());
                }
            }

            context.eval(source);
            Object rawOutput = scriptApi.getOutputData();
            if (rawOutput == null) {
                LOG.warnf("Script %s did not call %s.getOutput(). OutputData is null.", transformJob.scriptId(), SCRIPT_API_BINDING_NAME);
            }
            result.setOutputData(extractResult(Value.asValue(rawOutput)));
            result.setValidExecution(true);
            LOG.infof("Script %s executed successfully for job %s.", transformJob.scriptId(), transformJob.jobId());

        } catch (PolyglotException e) {
            result.setValidExecution(false);
            String errorMsg = String.format("Script execution failed for %s (job %s, lang %s): %s",
                    transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage());

            if (e.isHostException()) {
                Throwable hostEx = e.asHostException();
                LOG.errorf(hostEx, "%s - HostException: %s", errorMsg, hostEx.getMessage());
                result.setErrorInfo(errorMsg + " - HostException: " + hostEx.getMessage());
            } else if (e.isGuestException()) {
                LOG.errorf("%s - GuestException: %s. Source: %s", errorMsg, e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
                result.setErrorInfo(errorMsg + " - GuestException: " + e.getMessage());
            } else {
                LOG.errorf(e, errorMsg);
                result.setErrorInfo(errorMsg);
            }
        } catch (InterruptedException e) {
            result.setValidExecution(false);
            result.setErrorInfo("Thread interrupted while processing job " + transformJob.jobId() + ": " + e.getMessage());
            LOG.warnf("Thread interrupted for job %s", transformJob.jobId());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            result.setValidExecution(false);
            result.setErrorInfo("Unexpected error during transformation for job " + transformJob.jobId() + ": " + e.getMessage());
            LOG.errorf(e, "Unexpected error for job %s", transformJob.jobId());
        } finally {
            // TODO: handle scriptApi still being bound to context / potentially cleanup / packagemanager?
            if (context != null) {
                context.getBindings(transformJob.scriptLanguage()).removeMember(SCRIPT_API_BINDING_NAME);
                if("js".equalsIgnoreCase(transformJob.scriptLanguage()) && transformJob.sourceData() instanceof Map) {
                    Map<String, Object> namespacedData = (Map<String, Object>) transformJob.sourceData();
                    for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                        context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                    }
                }
                contextPool.returnContext(context);
                LOG.debugf("Returned context for job %s", transformJob.jobId());
            }
        }
        return result;
    }

    private Object extractResult(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isHostObject()) return value.asHostObject();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            if (value.fitsInDouble()) return value.asDouble();
            return value.asInt();
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(extractResult(value.getArrayElement(i)));
            }
            return list;
        }

        // TODO: Recursive Object conversion might be simpler with a library
        if (value.hasMembers()) {
            Map<String, Object> map = new HashMap<>();
            value.getMemberKeys().forEach(key -> map.put(key, extractResult(value.getMember(key))));
            return map;
        }
        // TODO: Evaluate other possible return types like functions and promises
        LOG.warnf("Unhandled GraalVM value type encountered during result extraction: %s", value);
        // TODO: Define proper Exception state and logger information handling
        return value.toString(); // fallback
    }

    ConditionResult evaluateCondition(String scriptId, String scriptContext, Object inputData, ExecutionContext executionContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    ValidationResult validateSyntax(String scriptContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    IntegrityResult validateDataSource(Object inputData) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
