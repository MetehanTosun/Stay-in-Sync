package de.unistuttgart.stayinsync.syncnode.logik_engine;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.JsonObject;

import java.util.Map;

public interface InputNode {
    boolean isParentNode();

    Object getValue(Map<String, JsonNode> context);

    boolean isJsonInputNode();

    boolean isConstantNode();

    LogicNode getParentNode();

}
