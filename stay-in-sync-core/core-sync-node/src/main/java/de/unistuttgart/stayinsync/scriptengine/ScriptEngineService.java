package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.exception.ScriptEngineException;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
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
    private static final String JAVASCRIPT_LANGUAGE_ID = "js";

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
                Log.errorf(e, "ScriptEngineException during async transformation for job %s, script %s: %s",
                        job.jobId(), job.scriptId(), e.getMessage());
                TransformationResult errorResult = new TransformationResult(job.jobId(), job.scriptId());
                errorResult.setValidExecution(false);
                errorResult.setErrorInfo(e.getTitle() + ": " + e.getMessage());
                return errorResult;
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
     *
     * @param transformJob The {@link TransformJob} to process.
     * @return A {@link TransformationResult} encapsulating the outcome of the transformation.
     * @throws ScriptEngineException for various error states that can appear when calling this method {@link ScriptEngineException}
     */
    private TransformationResult transformInternal(TransformJob transformJob) throws ScriptEngineException {
        TransformationResult result = new TransformationResult(transformJob.jobId(), transformJob.scriptId());
        String scriptLanguage = transformJob.scriptLanguage();

        ContextPool contextPool = contextPoolFactory.getPool(scriptLanguage);
        Context context = null;

        try {
            context = contextPool.borrowContext();
            Log.debugf("Borrowed context for job %s (language: %s)", transformJob.jobId(), transformJob.scriptLanguage());

            Source source = getCachedScriptSource(transformJob);
            ScriptApi scriptApi = new ScriptApi(transformJob.sourceData(), transformJob.jobId());

            setupBindings(context, transformJob, scriptApi);

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
            throw handlePolyglotException(e, transformJob);
        } catch (ScriptEngineException e) {
            throw e;
        } catch (Exception e) {
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
                    cleanupBindings(context, transformJob);
                } finally {
                    contextPool.returnContext(context);
                    Log.debugf("Returned context for job %s", transformJob.jobId());
                }
            }
        }
    }

    /**
     * Retrieves a pre-parsed {@link Source} object for the script specified in the {@link TransformJob}.
     * <p>
     * This method first checks the {@link ScriptCache} for an existing, matching script (by ID and hash).
     * If the script is not found or the hash doesn't match, it compiles the script code from the
     * {@code transformJob} and stores it in the cache before returning it.
     * If the script cannot be loaded or retrieved from the cache even after an attempt to compile,
     * a {@link ScriptEngineException} is thrown.
     * </p>
     *
     * @param transformJob The job containing script details (ID, hash, code, language).
     *                     The {@code scriptId} and {@code expectedHash} are used for cache lookups.
     *                     The {@code scriptCode} is used for compilation if the script is not cached.
     *                     The {@code scriptLanguage} is used for logging purposes.
     * @return The {@link Source} object ready for evaluation.
     * @throws ScriptEngineException if the script cannot be found in or loaded into the cache.
     *                               This typically occurs if {@link ScriptCache#getScript(String, String)}
     *                               returns null after an attempt to {@link ScriptCache#putScript(String, String, String)}.
     */
    private Source getCachedScriptSource(TransformJob transformJob) throws ScriptEngineException {
        if (!scriptCache.containsScript(transformJob.scriptId(), transformJob.expectedHash())) {
            Log.infof("Script %s (hash: %s, lang: %s) not in cache. Compiling...",
                    transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptLanguage());
            scriptCache.putScript(transformJob.scriptId(), transformJob.expectedHash(), transformJob.scriptCode());
        }
        Source source = scriptCache.getScript(transformJob.scriptId(), transformJob.expectedHash());

        if (source == null) {
            String errorMsg = String.format("Script source not found or could not be loaded for: %s (job %s, lang %s)",
                    transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage());
            Log.error(errorMsg);
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.SCRIPT_CACHING_ERROR,
                    "Script Not Found",
                    errorMsg
            );
        }
        return source;
    }

    /**
     * Sets up the necessary bindings within the GraalVM {@link Context} before script execution.
     * <p>
     * This includes:
     * <ul>
     *   <li>Binding the {@link ScriptApi} instance under the name {@link #SCRIPT_API_BINDING_NAME}.</li>
     *   <li>For JavaScript (identified by {@link #JAVASCRIPT_LANGUAGE_ID}) scripts, if the {@code transformJob.sourceData()}
     *       is a {@link Map}, its entries are exposed as global variables in the script's scope.
     *       A warning is logged if source data is present but not a map for JavaScript.</li>
     * </ul>
     * </p>
     *
     * @param context The GraalVM {@link Context} to configure with bindings.
     * @param transformJob The job containing script language, source data, and identifiers for logging.
     *                     The {@code scriptLanguage} determines binding behavior (e.g., for JavaScript).
     *                     The {@code sourceData} may be exposed to the script.
     *                     The {@code scriptId} and {@code jobId} are used in log messages.
     * @param scriptApi The {@link ScriptApi} instance to make available to the script.
     * @throws ScriptEngineException if any error occurs during the binding process (e.g., issues with {@code putMember}).
     */
    private void setupBindings(Context context, TransformJob transformJob, ScriptApi scriptApi) throws ScriptEngineException {
        String scriptLanguage = transformJob.scriptLanguage();
        try {
            context.getBindings(scriptLanguage).putMember(SCRIPT_API_BINDING_NAME, scriptApi);

            // TODO: Assign proper input and output data formats beyond current Map/basic type handling.
            // This is language-specific for JavaScript
            if (JAVASCRIPT_LANGUAGE_ID.equalsIgnoreCase(scriptLanguage)) {
                if (transformJob.sourceData() instanceof Map) {
                    Map<String, Object> namespacedData = (Map<String, Object>) transformJob.sourceData();
                    for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                        context.getBindings(JAVASCRIPT_LANGUAGE_ID).putMember(entry.getKey(), entry.getValue());
                    }
                } else if (transformJob.sourceData() != null) {
                    Log.warnf("Input data for JS script %s is not a Map. It won't be directly available as global vars. Data type: %s",
                            transformJob.scriptId(), transformJob.sourceData().getClass().getName());
                }
            }
        } catch (Exception e) {
            String errorMsg = String.format("Failed to set up bindings for script %s (job %s, lang %s): %s",
                    transformJob.scriptId(), transformJob.jobId(), scriptLanguage, e.getMessage());
            Log.errorf(e, errorMsg);
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.BINDING_ERROR,
                    "Script Binding Error",
                    errorMsg,
                    e
            );
        }
    }

    /**
     * Cleans up bindings from the GraalVM {@link Context} after script execution.
     * <p>
     * This method is typically called in a {@code finally} block to ensure that the context is
     * reset before being returned to a pool. It attempts to remove:
     * <ul>
     *   <li>The {@link ScriptApi} binding ({@link #SCRIPT_API_BINDING_NAME}).</li>
     *   <li>For JavaScript (identified by {@link #JAVASCRIPT_LANGUAGE_ID}) scripts, if the {@code transformJob.sourceData()}
     *       was a {@link Map}, it removes the global variables that were previously added from this map.
     *       It checks for member existence before attempting removal to prevent errors.</li>
     * </ul>
     * Any exceptions encountered during cleanup are logged but not rethrown, to avoid masking
     * an original exception that might have occurred during script execution.
     * </p>
     *
     * @param context The GraalVM {@link Context} from which to remove bindings.
     * @param transformJob The job containing script language and source data details, used to determine
     *                     which bindings to clean up. The {@code scriptLanguage} and type of {@code sourceData}
     *                     guide the cleanup process. {@code scriptId} and {@code jobId} are for logging.
     */
    private void cleanupBindings(Context context, TransformJob transformJob) {
        String scriptLanguage = transformJob.scriptLanguage();
        try {
            context.getBindings(scriptLanguage).removeMember(SCRIPT_API_BINDING_NAME);

            if (JAVASCRIPT_LANGUAGE_ID.equalsIgnoreCase(scriptLanguage) && transformJob.sourceData() instanceof Map) {
                Map<String, Object> namespacedData = (Map<String, Object>) transformJob.sourceData();
                for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                    if (context.getBindings(JAVASCRIPT_LANGUAGE_ID).hasMember(entry.getKey())) {
                        context.getBindings(JAVASCRIPT_LANGUAGE_ID).removeMember(entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            Log.errorf(e, "Error during context binding cleanup for job %s, script %s. Context might be in an inconsistent state.",
                    transformJob.jobId(), transformJob.scriptId());
        }
    }

    /**
     * Handles a {@link PolyglotException} thrown during script execution, converting it
     * into a {@link ScriptEngineException} with more specific details and logging.
     * <p>
     * It inspects the nature of the {@code PolyglotException} (e.g., host exception, guest exception,
     * syntax error, resource exhaustion) to set an appropriate title and {@link ScriptEngineException.ErrorType}.
     * Detailed information, including source location if available from the {@code PolyglotException},
     * is logged and included in the returned exception's message.
     * </p>
     *
     * @param e The {@link PolyglotException} that occurred during script execution or interaction.
     * @param transformJob The job context (ID, script ID, language) used for enriching log messages
     *                     and the resulting {@link ScriptEngineException}.
     * @return A new {@link ScriptEngineException} wrapping the original {@code PolyglotException}
     *         with processed error information (type, title, detailed message).
     */
    private ScriptEngineException handlePolyglotException(PolyglotException e, TransformJob transformJob) {
        String errorDetails;
        String title;
        ScriptEngineException.ErrorType errorType = ScriptEngineException.ErrorType.SCRIPT_EXECUTION_ERROR;

        String scriptContextInfo = String.format("script %s (job %s, lang %s)",
                transformJob.scriptId(), transformJob.jobId(), transformJob.scriptLanguage());

        if (e.isHostException()) {
            Throwable hostEx = e.asHostException();
            title = "Script Host Exception";
            errorDetails = String.format("HostException: %s", hostEx.getMessage());
            Log.errorf(hostEx, "Execution failed for %s due to HostException: %s", scriptContextInfo, hostEx.getMessage());
        } else if (e.isGuestException()) {
            title = "Script Guest Exception";
            errorDetails = String.format("GuestException: %s. Source: %s", e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
            Log.errorf("Execution failed for %s due to GuestException: %s. Source: %s",
                    scriptContextInfo, e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
        } else if (e.isSyntaxError()) {
            title = "Script Syntax Error";
            errorType = ScriptEngineException.ErrorType.SYNTAX_ERROR;
            errorDetails = String.format("SyntaxError: %s. Source: %s", e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
            Log.errorf("Execution failed for %s due to SyntaxError: %s. Source: %s",
                    scriptContextInfo, e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
        } else if (e.isResourceExhausted()) {
            title = "Resource Limit Exceeded";
            errorType = ScriptEngineException.ErrorType.RESOURCE_LIMIT_EXCEEDED;
            errorDetails = String.format("ResourceLimitExceeded: %s. Source: %s", e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
            Log.errorf("Execution failed for %s due to ResourceLimitExceeded: %s. Source: %s",
                    scriptContextInfo, e.getMessage(), e.getSourceLocation() != null ? e.getSourceLocation().toString() : "N/A");
        } else {
            title = "Polyglot Execution Error";
            errorDetails = String.format("PolyglotException: %s", e.getMessage());
            Log.errorf(e, "Execution failed for %s due to PolyglotException: %s", scriptContextInfo, e.getMessage());
        }

        String fullErrorMessage = String.format("Script execution failed for %s: %s", scriptContextInfo, errorDetails);
        return new ScriptEngineException(errorType, title, fullErrorMessage, e);
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
    private Object extractResult(Value value) throws ScriptEngineException {
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
}
