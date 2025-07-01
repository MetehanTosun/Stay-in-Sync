package de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;

import java.util.Map;

public interface InputNode {
    boolean isParentNode();

    Object getValue(Map<String, JsonNode> context);

    boolean isJsonInputNode();

    boolean isConstantNode();

    LogicNode getParentNode();

}
