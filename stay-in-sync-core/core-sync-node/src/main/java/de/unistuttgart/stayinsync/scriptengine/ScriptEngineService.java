package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.scriptengine.message.ConditionResult;
import de.unistuttgart.stayinsync.scriptengine.message.IntegrityResult;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.scriptengine.message.ValidationResult;
import io.quarkus.runtime.Quarkus;
import io.vertx.core.spi.launcher.ExecutionContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.graalvm.polyglot.*;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Facade ScriptEngineService that provides easy and well-defined entrypoints to interact with the script-engine.
 * Receives Data and scripts in order to run a transformation and return the respective results in addition to Log
 * information.
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
@ApplicationScoped
public class ScriptEngineService {

    private static final Logger LOG = Logger.getLogger(ScriptEngineService.class);

    private final ScriptCache scriptCache;
    private final ContextPool contextPool;

    private static final String SCRIPT_API_BINDING_NAME = "stayinsync";

    @Inject
    public ScriptEngineService(ScriptCache scriptCache, ContextPool contextPool) {
        this.scriptCache = scriptCache;
        this.contextPool = contextPool;
    }

    // TODO: Temporary testing constant will be changed to sync node requirements
    private static final int CONTEXT_POOL_SIZE = 4;
    private static final int RUN_COUNTER = 10_000;

    public static void main(String[] args) throws InterruptedException {
        Quarkus.run(args);
    }

    public String triggerBackgroundJob() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONTEXT_POOL_SIZE);
        for (int i = 0; i < CONTEXT_POOL_SIZE; i++) {
            executor.submit(new ScriptEngineRunner(this));
        }

        exampleManagementManufacturingJob();

        executor.shutdown();
        String result = SyncJobQueue.getResult().toString();
        System.out.println(result);
        return result;
    }

    // TODO: Implement SyncJob wrapper with necessary values
    public record SyncJob(String scriptId, String scriptCode, String expectedHash, Object data) {
        @Override
        public String scriptId() {
            return scriptId;
        }

        @Override
        public String expectedHash() {
            return expectedHash;
        }

        @Override
        public String scriptCode() {
            return scriptCode;
        }

        @Override
        public Object data() {
            return data;
        }
    }

    // TODO: Handle proper validity setting of a job inside the VM
    // TODO: Assign proper input and output data formats
    TransformationResult transform(SyncJob syncJob) {
        TransformationResult result = new TransformationResult("1", syncJob.scriptId());
        Context context = null;

        try {
            context = contextPool.borrowContext();

            if (!scriptCache.containsScript(syncJob.scriptId, syncJob.expectedHash)) {
                scriptCache.putScript(syncJob.scriptId, syncJob.expectedHash, syncJob.scriptCode);
            }
            Source source = scriptCache.getScript(syncJob.scriptId, syncJob.expectedHash);
            if (source == null) {
                result.setValidExecution(false);
                result.addLogMessage("Script source not found in script cache after attempting to add: " + syncJob.scriptId());
                return result;
            }

            ScriptApi scriptApi = new ScriptApi(syncJob.data(), syncJob.scriptId());
            context.getBindings("js").putMember(SCRIPT_API_BINDING_NAME, scriptApi);

            if (syncJob.data() instanceof Map){
                Map<String, Object> namespacedData = (Map<String, Object>) syncJob.data();
                for(Map.Entry<String, Object> entry : namespacedData.entrySet()){

                    System.out.println("" + entry.getKey() + entry.getValue());
                    String namespace = entry.getKey();
                    Object namespaceData = entry.getValue();
                    context.getBindings("js").putMember(namespace, namespaceData);
                }
            } else{
                // TODO: Requires Polling and Namespace Packaging to be implemented for further case testing.
                result.setValidExecution(false);
                result.addLogMessage("Input Data for namespaced script is not a valid Mapping: " + syncJob.scriptId());
            }

            try {
                context.eval(source);

                Object rawOutput = scriptApi.getOutputData();

                if(rawOutput == null){
                    result.addLogMessage("Script did not call " + SCRIPT_API_BINDING_NAME + ".setOutput(). OutputData is null.");
                }
                result.setOutputData(extractResult(Value.asValue(rawOutput)));
                result.setValidExecution(true);
            } catch (PolyglotException e) {
                result.setValidExecution(false);
                result.addLogMessage("Script execution failed [" + syncJob.scriptId() + "]: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            result.addLogMessage("Thread interrupted while waiting for context: " + e.getMessage());
            result.setValidExecution(false);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            result.setValidExecution(false);
            result.addLogMessage("Unexpected error during transformation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // TODO: handle scriptApi still being bound to context / potentially cleanup / packagemanager?
            if (context != null) {
                contextPool.returnContext(context);
            }
        }
        return result;
    }

    private Object extractResult(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isHostObject()) return value.asHostObject();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) return value.asDouble();
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(extractResult(value.getArrayElement(i)));
            }
            return list;
        }

        // TODO: Recursive Object conversion might be simpler with a library
        if (value.hasMembers()) {
            Map<String, Object> map = new HashMap<>();
            value.getMemberKeys().forEach(key -> map.put(key, extractResult(value.getMember(key))));
            return map;
        }
        // TODO: Evaluate other possible return types like functions and promises

        // TODO: Define proper Exception state and logger information handling
        return value.toString(); // temporary
    }

    ConditionResult evaluateCondition(String scriptId, String scriptContext, Object inputData, ExecutionContext executionContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    ValidationResult validateSyntax(String scriptContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    IntegrityResult validateDataSource(Object inputData) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void exampleManagementManufacturingJob(){
        Map<String, Object> mgmtData = new HashMap<>();
        mgmtData.put("facilityName", "Leipzig Plant");
        mgmtData.put("operatorId", "OP-007");
        mgmtData.put("notes", "Scheduled maintenance next week.");
        mgmtData.put("tags", List.of("high-priority", "europe"));


        Map<String, Object> mfgData = new HashMap<>();
        mfgData.put("productId", "MOTOR-V8-TURBO");
        mfgData.put("batchSize", 75);
        mfgData.put("criticalValue", 1.21);
        mfgData.put("status", "In Production");
        Map<String, String> subAssembly = new HashMap<>();
        subAssembly.put("partA", "A-123");
        subAssembly.put("partB", "B-456");
        mfgData.put("subAssemblyParts", subAssembly);


        ScriptEngineService.SyncJob jsonMockJob = SyncJobFactory.getJSONMockTransformationJobTwoNamespaces(
                "aasTransform_01",
                "someHash123",
                mgmtData,
                mfgData
        );

        SyncJobQueue.addJob(jsonMockJob);
    }
}
