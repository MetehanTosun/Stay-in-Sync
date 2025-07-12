package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.syncnode.domain.ExecutionPayload;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logik_engine.GraphMapperService;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import de.unistuttgart.stayinsync.transport.dto.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class DispatcherStateService {

    @Inject
    GraphMapperService graphMapperService;

    private final Map<String, Map<String, Object>> latestArcData = new ConcurrentHashMap<>();

    private final Map<Long, TransformationState> transformationRegistry = new ConcurrentHashMap<>();

    private final Map<String, List<Long>> arcToTransformationMap = new ConcurrentHashMap<>();

    private final Map<String, String> arcToSystemAliasMap = new ConcurrentHashMap<>();

    public void loadInitialTransformations(SyncJobMessageDTO syncJob){
        if (syncJob.transformations() == null){
            return;
        }

        transformationRegistry.clear();
        arcToTransformationMap.clear();
        arcToSystemAliasMap.clear();

        for(TransformationMessageDTO tx : syncJob.transformations()){
            Log.infof("Registering Transformation ID: %d, Manifest: %s", tx.id(), tx.arcManifest());
            TransformationState state = new TransformationState(tx);
            transformationRegistry.put(tx.id(), state);

            for (String arcAlias : tx.arcManifest()){
                arcToTransformationMap.computeIfAbsent(arcAlias, k -> new ArrayList<>()).add(tx.id());
            }

            if(tx.requestConfigurationMessageDTOS() != null){
                for(SourceSystemApiRequestConfigurationMessageDTO reqConfig : tx.requestConfigurationMessageDTOS()){
                    SourceSystemMessageDTO sourceSystem = reqConfig.apiConnectionDetails().sourceSystem();
                    if (reqConfig.name() != null && sourceSystem != null && sourceSystem.name() != null){
                        Log.debugf("Mapping arcAlias '%s' to systemName '%s'", reqConfig.name(), sourceSystem.name());
                        arcToSystemAliasMap.put(reqConfig.name(), sourceSystem.name());
                    }
                }
            }
        }
    }

    public List<ExecutionPayload> processArc(SyncDataMessageDTO arc){
        latestArcData.put(arc.arcAlias(), arc.jsonData());
        List<Long> affectedTransformationIds = arcToTransformationMap.getOrDefault(arc.arcAlias(), Collections.emptyList());
        if(affectedTransformationIds.isEmpty()){
            return Collections.emptyList();
        }

        List<ExecutionPayload> completedPayloads = new ArrayList<>();

        for (Long txId : affectedTransformationIds){
            TransformationState state = transformationRegistry.get(txId);

            state.recordArrival(arc.arcAlias());

            if (state.isReady()){
                Log.infof("Transformation %d is ready! Building job.", txId);
                ExecutionPayload payload = buildExecutionPayload(state.getTransformation());
                completedPayloads.add(payload);

                state.reset();
            }
        }
        return completedPayloads;
    }

    private ExecutionPayload buildExecutionPayload(TransformationMessageDTO tx){
        Map<String, Object> sourceSystemMap = new ConcurrentHashMap<>();

        for(String arcAlias : tx.arcManifest()){
            String systemName = arcToSystemAliasMap.get(arcAlias);
            if(systemName == null){
                Log.warnf("Could not find systemName for arcAlias '%s'. Skipping this ARC in the final payload.", arcAlias);
                continue;
            }
            Map<String, Object> arcData = latestArcData.get(arcAlias);

            Map<String, Object> arcsForSystem = (Map<String, Object>) sourceSystemMap
                    .computeIfAbsent(systemName, k -> new ConcurrentHashMap<String, Object>());

            arcsForSystem.put(arcAlias, arcData);
        }

        Map<String, Object> finalSource = Map.of("source", sourceSystemMap);

        TransformationRuleDTO rule = tx.transformationRuleDTO();
        List<Node> graphNodes = graphMapperService.toNodeGraph(rule.graphDTO());

        TransformJob job = new TransformJob(
                "Transformation-" + tx.id(),
                "job-" + Instant.now().toEpochMilli(),
                "script-for-" + tx.id(),
                tx.transformationScriptDTO().javascriptCode(),
                "js", // Placeholder
                tx.transformationScriptDTO().hash(),
                finalSource);
        return new ExecutionPayload(job, graphNodes);
    }

    public Map<Long, TransformationState> getTransformationRegistry(){
        return this.transformationRegistry;
    }

    public static class TransformationState {
        private final TransformationMessageDTO transformation;
        private final List<String> manifest;
        private final Set<String> receivedArcs;
        private volatile long lastActivityTimestamp;

        public TransformationState(TransformationMessageDTO transformation) {
            this.transformation = transformation;
            this.manifest = transformation.arcManifest();
            this.receivedArcs = ConcurrentHashMap.newKeySet();
            this.lastActivityTimestamp = Instant.now().toEpochMilli();
        }

        public synchronized void recordArrival(String arcAlias){
            receivedArcs.add(arcAlias);
            this.lastActivityTimestamp = Instant.now().toEpochMilli();
        }

        public synchronized boolean isReady(){
            return receivedArcs.size() == manifest.size();
        }

        public synchronized void reset(){
            receivedArcs.clear();
        }

        public synchronized List<String> getManifest(){
            return manifest;
        }

        public synchronized Set<String> getReceivedArcs(){
            return this.receivedArcs;
        }

        public TransformationMessageDTO getTransformation(){
            return this.transformation;
        }

        public long getLastActivityTimestamp(){ return lastActivityTimestamp; }
    }
}
