package de.unistuttgart.stayinsync.transport.dto.monitoringgraph;

import java.util.ArrayList;
import java.util.List;

public class NodeDto {
    public String id;
    public String type; // SourceSystem, ASS, SyncNode, TargetSystem, PollingNode
    public String label;
    public String status; // active, inactive, error
    public List<NodeConnectionDto> connections = new ArrayList<>();
    public Double x;
    public Double y;
    public Double fx;
    public Double fy;
}
