package de.unistuttgart.stayinsync.core.syncnode.domain;

import de.unistuttgart.graphengine.nodes.Node;

import java.util.List;

public record ExecutionPayload(
        TransformJob job,
        List<Node> graphNodes
) {
}
