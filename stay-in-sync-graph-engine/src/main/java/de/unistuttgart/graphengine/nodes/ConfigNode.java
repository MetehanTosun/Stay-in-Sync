package de.unistuttgart.graphengine.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A special node that performs change detection on data from connected {@link ProviderNode}s.
 * <p>
 * The ConfigNode is the heart of the change detection mechanism in the graph engine.
 * It compares current data values against a stored snapshot from the previous execution
 * to determine whether any changes have occurred. This enables efficient event-driven
 * processing where transformations are only triggered when relevant data changes.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li><b>Snapshot Management:</b> Maintains a memory of previous values for comparison</li>
 *   <li><b>Change Detection Modes:</b> Supports OR (any change) and AND (all changed) logic</li>
 *   <li><b>Time Window:</b> Optional time-based filtering for change detection</li>
 *   <li><b>First Execution Handling:</b> Returns true on first run while initializing snapshot</li>
 * </ul>
 * <p>
 * <b>Behavior on First Execution:</b><br>
 * On the very first execution (when no snapshot exists), this node returns {@code true}
 * to signal that new data has been detected and initializes the snapshot with current values.
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * ConfigNode config = new ConfigNode();
 * config.setMode(ChangeDetectionMode.OR);  // Trigger on any change
 * config.setActive(true);
 * config.setInputNodes(List.of(providerNode1, providerNode2));
 * </pre>
 *
 * @see ProviderNode
 * @see SnapshotEntry
 * @see de.unistuttgart.graphengine.cache.StatefulLogicGraph
 */
public class ConfigNode extends Node {

    /**
     * Defines the logical mode for evaluating multiple provider node changes.
     * <ul>
     *   <li><b>OR:</b> Returns true if <i>at least one</i> provider node has changed</li>
     *   <li><b>AND:</b> Returns true only if <i>all</i> provider nodes have changed</li>
     * </ul>
     */
    public enum ChangeDetectionMode { 
        /** Triggers when at least one input has changed */
        OR,
        /** Triggers when all inputs have changed */
        AND 
    }

    /** The change detection mode to use when evaluating provider nodes. Default is OR. */
    private ChangeDetectionMode mode = ChangeDetectionMode.OR;
    
    /** Whether change detection is active. If false, always returns false and builds snapshot. */
    private boolean active = true;
    
    /** Whether time window filtering is enabled for change detection. */
    private boolean timeWindowEnabled = false;
    
    /** The time window duration in milliseconds. Default is 30 seconds. */
    private long timeWindowMillis = 30000;
    
    /** The newly computed snapshot data after this evaluation. */
    private Map<String, SnapshotEntry> newSnapshotData;
    
    /** Optional test time override for deterministic testing. If null, uses system time. */
    private Long testTime = null;

    /**
     * Performs the change detection logic by comparing current provider node values
     * against the stored snapshot.
     * <p>
     * <b>Algorithm:</b>
     * <ol>
     *   <li>If inactive, return false and build snapshot</li>
     *   <li>For each connected ProviderNode:
     *     <ul>
     *       <li>First execution: Initialize snapshot and detect as change</li>
     *       <li>Subsequent executions: Compare with snapshot and detect changes</li>
     *     </ul>
     *   </li>
     *   <li>Evaluate change detection based on mode (OR/AND) and optional time window</li>
     * </ol>
     *
     * @param dataContext The runtime data context containing the old snapshot under key "__snapshot"
     */
    @Override
    public void calculate(Map<String, Object> dataContext) {
        this.newSnapshotData = new HashMap<>();
        long now = getCurrentTime();

        if (!active) {
            this.setCalculatedResult(false);
            buildSnapshotForInactive(now);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, SnapshotEntry> oldSnapshot = (Map<String, SnapshotEntry>) dataContext.get("__snapshot");
        if (oldSnapshot == null) {
            oldSnapshot = new HashMap<>();
        }

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

        // If no ProviderNodes are connected, the node acts as a pass-through and returns true.
        // The change detection is effectively inactive until configured.
        if (providerNodeCount == 0) {
            this.setCalculatedResult(true);
            return;
        }

        if (timeWindowEnabled) {
            evaluateWithTimeWindow(providerNodeCount, now);
        } else {
            evaluateStandard(hasAtLeastOneChange, changeCount, providerNodeCount);
        }
    }

    /**
     * Builds a snapshot of all connected provider node values when the node is inactive.
     * This ensures the snapshot stays up-to-date even when change detection is disabled.
     *
     * @param now The current timestamp in milliseconds
     */
    private void buildSnapshotForInactive(long now) {
        if (this.getInputNodes() != null) {
            for (Node input : this.getInputNodes()) {
                if (input instanceof ProviderNode) {
                    ProviderNode pNode = (ProviderNode) input;
                    Object liveValue = pNode.getCalculatedResult();
                    newSnapshotData.put(pNode.getJsonPath(), new SnapshotEntry(liveValue, now));
                }
            }
        }
    }

    /**
     * Evaluates change detection using standard mode (without time window).
     * <p>
     * In OR mode, returns true if at least one change was detected.
     * In AND mode, returns true only if all provider nodes have changed.
     *
     * @param hasAtLeastOneChange Whether at least one provider node changed
     * @param changeCount The total number of provider nodes that changed
     * @param providerNodeCount The total number of connected provider nodes
     */
    private void evaluateStandard(boolean hasAtLeastOneChange, int changeCount, int providerNodeCount) {
        boolean changeDetected = false;
        if (this.mode == ChangeDetectionMode.OR) {
            changeDetected = hasAtLeastOneChange;
        } else { // AND mode
            changeDetected = (providerNodeCount > 0 && changeCount == providerNodeCount);
        }
        this.setCalculatedResult(changeDetected);
    }

    /**
     * Evaluates change detection with time window filtering.
     * <p>
     * Only changes that occurred within the specified time window are considered.
     * This is useful for debouncing or filtering out stale changes.
     *
     * @param providerNodeCount The total number of connected provider nodes
     * @param now The current timestamp in milliseconds
     */
    private void evaluateWithTimeWindow(int providerNodeCount, long now) {
        long windowStart = now - timeWindowMillis;
        int changesInWindow = 0;

        for (SnapshotEntry newEntry : newSnapshotData.values()) {
            if (newEntry.timestamp() >= windowStart) {
                changesInWindow++;
            }
        }

        boolean timeConditionMet = false;
        if (mode == ChangeDetectionMode.OR) {
            timeConditionMet = changesInWindow > 0;
        } else { // AND mode
            timeConditionMet = (providerNodeCount > 0 && changesInWindow == providerNodeCount);
        }
        this.setCalculatedResult(timeConditionMet);
    }

    /**
     * Returns the newly computed snapshot data after the most recent evaluation.
     * <p>
     * This snapshot contains the current values and timestamps of all monitored
     * provider nodes and will be used as the baseline for the next evaluation.
     *
     * @return A map of JSON paths to their snapshot entries
     */
    public Map<String, SnapshotEntry> getNewSnapshotData() {
        return this.newSnapshotData;
    }

    /**
     * Gets the current time in milliseconds.
     * <p>
     * Returns the test time if set (for deterministic testing), otherwise
     * returns the current system time.
     *
     * @return The current timestamp in milliseconds
     */
    private long getCurrentTime() {
        return testTime != null ? testTime : System.currentTimeMillis();
    }

    /**
     * Sets a fixed test time for deterministic testing.
     * <p>
     * When set, this time is used instead of the system time for all
     * timestamp operations. This allows for predictable test behavior.
     *
     * @param time The test timestamp in milliseconds
     */
    public void setTestTime(long time) {
        this.testTime = time;
    }

    @Override
    public Class<?> getOutputType() {
        return Boolean.class;
    }

    // === Getters and Setters ===

    /**
     * Gets the current change detection mode.
     *
     * @return The configured change detection mode (OR or AND)
     */
    public ChangeDetectionMode getMode() {
        return mode;
    }

    /**
     * Sets the change detection mode.
     *
     * @param mode The change detection mode to use (OR or AND)
     */
    public void setMode(ChangeDetectionMode mode) {
        this.mode = mode;
    }

    /**
     * Checks if change detection is currently active.
     *
     * @return True if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether change detection is active.
     * <p>
     * When inactive, this node always returns false and only maintains the snapshot.
     *
     * @param active Whether change detection should be active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks if time window filtering is enabled.
     *
     * @return True if time window is enabled, false otherwise
     */
    public boolean isTimeWindowEnabled() {
        return timeWindowEnabled;
    }

    /**
     * Sets whether time window filtering is enabled.
     *
     * @param timeWindowEnabled Whether to enable time window filtering
     */
    public void setTimeWindowEnabled(boolean timeWindowEnabled) {
        this.timeWindowEnabled = timeWindowEnabled;
    }

    /**
     * Gets the time window duration in milliseconds.
     *
     * @return The time window duration
     */
    public long getTimeWindowMillis() {
        return timeWindowMillis;
    }

    /**
     * Sets the time window duration in milliseconds.
     *
     * @param timeWindowMillis The time window duration (must be positive)
     */
    public void setTimeWindowMillis(long timeWindowMillis) {
        this.timeWindowMillis = timeWindowMillis;
    }

    /**
     * Sets the snapshot data directly.
     * <p>
     * This is primarily used for testing purposes.
     *
     * @param newSnapshotData The snapshot data to set
     */
    public void setNewSnapshotData(Map<String, SnapshotEntry> newSnapshotData) {
        this.newSnapshotData = newSnapshotData;
    }

    /**
     * Gets the test time override value.
     *
     * @return The test time in milliseconds, or null if using system time
     */
    public Long getTestTime() {
        return testTime;
    }

    /**
     * Sets the test time override value.
     *
     * @param testTime The test time in milliseconds, or null to use system time
     */
    public void setTestTime(Long testTime) {
        this.testTime = testTime;
    }
}
