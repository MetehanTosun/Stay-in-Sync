package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SnapshotEntry(Object value, long timestamp) {}
