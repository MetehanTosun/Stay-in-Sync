package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SourceSystemClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.TargetSystemClient;
import de.unistuttgart.stayinsync.monitoring.mapping.MonitoringGraphSyncJobMapper;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MonitoringGraphService {

    @Inject
    @RestClient
    SourceSystemClient sourceSystemClient;

    @Inject
    @RestClient
    TargetSystemClient targetSystemClient;

    @Inject
    @RestClient
    SyncJobClient syncJobClient;

    @Inject
    MonitoringGraphSyncJobMapper syncJobMapper;

    public GraphResponse buildGraph() {
        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();

        // 1. Alle SourceSystems
        for (MonitoringSourceSystemDto src : sourceSystemClient.getAll()) {
            nodeMap.put("SRC_" + src.id, createNode("SRC_" + src.id, "SourceSystem", src.name, src.status));
        }

        // 2. Alle TargetSystems
        for (MonitoringTargetSystemDto tgt : targetSystemClient.getAll()) {
            nodeMap.put("TGT_" + tgt.id, createNode("TGT_" + tgt.id, "TargetSystem", tgt.name, tgt.status));
        }

        // Mapping Schritt hier
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll().stream()
                .map(syncJobMapper::mapToDto) // Entity → DTO
                .toList();

        // 3. SyncJobs + Verbindungen
        for (MonitoringSyncJobDto job : jobs) {
            String syncNodeId =  job.id.toString();
            nodeMap.put(syncNodeId, createNode(syncNodeId, "SyncNode", job.name, job.deployed ? "active" : "inactive"));

            if (job.transformations != null) {
                for (MonitoringTransformationDto tf : job.transformations) {

                    // --- PollingNodes hinzufügen ---
                    if (tf.pollingNodes != null) {
                        for (String pollingNodeName : tf.pollingNodes) {
                            String pollingNodeId = "POLL_" + pollingNodeName;
                            nodeMap.putIfAbsent(pollingNodeId, createNode(pollingNodeId, "PollingNode", pollingNodeName, "active"));

                            // SourceSystem → PollingNode
                            for (Long srcId : tf.sourceSystemIds) {
                                connections.add(createConnection("SRC_" + srcId, pollingNodeId, "active"));
                            }

                            // PollingNode → SyncNode
                            connections.add(createConnection(pollingNodeId, syncNodeId, "active"));
                        }
                    }

                    // SyncNode → Target
                    for (Long tgtId : tf.targetSystemIds) {
                        connections.add(createConnection(syncNodeId, "TGT_" + tgtId, "active"));
                    }
                }
            }
        }

        GraphResponse graph = new GraphResponse();
        graph.nodes = new ArrayList<>(nodeMap.values());
        graph.connections = connections;
        return graph;
    }


    private NodeDto createNode(String id, String type, String label, String status) {
        NodeDto node = new NodeDto();
        node.id = id;
        node.type = type;
        node.label = label;
        node.status = status;
        return node;
    }

    private NodeConnectionDto createConnection(String sourceId, String targetId, String status) {
        NodeConnectionDto conn = new NodeConnectionDto();
        conn.source = sourceId;
        conn.target = targetId;
        conn.status = status;
        return conn;
    }
}
