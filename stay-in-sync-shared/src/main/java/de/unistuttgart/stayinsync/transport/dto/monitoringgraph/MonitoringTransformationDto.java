package de.unistuttgart.stayinsync.transport.dto.monitoringgraph;

import java.util.List;

public class MonitoringTransformationDto {
    public Long id;
    public List<Long> sourceSystemIds;
    public List<Long> targetSystemIds;
    public String name;
    public String description;
    public List<String> pollingNodes;
}
