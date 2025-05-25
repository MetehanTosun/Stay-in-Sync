package de.unistuttgart.stayinsync.scriptengine;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.jboss.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(ContextPool.class);

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
    private final int poolSize;
    private final String languageId;

    /**
     * Constructs a new {@code ContextPool} for the specified language and size.
     * The pool is immediately initialized with {@code size} number of {@link Context} instances.
     *
     * @param languageId The identifier of the scripting language (e.g., "js").
     *                   While this parameter is stored, the current implementation of {@link #initializePool()}
     *                   hardcodes "js" for context creation.
     * @param size The maximum number of contexts to be maintained in this pool.
     */
    public ContextPool(String languageId, int size) {
        this.languageId = languageId;
        this.poolSize = size;
        this.pool = new LinkedBlockingQueue<>(size);
        initializePool();
    }

    /**
     * Borrows a {@link Context} from the pool.
     * This method will wait for up to 10 seconds for a context to become available.
     *
     * @return An available {@link Context} instance from the pool.
     * @throws InterruptedException if the thread is interrupted while waiting for a context,
     *                              or if a timeout occurs (10 seconds hardcoded, might have to be a config value) while waiting for an available context.
     */
    public Context borrowContext() throws InterruptedException {
        LOG.debugf("Attempting to borrow context for language: %s. Available: %d/%d", languageId, pool.size(), poolSize);
        Context context = pool.poll(10, TimeUnit.SECONDS);
        if (context == null) {
            LOG.warnf("Timeout while waiting for a JavaScript context to borrow from the pool.");
            throw new InterruptedException("Timeout borrowing context for JavaScript");
        }
        LOG.debugf("Borrowed context for language: %s. Available: %d/%d", languageId, pool.size() +1, poolSize);
        return context;
    }

    /**
     * Returns a {@link Context} to the pool.
     * If the context cannot be added to the pool (e.g., if the pool is unexpectedly full or the context is null),
     * the context will be closed.
     *
     * @param context The {@link Context} instance to return to the pool.
     * @return {@code true} if the context was successfully returned to the pool, {@code false} otherwise
     *         (e.g., if the context was null or the pool was full, leading to the context being closed).
     */
    public boolean returnContext(Context context) {
        if (context != null && pool.offer(context)) {
            LOG.debugf("Returned context for language: %s. Available: %d/%d", languageId, pool.size(), poolSize);
            return true;
        } else if(context != null) {
            LOG.warnf("Context for language %s could not be returned to pool (possibly full). Closing context instead. Pool available: %d/%d",
                    languageId, pool.size(), poolSize);
            try {
                context.close();
            } catch (Exception e) {
                LOG.errorf(e, "Error closing context that could not be returned to pool for language %s", languageId);
            }
        }
        return false;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getAvailableCount(){
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
     */
    private void initializePool() {
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
                pool.add(builder.build());
            } catch (Exception e) {
                // TODO: Test and discussion about validity of sync node and if program may even start.
                LOG.errorf(e, "Failed to create GraalVM context #%d for language %s", (i + 1), languageId);
            }
        }
    }

    /**
     * Closes all {@link Context} instances currently held in the pool.
     * This method should be called during application shutdown or when the pool is no longer needed
     * to release resources associated with the contexts.
     * Any errors encountered during context closure are logged as warnings.
     */
    public void closeAllContexts(){
        LOG.infof("Closing all contexts in pool for language '%s'", languageId);
        Context context;
        while((context = pool.poll()) != null) {
            try{
                context.close();
            } catch(Exception e) {
                LOG.warnf(e, "Error closing context during pool cleanup for language %s", languageId);
            }
        }
    }
}
