package de.unistuttgart.stayinsync.transport.dto.monitoringgraph;

public class MonitoringSyncJobDto {
        public Long id;
        public String name;
        public boolean deployed;
        public List<MonitoringTransformationDto> transformations;

}
