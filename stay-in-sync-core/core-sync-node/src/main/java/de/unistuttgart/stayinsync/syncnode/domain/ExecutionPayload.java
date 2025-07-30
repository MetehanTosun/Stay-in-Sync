package de.unistuttgart.stayinsync.syncnode.domain;

import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;

import java.util.List;

public record ExecutionPayload(
        TransformJob job,
        List<Node> graphNodes
) {
}
