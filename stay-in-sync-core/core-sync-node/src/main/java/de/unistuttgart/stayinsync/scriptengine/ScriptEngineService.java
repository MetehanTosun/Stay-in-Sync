package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.exception.ScriptEngineException;
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
        return Uni.createFrom().item(() -> {
            try {
                MDC.put("jobId", job.jobId());
                MDC.put("scriptId", job.scriptId());
                Log.infof("Starting async transformation of job: %s, script: %s", job.jobId(), job.scriptId());

                return transformInternal(job);
            } catch (ScriptEngineException e) {
                Log.warnf(e, "ScriptEngineException during async transformation for job %s, script %s. Title: %s",
                        job.jobId(), job.scriptId(), e.getTitle());
                return getResult(job, e);
            } catch (Exception e) {
                Log.errorf(e, "Unexpected generic exception during async transformation for job %s, script %s",
                        job.jobId(), job.scriptId());
                TransformationResult errorResult = new TransformationResult(job.jobId(), job.scriptId());
                errorResult.setValidExecution(false);
                errorResult.setErrorInfo("Unexpected error during transformation: " + e.getMessage());
                return errorResult;
            } finally {
                Log.infof("Finished async transformation (attempt) for job: %s, script: %s", job.jobId(), job.scriptId());
                MDC.clear();
            }
        }).runSubscriptionOn(managedExecutor);
    }

    private static TransformationResult getResult(TransformJob job, ScriptEngineException e) {
        TransformationResult errorResult = new TransformationResult(job.jobId(), job.scriptId());
        errorResult.setValidExecution(false);
        String errorMessage = String.format("[%s] %s (Details: %s)", e.getErrorType(), e.getTitle(), e.getMessage());
        if (e.getCause() != null) {
            errorMessage += " | Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage();
        }
        errorResult.setErrorInfo(errorMessage);
        return errorResult;
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
        // TODO: languageId hier dynamisch aus transformJob.scriptLanguage() holen und den Pool entsprechend anfordern

        ContextPool contextPool = contextPoolFactory.getPool("js");
        Context context = null;

        try {
            try {
                context = contextPool.borrowContext();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptEngineException(
                        ScriptEngineException.ErrorType.CONTEXT_POOL_ERROR,
                        "Context Borrow Interrupted",
                        "Thread interrupted while waiting to borrow context for job " + transformJob.jobId(),
                        e
                );
            }
            Log.debugf("Borrowed context for job %s (language: %s)", transformJob.jobId(), transformJob.scriptLanguage());

            Source source;
            try {
                if (!scriptCache.containsScript(transformJob.scriptId(), transformJob.expectedHash())) {
                    Log.infof("Script %s (hash: %s, lang: %s) not in cache. Compiling...",
                            transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptLanguage());
                    scriptCache.putScript(transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptCode());
                }
                source = scriptCache.getScript(transformJob.scriptId(), transformJob.expectedHash());
            } catch (ScriptEngineException e) {
                throw new ScriptEngineException(
                        e.getErrorType(),
                        e.getTitle(),
                        String.format("Caching/Parsing script %s for job %s failed: %s", transformJob.scriptId(), transformJob.jobId(), e.getMessage()),
                        e
                );
            }

            if (source == null) {
                String errorMsg = "Script source not found or could not be loaded for: " + transformJob.scriptId() + " (job " + transformJob.jobId() + ")";
                Log.error(errorMsg);
                throw new ScriptEngineException(
                        ScriptEngineException.ErrorType.SCRIPT_CACHING_ERROR,
                        "Script Not Found",
                        errorMsg
                );
            }

            ScriptApi scriptApi = new ScriptApi(transformJob.sourceData(), transformJob.jobId());
            try {
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
            } catch (Exception e) {
                String errorMsg = String.format("Failed to set up bindings for script %s (job %s, lang %s): %s",
                        transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage());
                Log.errorf(e, errorMsg);
                throw new ScriptEngineException(
                        ScriptEngineException.ErrorType.BINDING_ERROR,
                        "Script Binding Error",
                        errorMsg,
                        e
                );
            }

            context.eval(source);
            Object rawOutput = scriptApi.getOutputData();
            if (rawOutput == null) {
                Log.warnf("Script %s did not call %s.setOutput(). OutputData is null for job %s.", transformJob.scriptId(), SCRIPT_API_BINDING_NAME, transformJob.jobId());
            }

            result.setOutputData(extractResult(Value.asValue(rawOutput)));
            result.setValidExecution(true);
            Log.infof("Script %s executed successfully for job %s.", transformJob.scriptId(), transformJob.jobId());
            return result;

        } catch (PolyglotException e) {
            String errorDetails;
            String title = "Script Execution Error";
            if (e.isHostException()) {
                Throwable hostEx = e.asHostException();
                errorDetails = String.format("HostException: %s", hostEx.getMessage());
                Log.errorf(hostEx, "Script execution failed for %s (job %s, lang %s) due to HostException: %s",
                        transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), hostEx.getMessage());
            } else if (e.isGuestException()) {
                errorDetails = String.format("GuestException: %s. Source: %s", e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
                Log.errorf("Script execution failed for %s (job %s, lang %s) due to GuestException: %s. Source: %s",
                        transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
            } else if (e.isSyntaxError()) {
                title = "Script Syntax Error";
                errorDetails = String.format("SyntaxError: %s. Source: %s", e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
                Log.errorf("Script execution failed for %s (job %s, lang %s) due to SyntaxError: %s. Source: %s",
                        transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
            } else if (e.isResourceExhausted()) {
                title = "Resource Limit Exceeded";
                errorDetails = String.format("ResourceLimitExceeded: %s. Source: %s", e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
                Log.errorf("Script execution failed for %s (job %s, lang %s) due to ResourceLimitExceeded: %s. Source: %s",
                        transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
            }
            else {
                errorDetails = String.format("PolyglotException: %s", e.getMessage());
                Log.errorf(e, "Script execution failed for %s (job %s, lang %s) due to PolyglotException: %s",
                        transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), e.getMessage());
            }
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.SCRIPT_EXECUTION_ERROR,
                    title,
                    String.format("Script execution failed for %s (job %s, lang %s): %s",
                            transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage(), errorDetails),
                    e
            );
        } catch (ScriptEngineException e) {
            Log.errorf(e, "A ScriptEngineException occurred during transformation for job %s, script %s: %s",
                    transformJob.jobId(), transformJob.scriptId(), e.getMessage());
            throw e;
        }
        catch (Exception e) {
            String errorMsg = String.format("Unexpected error during transformation for job %s, script %s: %s",
                    transformJob.jobId(), transformJob.scriptId(), e.getMessage());
            Log.errorf(e, errorMsg);
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.SCRIPT_EXECUTION_ERROR,
                    "Unexpected Transformation Error",
                    errorMsg,
                    e
            );
        } finally {
            if (context != null) {
                try {
                    context.getBindings(transformJob.scriptLanguage()).removeMember(SCRIPT_API_BINDING_NAME);
                    if ("js".equalsIgnoreCase(transformJob.scriptLanguage()) && transformJob.sourceData() instanceof Map) {
                        Map<String, Object> namespacedData = (Map<String, Object>) transformJob.sourceData();
                        for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                            context.getBindings("js").removeMember(entry.getKey());
                        }
                    }
                } catch (Exception e) {
                    Log.errorf(e, "Error during context binding cleanup for job %s, script %s. Context might be in an inconsistent state when returned to pool.",
                            transformJob.jobId(), transformJob.scriptId());
                } finally {
                    contextPool.returnContext(context);
                    Log.debugf("Returned context for job %s", transformJob.jobId());
                }
            }
        }
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
        try {
            if (value.isProxyObject()) {
                String errorMsg = String.format("Unhandled GraalVM proxy object encountered during result extraction: %s", value);
                Log.warn(errorMsg);
                throw new ScriptEngineException(ScriptEngineException.ErrorType.RESULT_EXTRACTION_ERROR,
                        "Result Extraction Error",
                        errorMsg);
            }
        } catch (PolyglotException e) {
            String errorMsg = String.format("PolyglotException during result extraction of value '%s': %s", value, e.getMessage());
            Log.warnf(e, errorMsg);
            throw new ScriptEngineException(ScriptEngineException.ErrorType.RESULT_EXTRACTION_ERROR,
                    "Result Extraction Error",
                    errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error during result extraction of value '%s': %s", value, e.getMessage());
            Log.warnf(e, errorMsg);
            throw new ScriptEngineException(ScriptEngineException.ErrorType.RESULT_EXTRACTION_ERROR,
                    "Unexpected Result Extraction Error",
                    errorMsg, e);
        }
        if (value.isNull()) return null;
        if (value.isHostObject()) return value.asHostObject();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            if (value.fitsInDouble()) return value.asDouble();
            if (value.fitsInInt()) return value.asInt();
            return value.asDouble();
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
            for (String key : value.getMemberKeys()) {
                map.put(key, extractResult(value.getMember(key)));
            }
            return map;
        }
        String errorMsg = String.format("Unhandled GraalVM value type encountered during result extraction: MetaObject=%s, Value=%s", value.getMetaObject(), value);
        Log.warn(errorMsg);
        throw new ScriptEngineException(ScriptEngineException.ErrorType.RESULT_EXTRACTION_ERROR,
                "Result Extraction Error",
                errorMsg + ". This type is not explicitly handled.");
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
