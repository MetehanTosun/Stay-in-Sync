package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.PrometheusClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SourceSystemClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.TargetSystemClient;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.*;
import io.quarkus.logging.Log;
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
    PrometheusClient prometheusClient;


    public GraphResponse buildGraph() {
        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();

        // 1. Alle SourceSystems
        for (MonitoringSourceSystemDto src : sourceSystemClient.getAll()) {
            boolean isHealthy = prometheusClient.isUp(src.apiUrl);
            nodeMap.put("SRC_" + src.id, createNode("SRC_" + src.id, "SourceSystem", src.name, isHealthy ? "active" : "error"));
        }

        Log.info(nodeMap.toString());

        // 2. Alle TargetSystems
        for (MonitoringTargetSystemDto tgt : targetSystemClient.getAll()) {
            boolean isHealthy = prometheusClient.isUp(tgt.apiUrl);
            nodeMap.put("TGT_" + tgt.id, createNode("TGT_" + tgt.id, "TargetSystem", tgt.name, isHealthy ? "active" : "error"));
        }

        Log.info(nodeMap.toString());

        // Mapping Schritt hier
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll();

        Log.info("Jobs: " + jobs);

        // 3. SyncJobs + Verbindungen
        for (MonitoringSyncJobDto job : jobs) {
            String syncNodeId =  job.id.toString();
            nodeMap.put(syncNodeId, createNode(syncNodeId, "SyncNode", job.name, "active"));

            if (job.transformations != null) {
                for (MonitoringTransformationDto tf : job.transformations) {
                    Log.info(tf.name);

                    Log.info("SourceSystems:" + tf.sourceSystemIds);
                    Log.info("TargetSystems:" + tf.targetSystemIds);
                    Log.info("PollingNodes:" + tf.pollingNodes);

                    // --- PollingNodes hinzufügen ---
                    if (tf.pollingNodes != null) {
                        for (String pollingNodeName : tf.pollingNodes) {
                            String pollingNodeId = "POLL_" + pollingNodeName;
                            //TODO: Mit WorkerPodName machen, wenn auf kubernetes läuft
                            boolean isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live"); // Platzhalter
                            nodeMap.putIfAbsent(pollingNodeId, createNode(pollingNodeId, "PollingNode", pollingNodeName, isHealthy ? "active" : "error"));

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
