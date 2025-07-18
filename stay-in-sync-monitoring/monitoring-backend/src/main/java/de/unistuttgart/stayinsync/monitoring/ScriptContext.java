package de.unistuttgart.stayinsync.monitoring;

public class ScriptContext {
    private static final ThreadLocal<String> JOB_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SCRIPT_ID = new ThreadLocal<>();

    public static void setJobId(String id) { JOB_ID.set(id); }
    public static void setScriptId(String id) { SCRIPT_ID.set(id); }

    public static String getJobId() { return JOB_ID.get(); }
    public static String getScriptId() { return SCRIPT_ID.get(); }

    public static void clear() {
        JOB_ID.remove();
        SCRIPT_ID.remove();
    }
}