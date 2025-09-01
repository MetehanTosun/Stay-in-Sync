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
    private boolean timeWindowEnabled = false;
    private long timeWindowMillis = 30000;
    private Map<String, SnapshotEntry> newSnapshotData;
    private Long testTime = null;

    /**
     * Default constructor. Initializes with default settings.
     */
    public ConfigNode() {
        this.mode = ChangeDetectionMode.OR;
        this.active = true;
    }

    /**
     * Sets a fixed time for testing to make time-based logic predictable.
     * @param time The timestamp in milliseconds to use as "now".
     */
    public void setTestTime(long time) {
        this.testTime = time;
    }

    /**
     * Gets the current time, prioritizing the test time if it has been set.
     * @return The current timestamp in milliseconds.
     */
    private long getCurrentTime() {
        return testTime != null ? testTime : System.currentTimeMillis();
    }

    /**
     * Executes the change detection logic for this node.
     * <p>
     * This method performs a multi-stage evaluation. First, it updates its internal state snapshot by
     * comparing the live values from its {@link ProviderNode} inputs with a previous snapshot provided
     * in the {@code dataContext}. Based on this comparison, it performs one of two checks:
     * <ul>
     * <li><b>Standard Change Detection:</b> If the time window is disabled, it applies a simple
     * {@code AND} or {@code OR} logic to the detected changes.</li>
     * <li><b>Time-Windowed Change Detection:</b> If enabled, it performs a "Sliding Window" check,
     * verifying that the required changes (AND/OR) occurred within the configured time frame.</li>
     * </ul>
     * The method also handles a bypass switch (`active` flag) to disable the check entirely.
     * The final boolean result is stored, and the newly generated snapshot is made available
     * to the LogicGraphEvaluator.
     *
     * @param dataContext A map containing runtime data, including the old snapshot under the "__snapshot" key.
     */
    @Override
    public void calculate(Map<String, JsonNode> dataContext) {
        this.newSnapshotData = new HashMap<>();
        long now = getCurrentTime();

        // 1. Bypass-Check: Wenn der Knoten deaktiviert ist, ist das Ergebnis immer false.
        if (!active) {
            this.setCalculatedResult(false);
            if (this.getInputNodes() != null) {
                for (Node input : this.getInputNodes()) {
                    if (input instanceof ProviderNode) {
                        ProviderNode pNode = (ProviderNode) input;
                        Object liveValue = pNode.getCalculatedResult();
                        newSnapshotData.put(pNode.getJsonPath(), new SnapshotEntry(liveValue, now));
                    }
                }
            }
            return;
        }

        // 2. Alten Snapshot aus dem dataContext holen
        JsonNode oldSnapshotNode = dataContext.get("__snapshot");
        Map<String, SnapshotEntry> oldSnapshot = new HashMap<>();
        if (oldSnapshotNode != null && !oldSnapshotNode.isNull()) {
            TypeReference<HashMap<String, SnapshotEntry>> typeRef = new TypeReference<>() {};
            oldSnapshot = new ObjectMapper().convertValue(oldSnapshotNode, typeRef);
        }

        // 3. Werte vergleichen und neuen Snapshot erstellen
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
                        newSnapshotData.put(key, new SnapshotEntry(liveValue, now));
                    } else {
                        newSnapshotData.put(key, oldEntry);
                    }
                }
            }
        }

        // 4. Finale Entscheidung basierend auf dem Modus treffen
        if (timeWindowEnabled) {
            // Logik für das Zeitfenster
            long windowStart = now - timeWindowMillis;
            int valuesInWindow = 0;

            for (Node input : this.getInputNodes()) {
                if (input instanceof ProviderNode) {
                    ProviderNode pNode = (ProviderNode) input;
                    SnapshotEntry currentEntry = newSnapshotData.get(pNode.getJsonPath());
                    if (currentEntry != null && currentEntry.timestamp() >= windowStart) {
                        valuesInWindow++;
                    }
                }
            }

            boolean timeConditionMet = false;
            if (mode == ChangeDetectionMode.OR) {
                timeConditionMet = valuesInWindow > 0;
            } else { // AND
                timeConditionMet = (providerNodeCount > 0 && valuesInWindow == providerNodeCount);
            }
            this.setCalculatedResult(timeConditionMet);

        } else {
            // Normale Change-Detection-Logik
            boolean changeDetected = false;
            if (this.mode == ChangeDetectionMode.OR) {
                changeDetected = hasAtLeastOneChange;
            } else { // AND
                changeDetected = (providerNodeCount > 0 && changeCount == providerNodeCount);
            }
            this.setCalculatedResult(changeDetected);
        }
    }


    public Map<String, SnapshotEntry> getNewSnapshotData() {
        return this.newSnapshotData;
    }

    @Override
    public Class<?> getOutputType() {
        return Boolean.class;
    }
}