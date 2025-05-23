package de.unistuttgart.stayinsync.scriptengine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SyncJobQueue {
    // TODO: Look into passing JobQueue as ref to ScriptEngineService instead of static queue
    private static final BlockingQueue<ScriptEngineService.SyncJob> jobQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Object> resultQueue = new LinkedBlockingQueue<>();

    public static void addJob(ScriptEngineService.SyncJob job) {
        System.out.println("offered job: " + job.scriptId());
        jobQueue.offer(job);
    }

    public static ScriptEngineService.SyncJob takeJob() throws InterruptedException {
        System.out.println("took job");
        return jobQueue.take();
    }

    public static void addResult(Object result) {
        System.out.println("added result");
        resultQueue.offer(result);
    }

    public static Object getResult() throws InterruptedException {
        System.out.println("getting result");
        return resultQueue.take();
    }

}
