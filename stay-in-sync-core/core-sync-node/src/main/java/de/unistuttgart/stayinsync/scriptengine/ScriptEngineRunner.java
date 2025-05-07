package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.scriptengine.resultobject.TransformationResult;

public class ScriptEngineRunner implements Runnable {

    private final ScriptEngineService service;

    public ScriptEngineRunner(ScriptEngineService service) {
        this.service = service;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ScriptEngineService.SyncJob job = SyncJobQueue.takeJob();
                TransformationResult result = service.transform(job);
                System.out.println(result.getOutputData());
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                e.printStackTrace();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
