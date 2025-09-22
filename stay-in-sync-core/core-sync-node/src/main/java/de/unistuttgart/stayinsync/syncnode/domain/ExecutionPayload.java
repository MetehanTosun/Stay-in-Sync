package de.unistuttgart.stayinsync.syncnode.domain;

import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.stayinsync.core.transport.dto.TransformationMessageDTO;


import java.util.List;

public record ExecutionPayload(
        TransformJob job,
        List<Node> graphNodes,
        TransformationMessageDTO transformationContext
) {
}
