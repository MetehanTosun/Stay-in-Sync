package de.unistuttgart.stayinsync.scriptengine;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.jboss.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ContextPool {
    private static final Logger LOG = Logger.getLogger(ContextPool.class);

    private static final HostAccess SCRIPT_API_ACCESS = HostAccess.newBuilder()
            .allowAccessAnnotatedBy(HostAccess.Export.class)
            .allowImplementationsAnnotatedBy(HostAccess.Implementable.class)
            .allowMapAccess(true)
            .allowListAccess(true)
            .build();

    private final BlockingQueue<Context> pool;
    private final int poolSize;
    private final String languageId;

    // TODO: Look into ResourceLimits, can have execution slowdown since statementLimit counter needs to increment
    // TODO: Define a proper Context environment for script execution (add params)
    public ContextPool(String languageId, int size) {
        this.languageId = languageId;
        this.poolSize = size;
        this.pool = new LinkedBlockingQueue<>(size);
        initializePool();
    }

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
