package de.unistuttgart.stayinsync.syncnode.logik_engine;

public interface InputNode {
    boolean isParentNode();

    Object getValue();

    boolean isJsonNode();

    boolean isConstantNode();

    LogicNode getParentNode();

}
