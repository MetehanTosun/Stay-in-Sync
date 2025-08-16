package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The central node for performing change detection logic.
 */
@Getter
@Setter
public class ConfigNode extends Node {

    public enum ChangeDetectionMode { AND, OR }

    private ChangeDetectionMode mode;
    private boolean active;

    private Map<String, SnapshotEntry> newSnapshotData;

    /**
     * Default constructor. Initializes with default settings.
     */
    public ConfigNode() {
        this.mode = ChangeDetectionMode.OR;
        this.active = true;
    }

    /**
     * Executes the change detection logic for this node.
     * <p>
     * This method performs four main steps:
     * <ol>
     * <li><b>Bypass Check:</b> If the node's bypass switch (`active` flag) is disabled,
     * it immediately sets its result to {@code false} and builds a new snapshot
     * from the current live values without performing any comparison.</li>
     *
     * <li><b>Snapshot Retrieval:</b> It fetches the previous state (the old snapshot)
     * from a reserved {@code __snapshot} key within the provided {@code dataContext}.</li>
     *
     * <li><b>Comparison Logic:</b> It iterates through its {@link ProviderNode} inputs. For each one,
     * it compares the current live value with the corresponding value from the old snapshot.</li>
     *
     * <li><b>Result Calculation:</b> Based on the configured {@link ChangeDetectionMode} (AND/OR),
     * it calculates the final boolean result. Simultaneously, it constructs the new snapshot,
     * updating entries with new values and timestamps where changes were detected,
     * and carrying over old entries where no change occurred.</li>
     * </ol>
     * The calculated boolean result is stored via {@code setCalculatedResult}, and the new snapshot
     * is stored internally, ready to be retrieved by the LogicGraphEvaluator.
     *
     * @param dataContext A map containing the runtime data for the graph evaluation.
     * This must include the live data under the {@code "source"} key and may
     * contain the old state under the {@code "__snapshot"} key.
     */
    @Override
    public void calculate(Map<String, JsonNode> dataContext) {
        this.newSnapshotData = new HashMap<>();

        // 1. Check bypass switch
        if (!active) {
            this.setCalculatedResult(false);
            // Also create a new snapshot in the bypass to keep the state up to date
            if (this.getInputNodes() != null) {
                for (Node input : this.getInputNodes()) {
                    if (input instanceof ProviderNode) {
                        ProviderNode pNode = (ProviderNode) input;
                        Object liveValue = pNode.getCalculatedResult();
                        newSnapshotData.put(pNode.getJsonPath(), new SnapshotEntry(liveValue, System.currentTimeMillis()));
                    }
                }
            }
            return;
        }

        // 2. Get old snapshot from the dataContext
        JsonNode oldSnapshotNode = dataContext.get("__snapshot");
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        if (oldSnapshotNode != null && !oldSnapshotNode.isNull()) {
            TypeReference<HashMap<String, SnapshotEntry>> typeRef = new TypeReference<>() {};
            oldSnapshot = new ObjectMapper().convertValue(oldSnapshotNode, typeRef);
        }

        // 3. Comparison logic
        boolean hasAtLeastOneChange = false;
        int changeCount = 0;
        int providerNodeCount = 0;

        if (this.getInputNodes() != null) {
            for (Node input : this.getInputNodes()) {
                if (input instanceof ProviderNode) {
                    providerNodeCount++;
                    ProviderNode pNode = (ProviderNode) input;
                    String key = pNode.getJsonPath();
                    Object liveValue = pNode.getCalculatedResult();
                    SnapshotEntry oldEntry = oldSnapshot.get(key);

                    boolean isChanged = (oldEntry == null) || !Objects.equals(oldEntry.value(), liveValue);

                    if (isChanged) {
                        hasAtLeastOneChange = true;
                        changeCount++;
                        newSnapshotData.put(key, new SnapshotEntry(liveValue, System.currentTimeMillis()));
                    } else {
                        newSnapshotData.put(key, oldEntry);
                    }
                }
            }
        }

        // 4. Set result based on mode
        boolean finalResult = false;
        if (this.mode == ChangeDetectionMode.OR) {
            finalResult = hasAtLeastOneChange;
        } else { // AND
            finalResult = (providerNodeCount > 0 && changeCount == providerNodeCount);
        }
        this.setCalculatedResult(finalResult);
    }

    public Map<String, SnapshotEntry> getNewSnapshotData() {
        return this.newSnapshotData;
    }

    @Override
    public Class<?> getOutputType() {
        return Boolean.class;
    }
}