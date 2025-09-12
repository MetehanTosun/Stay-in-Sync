package de.unistuttgart.stayinsync.core.scriptengine;

import de.unistuttgart.stayinsync.core.exception.ScriptEngineException;
import io.quarkus.logging.Log;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages a pool of pre-initialized GraalVM {@link Context} objects for a specific scripting language.
 * This pooling mechanism helps to reduce the overhead of creating new contexts for each script execution,
 * especially in scenarios where scripts are executed frequently.
 *
 * <p>The pool provides methods to borrow a context and return it once the execution is complete.
 * It also handles the configuration of contexts, including host access permissions and resource limits.</p>
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
public class ContextPool {
    /**
     * Defines the host access permissions for scripts executed within the contexts from this pool.
     * It allows access to Java methods and fields annotated with {@link HostAccess.Export},
     * allows Java types to be implemented by script functions if annotated with {@link HostAccess.Implementable},
     * and permits access to Map and List structures from the host.
     */
    private static final HostAccess SCRIPT_API_ACCESS = HostAccess.newBuilder()
            .allowAccessAnnotatedBy(HostAccess.Export.class)
            .allowImplementationsAnnotatedBy(HostAccess.Implementable.class)
            .allowMapAccess(true)
            .allowListAccess(true)
            .build();

    /**
     * The underlying blocking queue that holds the available {@link Context} instances.
     * Using a {@link LinkedBlockingQueue} ensures thread-safe access to the pool.
     */
    private final BlockingQueue<Context> pool;

    /**
     * The synchronized List that tracks all created Contexts in order to keep household of
     * creations and closings.
     */
    private final List<Context> allCreatedContexts;
    private final int poolSize;
    private final String languageId;
    private volatile boolean closed = false;

    /**
     * Constructs a new {@code ContextPool} for the specified language and size.
     * The pool is immediately initialized with {@code size} number of {@link Context} instances.
     *
     * @param languageId The identifier of the scripting language (e.g., "js").
     *                   While this parameter is stored, the current implementation of {@link #initializePool()}
     *                   hardcodes "js" for context creation.
     * @param size       The maximum number of contexts to be maintained in this pool. Must be at least 1
     * @throws ScriptEngineException if the size is less than or equal to 0.
     */
    public ContextPool(String languageId, int size) throws ScriptEngineException {
        if (size <= 0) {
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.CONFIGURATION_ERROR,
                    "Wrong Context Pool size",
                    "ContextPool size must be positive but was: " + size);
        }

        this.languageId = languageId;
        this.poolSize = size;
        this.pool = new LinkedBlockingQueue<>(size);
        this.allCreatedContexts = Collections.synchronizedList(new ArrayList<>());
        initializePool();
    }

    /**
     * Borrows a {@link Context} from the pool.
     * This method will wait for up to 10 seconds for a context to become available.
     *
     * @return An available {@link Context} instance from the pool.
     * @throws ScriptEngineException if the ContextPool is closed or was closed during borrowing,
     *                               or if the thread is interrupted while waiting for a context,
     *                               or if a timeout occurs (10 seconds hardcoded, might have
     *                               to be a config value) while waiting for an available context.
     */
    public Context borrowContext() throws ScriptEngineException, InterruptedException {
        if (closed) {
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.CONTEXT_POOL_ERROR,
                    "Context Pool Closed",
                    "ContextPool for language " + languageId + " is closed and cannot provide new contexts."
            );
        }
        Log.debugf("Attempting to borrow context for language: %s. Available: %d/%d", languageId, pool.size(), poolSize);
        Context context = pool.poll(10, TimeUnit.SECONDS);
        if (context == null) {
            if (closed) {
                throw new ScriptEngineException(
                        ScriptEngineException.ErrorType.CONTEXT_POOL_ERROR,
                        "Context Pool Closed",
                        "ContextPool for language " + languageId + " was closed while waiting to borrow a context."
                );
            }
            String errorMsg = String.format("Timeout borrowing context for language %s. Pool size: %d, Available: %d", languageId, poolSize, pool.size());
            Log.warnf(errorMsg);
            throw new ScriptEngineException(
                    ScriptEngineException.ErrorType.CONTEXT_POOL_ERROR,
                    "Context Borrow Timeout",
                    errorMsg,
                    new InterruptedException("Timeout borrowing context from pool")
            );
        }
        Log.debugf("Borrowed context for language: %s. Available: %d/%d", languageId, pool.size(), poolSize);
        return context;
    }

    /**
     * Returns a {@link Context} to the pool.
     * null contexts are simply returned as false, since nothing is to be added to the pool.
     * If the pool is closed, the context itself will be closed safely.
     * If a context is to be returned from a different Pool, the returned context will be closed.
     *
     * @param context The {@link Context} instance to return to the pool.
     * @return {@code true} if the context was successfully returned to the pool, {@code false} otherwise
     * (e.g., if the context was null or the pool was full, leading to the context being closed).
     */
    public boolean returnContext(Context context) {
        if (context == null) return false;

        if (closed) {
            Log.warnf("Context for language %s returned to an already closed pool. Closing context directly.", languageId);
            closeSafely(context);
            return false;
        }

        if (!allCreatedContexts.contains(context)) {
            Log.warnf("Attempt to return a context to pool '%s' that did not originate from it. Closing context.", languageId);
            closeSafely(context);
            return false;
        }

        if (pool.offer(context)) {
            Log.debugf("Returned context for language: %s. Available: %d/%d", languageId, pool.size(), poolSize);
            return true;
        } else {
            Log.warnf("Context for language %s could not be returned to pool (possibly full or offer failed). Closing context instead. Pool available: %d/%d",
                    languageId, pool.size(), poolSize);
            closeSafely(context);
            return false;
        }
    }

    private void closeSafely(Context context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                Log.errorf(e, "Error closing context that could not be returned to pool for language %s", languageId);
            }
        }
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getAvailableCount() {
        return pool.size();
    }

    public String getLanguageId() {
        return languageId;
    }

    /**
     * Initializes the pool by creating and configuring {@link Context} instances.
     * Each context is configured with specific host access permissions ({@link #SCRIPT_API_ACCESS})
     * and resource limits (e.g., statement execution limit).
     *
     * <p><b>Note:</b> Currently, this method hardcodes the language to "js" when creating new contexts
     * ({@code Context.newBuilder("js")}), regardless of the {@code languageId} field.
     * The {@code TODO} comments indicate planned features for module loading, specifically for JavaScript.</p>
     *
     * <p>If context creation fails, an error is logged, and the pool might contain fewer contexts
     * than its configured {@code poolSize}. The {@code TODO} comment suggests further discussion
     * on how to handle such failures, particularly regarding application startup.</p>
     *
     * @throws ScriptEngineException if a specific context could not be created because of configuration errors.
     */
    private void initializePool() throws ScriptEngineException {
        for (int i = 0; i < poolSize; i++) {
            try {
                Context.Builder builder = Context.newBuilder("js")
                        .allowAllAccess(false)
                        .allowHostAccess(SCRIPT_API_ACCESS)
                        .resourceLimits(ResourceLimits.newBuilder()
                                .statementLimit(100_000L, null)
                                .build());
                /*
                Todo: Preparation for module loading inside script environment
                if("js".equals(languageId)) {
                    builder.option("js.commonjs-require", "true");
                    builder.option("js.commonjs-require-cwd", "PATH TO MODULE MANAGEMENT");
                }
                */
                Context newContext = builder.build();
                allCreatedContexts.add(newContext);
                pool.add(newContext);
            } catch (Exception e) {
                String errorMsg = String.format("Failed to create GraalVM context #%d for language %s during pool initialization.", (i + 1), languageId);
                Log.errorf(e, errorMsg);
                throw new ScriptEngineException(
                        ScriptEngineException.ErrorType.CONFIGURATION_ERROR,
                        "Context Initialization Failed",
                        errorMsg,
                        e
                );
            }
        }
    }

    /**
     * Closes all {@link Context} instances currently held in the pool.
     * This method should be called during application shutdown or when the pool is no longer needed
     * to release resources associated with the contexts.
     * Any errors encountered during context closure are logged as warnings.
     */
    public void closeAllContexts() {
        Log.infof("Closing all contexts in pool for language '%s'", languageId);
        this.closed = true;

        Context contextFromQueue;
        while ((contextFromQueue = pool.poll()) != null) {
            closeSafely(contextFromQueue);
            allCreatedContexts.remove(contextFromQueue);
        }

        synchronized (allCreatedContexts) {
            for (Context ctx : new ArrayList<>(allCreatedContexts)) {
                closeSafely(ctx);
            }
            allCreatedContexts.clear();
        }
        Log.infof("ContextPool for language '%s' closed. All contexts processed.", languageId);
    }
}
