package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.syncnode.domain.ExecutionPayload;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
import de.unistuttgart.graphengine.dto.transformationrule.TransformationRuleDTO;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.graphengine.service.GraphMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.MDC;

/**
 * A central, stateful service that manages the lifecycle of transformations.
 * <p>
 * This service acts as the brain for the synchronization process. Its core responsibilities are:
 * <ul>
 *     <li>Registering transformation definitions, which includes their expected data inputs (ARC manifest).</li>
 *     <li>Tracking the arrival of data chunks (ARCs) for each active transformation.</li>
 *     <li>Determining when a transformation has received all its required data and is "ready" for execution.</li>
 *     <li>Constructing and dispatching an {@link ExecutionPayload} for ready transformations.</li>
 *     <li>Maintaining a global cache of the most recently received data for each ARC alias.</li>
 * </ul>
 */
@ApplicationScoped
public class DispatcherStateService {

    private final GraphMapper graphMapperService;

    // A global cache holding the most recent data received for any given ARC alias.
    private final Map<String, Map<String, Object>> latestArcData = new ConcurrentHashMap<>();

    // The primary registry of all active transformations and their current state.
    private final Map<Long, TransformationState> transformationRegistry = new ConcurrentHashMap<>();

    // A reverse lookup map from an ARC alias to all transformation IDs that require it.
    private final Map<String, List<Long>> arcToTransformationMap = new ConcurrentHashMap<>();

    // A mapping from an ARC alias to its source system's name, used for payload construction.
    private final Map<String, String> arcToSystemAliasMap = new ConcurrentHashMap<>();

    public DispatcherStateService(GraphMapper graphMapperService) {
        this.graphMapperService = graphMapperService;
    }

    /**
     * Registers a new transformation or updates an existing one.
     * <p>
     * This method performs a surgical update. It first removes any existing state associated
     * with the given transformation's ID (e.g., old ARC mappings) and then registers the new
     * configuration. Other transformations remain completely untouched.
     *
     * @param transformation The transformation definition to add or update.
     */
    public void registerOrUpdateTransformation(TransformationMessageDTO transformation) {
        final Long transformationId = transformation.id();
        try {
            MDC.put("transformationId", transformationId.toString());
            Log.infof("Registering or updating transformation. ID: %d, Manifest: %s",
                    transformationId, transformation.arcManifest());

            // Step 1: Surgically remove the old state for this specific transformation ID.
            // This is critical to handle updates where the ARC manifest might have changed.
            deregisterTransformation(transformationId);

            // Step 2: Register the new state.
            TransformationState state = new TransformationState(transformation);
            transformationRegistry.put(transformationId, state);
            registerArcToTransformationMappings(transformation);
            mapArcToSystemAliases(transformation);

            Log.infof("Successfully registered/updated transformation ID: %d", transformationId);
        } finally {
            MDC.remove("transformationId");
        }
    }

    /**
     * Processes an incoming data chunk (ARC) and checks if it completes any transformations.
     *
     * @param arcData The synchronization data message for a specific ARC.
     * @return A list of {@link ExecutionPayload}s for transformations that are now ready to execute.
     * The list will be empty if no transformations were completed by this data.
     */
    public List<ExecutionPayload> processArc(SyncDataMessageDTO arcData) {
        // Step 1: Update the global cache with the latest data for this ARC.
        latestArcData.put(arcData.arcAlias(), arcData.jsonData());

        // Step 2: Find all transformations that are waiting for this ARC.
        List<Long> affectedTransformationIds = arcToTransformationMap.getOrDefault(arcData.arcAlias(), Collections.emptyList());
        if (affectedTransformationIds.isEmpty()) {
            Log.warnf("Received data for unmapped ARC alias '%s'. No transformations are waiting for it.", arcData.arcAlias());
            return Collections.emptyList();
        }

        List<ExecutionPayload> completedPayloads = new ArrayList<>();

        // Step 3: For each affected transformation, record the arrival and check if it's ready.
        for (Long transformationId : affectedTransformationIds) {
            TransformationState state = transformationRegistry.get(transformationId);
            if (state == null) {
                // This could happen in a race condition if a deregisterTransformation() is called.
                Log.warnf("State for transformation ID %d not found in registry, though it was mapped to ARC '%s'.",
                        transformationId, arcData.arcAlias());
                continue;
            }

            state.recordArrival(arcData.arcAlias());

            if (state.isReady()) {
                try {
                    MDC.put("transformationId", transformationId.toString());
                    Log.infof("Transformation %d is ready. Building execution payload.", transformationId);
                    ExecutionPayload payload = buildExecutionPayload(state.getTransformation());
                    completedPayloads.add(payload);
                    state.reset(); // Reset the state for the next run.
                } finally {
                    MDC.remove("transformationId");
                }
            }
        }
        return completedPayloads;
    }

    /**
     * Returns a read-only view of the transformation registry.
     * This is used for monitoring purposes, such as timeout detection.
     *
     * @return An unmodifiable map of the current transformation states.
     */
    public Map<Long, TransformationState> getTransformationRegistry() {
        return Collections.unmodifiableMap(this.transformationRegistry);
    }

    private ExecutionPayload buildExecutionPayload(TransformationMessageDTO transformation) {
        Map<String, Object> sourceSystemPayload = buildSourceSystemPayload(transformation);
        Map<String, Object> finalSource = Map.of("source", sourceSystemPayload);

        List<Node> graphNodes = mapTransformationRuleToGraph(transformation.transformationRuleDTO());

        TransformJob job = new TransformJob(
                transformation.id(),
                "Transformation-" + transformation.id(),
                "job-" + Instant.now().toEpochMilli(),
                transformation.transformationScriptDTO().id().toString(),
                transformation.transformationScriptDTO().javascriptCode(),
                "js", // Placeholder for script type (javascript is default support)
                transformation.transformationScriptDTO().hash(),
                transformation.transformationScriptDTO().generatedSdkCode(),
                transformation.transformationScriptDTO().generatedSdkHash(),
                finalSource);

        return new ExecutionPayload(job, graphNodes, transformation);
    }

    private Map<String, Object> buildSourceSystemPayload(TransformationMessageDTO transformation) {
        Map<String, Object> sourceSystemMap = new ConcurrentHashMap<>();
        for (String arcAlias : transformation.arcManifest()) {
            String systemName = arcToSystemAliasMap.get(arcAlias);
            if (systemName == null) {
                Log.warnf("Could not find systemName for arcAlias '%s'. Skipping this ARC in the final payload.", arcAlias);
                continue;
            }
            Map<String, Object> arcData = latestArcData.get(arcAlias);

            // The defined structure for this object is always the same, thus we can uncheck the cast.
            @SuppressWarnings("unchecked")
            Map<String, Object> arcsForSystem = (Map<String, Object>) sourceSystemMap
                    .computeIfAbsent(systemName, k -> new ConcurrentHashMap<String, Object>());

            arcsForSystem.put(arcAlias, arcData);
        }
        return sourceSystemMap;
    }

    private List<Node> mapTransformationRuleToGraph(TransformationRuleDTO rule) {
        if (rule != null && rule.graphDTO() != null) {
            GraphMapper.MappingResult mappingResult = graphMapperService.toNodeGraph(rule.graphDTO());
            return mappingResult.nodes();
        }
        return new ArrayList<>();
    }

    /**
     * Removes all state associated with a single transformation ID.
     * This is a helper method to ensure clean updates.
     *
     * @param transformationId The ID of the transformation to remove.
     */
    private void deregisterTransformation(Long transformationId) {
        // Remove from the main registry
        TransformationState oldState = transformationRegistry.remove(transformationId);
        if (oldState == null) {
            Log.debugf("Deregister: No existing state found for transformation ID %d. This is a new registration.", transformationId);
            // No old state to clean up.
            return;
        }

        Log.infof("Deregister: Removing existing state for transformation ID %d before update.", transformationId);

        // Remove from the arc -> transformation lookup map
        // This is the most complex part. We must iterate over all ARCs.
        if (oldState.getTransformation().arcManifest() != null) {
            for (String arcAlias : oldState.getTransformation().arcManifest()) {
                List<Long> transformationIds = arcToTransformationMap.get(arcAlias);
                if (transformationIds != null) {
                    // This must be synchronized to prevent race conditions with processArc
                    synchronized (transformationIds) {
                        transformationIds.remove(transformationId);
                    }
                    if (transformationIds.isEmpty()) {
                        arcToTransformationMap.remove(arcAlias);
                    }
                }
            }
        }

        // Remove from the arc -> system alias map
        if (oldState.getTransformation().requestConfigurationMessageDTOS() != null) {
            for (SourceSystemApiRequestConfigurationMessageDTO reqConfig : oldState.getTransformation().requestConfigurationMessageDTOS()) {
                if (reqConfig.name() != null) {
                    arcToSystemAliasMap.remove(reqConfig.name());
                }
            }
        }
    }

    private void registerArcToTransformationMappings(TransformationMessageDTO transformation) {
        if (transformation.arcManifest() != null) {
            for (String arcAlias : transformation.arcManifest()) {
                // computeIfAbsent is thread-safe for adding the list itself
                List<Long> transformationIds = arcToTransformationMap.computeIfAbsent(arcAlias, k -> Collections.synchronizedList(new ArrayList<>()));
                // The add operation on the list must be synchronized
                synchronized (transformationIds) {
                    if (!transformationIds.contains(transformation.id())) {
                        transformationIds.add(transformation.id());
                    }
                }
            }
        }
    }

    private void mapArcToSystemAliases(TransformationMessageDTO transformation) {
        if (transformation.requestConfigurationMessageDTOS() != null) {
            for (SourceSystemApiRequestConfigurationMessageDTO reqConfig : transformation.requestConfigurationMessageDTOS()) {
                SourceSystemMessageDTO sourceSystem = reqConfig.apiConnectionDetails().sourceSystem();
                if (reqConfig.name() != null && sourceSystem != null && sourceSystem.name() != null) {
                    Log.debugf("Mapping arcAlias '%s' to systemName '%s'", reqConfig.name(), sourceSystem.name());
                    arcToSystemAliasMap.put(reqConfig.name(), sourceSystem.name());
                }
            }
        }
    }

    /**
     * Holds the dynamic state for a single, registered transformation.
     * This class is thread-safe, with all state-mutating methods being synchronized.
     */
    public static class TransformationState {
        private final TransformationMessageDTO transformation;
        private final List<String> manifest;
        private final Set<String> receivedArcs;
        private volatile long lastActivityTimestamp;

        public TransformationState(TransformationMessageDTO transformation) {
            this.transformation = transformation;
            this.manifest = transformation.arcManifest() != null ? transformation.arcManifest() : Collections.emptyList();
            this.receivedArcs = ConcurrentHashMap.newKeySet();
            this.lastActivityTimestamp = Instant.now().toEpochMilli();
        }

        /**
         * Records the arrival of a data chunk (ARC) and updates the last activity timestamp.
         *
         * @param arcAlias The alias of the ARC that was received.
         */
        public synchronized void recordArrival(String arcAlias) {
            receivedArcs.add(arcAlias);
            this.lastActivityTimestamp = Instant.now().toEpochMilli();
        }

        /**
         * Checks if all required ARCs as defined in the manifest have been received.
         *
         * @return {@code true} if the transformation is ready, {@code false} otherwise.
         */
        public synchronized boolean isReady() {
            if (manifest.isEmpty() && receivedArcs.isEmpty()) {
                return false;
            }
            return receivedArcs.containsAll(manifest);
        }

        /**
         * Resets the state of this transformation by clearing all received ARCs.
         * This prepares the transformation to be triggered again.
         */
        public synchronized void reset() {
            receivedArcs.clear();
            Log.debugf("State for transformation %d has been reset.", transformation.id());
        }

        public Set<String> getReceivedArcs() {
            return this.receivedArcs;
        }

        public TransformationMessageDTO getTransformation() {
            return this.transformation;
        }

        public long getLastActivityTimestamp() {
            return lastActivityTimestamp;
        }
    }
}
