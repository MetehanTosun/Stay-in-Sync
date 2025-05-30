package de.unistuttgart.stayinsync.syncnode.logik_engine;

public interface InputProvider {
    boolean isNodeSource();

    boolean isExternalSource();

    boolean isUISource();

    LogicNode getParentNode();

    String getExternalJsonPath();

    String getUiElementName();
}
