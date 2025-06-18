package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;

import java.util.Map;

public interface InputNode {
    boolean isParentNode();

    Object getValue(Map<String, JsonObject> context);

    boolean isJsonNode();

    boolean isConstantNode();

    LogicNode getParentNode();

}
