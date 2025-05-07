package de.unistuttgart.stayinsync.scriptengine;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ContextPool {

    private static final HostAccess SCRIPT_API_ACCESS = HostAccess.newBuilder()
            .allowAccessAnnotatedBy(HostAccess.Export.class)
            .allowImplementationsAnnotatedBy(HostAccess.Implementable.class)
            .allowMapAccess(true)
            .build();

    private final BlockingQueue<Context> pool = new LinkedBlockingQueue<>();

    // TODO: Look into ResourceLimits, can have execution slowdown since statementLimit counter needs to increment
    // TODO: Define a proper Context environment for script execution (add params)
    public ContextPool(int size) {
        for (int i = 0; i < size; i++) {
            pool.add(Context.newBuilder("js")
                    .allowAllAccess(false)
                    .allowHostAccess(SCRIPT_API_ACCESS)
                    .build());
        }
    }

    public Context borrowContext() throws InterruptedException {
        return pool.take();
    }

    public boolean returnContext(Context context) {
        return pool.offer(context);
    }
}
