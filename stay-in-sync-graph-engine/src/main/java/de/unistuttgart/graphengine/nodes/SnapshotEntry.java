package de.unistuttgart.graphengine.nodes;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SnapshotEntry(Object value, long timestamp) {}
