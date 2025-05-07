package de.unistuttgart.stayinsync.scriptengine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SyncJobQueue {
    // TODO: Look into passing JobQueue as ref to ScriptEngineService instead of static queue
    private static final BlockingQueue<ScriptEngineService.SyncJob> queue = new LinkedBlockingQueue<>();

    public static void addJob(ScriptEngineService.SyncJob job) {
        queue.offer(job);
    }

    public static ScriptEngineService.SyncJob takeJob() throws InterruptedException {
        return queue.take();
    }
}
