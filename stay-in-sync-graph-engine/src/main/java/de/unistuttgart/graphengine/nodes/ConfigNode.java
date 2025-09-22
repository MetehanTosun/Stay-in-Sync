package de.unistuttgart.graphengine.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class ConfigNode extends Node {

    public enum ChangeDetectionMode { AND, OR }

    private ChangeDetectionMode mode = ChangeDetectionMode.OR;
    private boolean active = true;
    private boolean timeWindowEnabled = false;
    private long timeWindowMillis = 30000;
    private Map<String, SnapshotEntry> newSnapshotData;
    private Long testTime = null;

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

    private void evaluateStandard(boolean hasAtLeastOneChange, int changeCount, int providerNodeCount) {
        boolean changeDetected = false;
        if (this.mode == ChangeDetectionMode.OR) {
            changeDetected = hasAtLeastOneChange;
        } else { // AND mode
            changeDetected = (providerNodeCount > 0 && changeCount == providerNodeCount);
        }
        this.setCalculatedResult(changeDetected);
    }

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

    public Map<String, SnapshotEntry> getNewSnapshotData() {
        return this.newSnapshotData;
    }

    private long getCurrentTime() {
        return testTime != null ? testTime : System.currentTimeMillis();
    }

    public void setTestTime(long time) {
        this.testTime = time;
    }

    @Override
    public Class<?> getOutputType() {
        return Boolean.class;
    }
}