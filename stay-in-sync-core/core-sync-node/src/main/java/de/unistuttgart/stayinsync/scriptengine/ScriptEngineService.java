package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.scriptengine.message.ConditionResult;
import de.unistuttgart.stayinsync.scriptengine.message.IntegrityResult;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.scriptengine.message.ValidationResult;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.graalvm.polyglot.*;
import org.jboss.logging.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A facade service providing well-defined entry points to interact with the script engine.
 * This service is responsible for orchestrating script execution, including fetching scripts
 * from a cache, acquiring script execution contexts from a pool, running the scripts with
 * provided input data, and returning the transformation results.
 *
 * <p>The primary way to use this service is via the {@link #transformAsync(TransformJob)} method,
 * which executes script transformations asynchronously.</p>
 *
 * <p>It leverages a {@link ScriptCache} for efficient script parsing and a {@link ContextPoolFactory}
 * to manage {@link Context} instances for script execution. Asynchronous operations are handled
 * by a {@link ManagedExecutor}.</p>
 *
 * <p>The {@code main} method included is for development or standalone testing purposes of the
 * Quarkus application and is not part of the service's typical API usage.</p>
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
@ApplicationScoped
public class ScriptEngineService {
    /**
     * The name under which the {@link ScriptApi} instance is bound and made available
     * to the executed scripts. Scripts can access the API using this name (e.g., {@code stayinsync.log("message")}).
     */
    private static final String SCRIPT_API_BINDING_NAME = "stayinsync";

    private final ScriptCache scriptCache;
    private final ContextPoolFactory contextPoolFactory;
    private final ManagedExecutor managedExecutor;

    /**
     * Constructs a new {@code ScriptEngineService} with injected dependencies.
     * This constructor is typically invoked by the CDI container.
     *
     * @param scriptCache        The cache for storing and retrieving pre-parsed scripts.
     * @param contextPoolFactory The factory for obtaining pools of script execution contexts.
     * @param managedExecutor    The executor service for running asynchronous tasks, managed by the MicroProfile Context Propagation.
     */
    @Inject
    public ScriptEngineService(ScriptCache scriptCache, ContextPoolFactory contextPoolFactory, ManagedExecutor managedExecutor) {
        this.scriptCache = scriptCache;
        this.contextPoolFactory = contextPoolFactory;
        this.managedExecutor = managedExecutor;
    }

    /**
     * Asynchronously executes a script transformation based on the provided {@link TransformJob}.
     * The job contains the script code, its identifier, expected hash, input data, and language.
     * The transformation is performed on a separate thread managed by the {@link #managedExecutor}.
     *
     * <p>MDC (Mapped Diagnostic Context) is used to enrich logs with {@code jobId} and {@code scriptId}
     * for the duration of the asynchronous task.</p>
     *
     * @param job The {@link TransformJob} defining the transformation to be executed.
     * @return A {@link Uni} that will emit a {@link TransformationResult}
     * containing the output of the script execution, its validity, and any error information.
     */
    public Uni<TransformationResult> transformAsync(TransformJob job) {
        return Uni.createFrom().item(() ->{
            try {
                MDC.put("jobId", job.jobId());
                MDC.put("scriptId", job.scriptId());
                Log.infof("Starting async transformation of job: %s, script: %s", job.jobId(), job.scriptId());
                return transformInternal(job);
            } finally {
                Log.infof("finished async transformation of job: %s, script: %s", job.jobId(), job.scriptId());
                MDC.clear();
            }
        }).runSubscriptionOn(managedExecutor);
    }

    /**
     * Main method for running the Quarkus application.
     * This is primarily intended for development, testing, or standalone execution
     * and is not the typical way to interact with this service's API.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        Quarkus.run(args);
    }

    /**
     * Internal synchronous method to perform the script transformation.
     * This method handles context borrowing, script caching, API binding, script evaluation,
     * and result extraction.
     *
     * <p>TODO: Handle proper validity setting of a job inside the VM.</p>
     * <p>TODO: Assign proper input and output data formats beyond current Map/basic type handling.</p>
     * <p>TODO: The language is currently hardcoded to "js" for context pool retrieval. This is an extension point.</p>
     *
     * @param transformJob The {@link TransformJob} to process.
     * @return A {@link TransformationResult} encapsulating the outcome of the transformation.
     */
    private TransformationResult transformInternal(TransformJob transformJob) {
        TransformationResult result = new TransformationResult(transformJob.jobId(), transformJob.scriptId());
        ContextPool contextPool = contextPoolFactory.getPool("js"); // TODO: extension point for more languages
        Context context = null;

        try {
            context = contextPool.borrowContext();
            Log.debugf("Borrowed context for job %s (language: %s)", transformJob.jobId(), transformJob.scriptLanguage());

            if (!scriptCache.containsScript(transformJob.scriptId(), transformJob.expectedHash())) { // TODO: add scriptLanguage
                Log.infof("Script %s (hash: %s, lang: %s) not in cache. Compiling...",
                        transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptLanguage());
                scriptCache.putScript(transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptCode()); // TODO: add scriptLanguage
            }
            Source source = scriptCache.getScript(transformJob.scriptId(), transformJob.expectedHash());
            if (source == null) {
                result.setValidExecution(false);
                result.setErrorInfo("Script source not found in cache for: " + transformJob.scriptId() + " (lang: " + transformJob.scriptLanguage() + ")");
                Log.error(result.getErrorInfo());
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
                    Log.warnf("Input data for JS script %s is not a Map. It won't be directly available as global vars.", transformJob.scriptId());
                }
            }

            context.eval(source);
            Object rawOutput = scriptApi.getOutputData();
            if (rawOutput == null) {
                Log.warnf("Script %s did not call %s.getOutput(). OutputData is null.", transformJob.scriptId(), SCRIPT_API_BINDING_NAME);
            }
            result.setOutputData(extractResult(Value.asValue(rawOutput)));
            result.setValidExecution(true);
            Log.infof("Script %s executed successfully for job %s.", transformJob.scriptId(), transformJob.jobId());

        } catch (PolyglotException e) {
            result.setValidExecution(false);
            String errorMsg = String.format("Script execution failed for %s (job %s, lang %s): %s",
                    transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage());

            if (e.isHostException()) {
                Throwable hostEx = e.asHostException();
                Log.errorf(hostEx, "%s - HostException: %s", errorMsg, hostEx.getMessage());
                result.setErrorInfo(errorMsg + " - HostException: " + hostEx.getMessage());
            } else if (e.isGuestException()) {
                Log.errorf("%s - GuestException: %s. Source: %s", errorMsg, e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
                result.setErrorInfo(errorMsg + " - GuestException: " + e.getMessage());
            } else {
                Log.errorf(e, errorMsg);
                result.setErrorInfo(errorMsg);
            }
        } catch (InterruptedException e) {
            result.setValidExecution(false);
            result.setErrorInfo("Thread interrupted while processing job " + transformJob.jobId() + ": " + e.getMessage());
            Log.warnf("Thread interrupted for job %s", transformJob.jobId());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            result.setValidExecution(false);
            result.setErrorInfo("Unexpected error during transformation for job " + transformJob.jobId() + ": " + e.getMessage());
            Log.errorf(e, "Unexpected error for job %s", transformJob.jobId());
        } finally {
            // TODO: handle scriptApi still being bound to context / potentially cleanup / packagemanager?
            if (context != null) {
                context.getBindings(transformJob.scriptLanguage()).removeMember(SCRIPT_API_BINDING_NAME);
                if ("js".equalsIgnoreCase(transformJob.scriptLanguage()) && transformJob.sourceData() instanceof Map) {
                    Map<String, Object> namespacedData = (Map<String, Object>) transformJob.sourceData();
                    for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                        context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                    }
                }
                contextPool.returnContext(context);
                Log.debugf("Returned context for job %s", transformJob.jobId());
            }
        }
        return result;
    }

    /**
     * Extracts a Java object representation from a GraalVM {@link Value}.
     * This method recursively converts GraalVM Values that represent arrays or objects (maps)
     * into Java Lists and Maps, respectively. Primitive types are converted to their Java equivalents.
     *
     * <p>TODO: Recursive Object conversion might be simpler or more robust with a dedicated library or more comprehensive handling.</p>
     * <p>TODO: Evaluate other possible return types like functions and promises if they need to be handled.</p>
     *
     * @param value The GraalVM {@link Value} to convert.
     * @return The corresponding Java object, or a string representation as a fallback for unhandled types.
     * Returns {@code null} if the input value is {@code null} or represents a GraalVM null.
     */
    private Object extractResult(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isHostObject()) return value.asHostObject();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            if (value.fitsInDouble()) return value.asDouble();
            if (value.fitsInInt()) return value.asInt();
            return value.asDouble(); // Fallback for numbers that don't fit long, could be a large float or int.
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(extractResult(value.getArrayElement(i)));
            }
            return list;
        }

        if (value.hasMembers()) {
            Map<String, Object> map = new HashMap<>();
            value.getMemberKeys().forEach(key -> map.put(key, extractResult(value.getMember(key))));
            return map;
        }
        Log.warnf("Unhandled GraalVM value type encountered during result extraction: %s", value);
        return value.toString(); // fallback in case that no fitting type conversions were found.
    }

    /**
     * Evaluates a condition script.
     * <strong>NOTE: This method is not yet implemented.</strong>
     *
     * @param scriptId      The ID of the script.
     * @param scriptContext The script code or content.
     * @param inputData     The input data for the script.
     * @return A {@link ConditionResult}.
     * @throws UnsupportedOperationException always, as this method is not implemented.
     */
    ConditionResult evaluateCondition(String scriptId, String scriptContext, Object inputData) {
        // TODO: Implement condition evaluation logic.
        throw new UnsupportedOperationException("Condition evaluation is not supported yet.");
    }

    /**
     * Validates the syntax of a script.
     * <strong>NOTE: This method is not yet implemented.</strong>
     *
     * @param scriptContext The script code or content to validate.
     * @return A {@link ValidationResult}.
     * @throws UnsupportedOperationException always, as this method is not implemented.
     */
    ValidationResult validateSyntax(String scriptContext) {
        // TODO: Implement script syntax validation logic.
        // This could involve attempting to parse the script without full execution.
        throw new UnsupportedOperationException("Syntax validation is not supported yet.");
    }

    /**
     * Validates a data source.
     * <strong>NOTE: This method is not yet implemented.</strong>
     *
     * @param inputData The input data to validate.
     * @return An {@link IntegrityResult}.
     * @throws UnsupportedOperationException always, as this method is not implemented.
     */
    IntegrityResult validateDataSource(Object inputData) {
        // TODO: Implement data source integrity validation logic.
        throw new UnsupportedOperationException("Data source validation is not supported yet.");
    }
}
