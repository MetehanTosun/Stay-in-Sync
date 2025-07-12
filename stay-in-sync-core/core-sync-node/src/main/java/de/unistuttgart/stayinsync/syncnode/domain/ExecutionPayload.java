package de.unistuttgart.stayinsync.syncnode.domain;

import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;

import java.util.List;

public record ExecutionPayload(
        TransformJob job,
        List<Node> graphNodes
) {
}
