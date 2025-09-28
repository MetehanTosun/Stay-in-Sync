package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.*;
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

    @Inject
    ClientLogger clientLogger;

    @Inject
    KubernetesPollingNodeService kubernetesPollingNodeService;


    public GraphResponse buildGraph() {
        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();
        try{
            clientLogger.logUrl();
            List<MonitoringSourceSystemDto> monitoringSources = sourceSystemClient.getAll();
            Log.info("Monitoring Sources: " + monitoringSources);
        }
        catch (Exception e) {
            Log.error("Fehler beim Abrufen der SourceSystems", e);
        }

        // 1. Alle SourceSystems
        for (MonitoringSourceSystemDto src : sourceSystemClient.getAll()) {
            boolean isHealthy = false;
            try {
                if (src.apiUrl != null) {
                    isHealthy = prometheusClient.isUp(src.apiUrl);
                }
            } catch (Exception e) {
                Log.error("Fehler beim Prometheus-Check für SourceSystem " + src.id + " (" + src.apiUrl + ")", e);
            }
            nodeMap.put("SRC_" + src.id,
                    createNode("SRC_" + src.id, "SourceSystem", src.name, isHealthy ? "active" : "error"));
        }

        Log.info(nodeMap.toString());

        // 2. Alle TargetSystems
        for (MonitoringTargetSystemDto tgt : targetSystemClient.getAll()) {
            boolean isHealthy = false;
            try {
                if (tgt.apiUrl != null) {
                    isHealthy = prometheusClient.isUp(tgt.apiUrl);
                }
            } catch (Exception e) {
                Log.error("Fehler beim Prometheus-Check für TargetSystem " + tgt.id + " (" + tgt.apiUrl + ")", e);
            }
            nodeMap.put("TGT_" + tgt.id,
                    createNode("TGT_" + tgt.id, "TargetSystem", tgt.name, isHealthy ? "active" : "error"));
        }


        Log.info(nodeMap.toString());

        // Mapping Schritt hier
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll();

        Log.info("Jobs: " + jobs);

        List<String> pollingNodesFromK8s = kubernetesPollingNodeService.getPollingNodes();
        if (!pollingNodesFromK8s.isEmpty()){
            Log.info("PollingNodes in K8s: " + pollingNodesFromK8s);
            for (String pollingNodeName : pollingNodesFromK8s) {
                String pollingNodeId = "POLL_" + pollingNodeName;

                boolean isHealthy = false;
                try {
                    isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live");
                } catch (Exception e) {
                    Log.error("Fehler beim Prometheus-Check für PollingNode " + pollingNodeName, e);
                }

                nodeMap.putIfAbsent(pollingNodeId,
                        createNode(pollingNodeId, "PollingNode", pollingNodeName,
                                isHealthy ? "active" : "error"));

                // Hier musst du die Zuordnung zu SourceSystemen definieren.
                // Beispiel: alle SourceSystems verbinden:
                for (MonitoringSourceSystemDto src : sourceSystemClient.getAll()) {
                    connections.add(createConnection("SRC_" + src.id, pollingNodeId, "active"));
                }

                // Verbindung PollingNode → SyncNode (wenn du konkrete SyncNodes hast)
                for (MonitoringSyncJobDto job : jobs) {
                    String syncNodeId = job.id.toString();
                    connections.add(createConnection(pollingNodeId, syncNodeId, "active"));
                }
            }
        }
        // 3. SyncJobs + Verbindungen
        for (MonitoringSyncJobDto job : jobs) {
            String syncNodeId = job.id.toString();
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

                            boolean isHealthy = false;
                            try {
                                isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live");
                            } catch (Exception e) {
                                Log.error("Fehler beim Prometheus-Check für PollingNode " + pollingNodeName, e);
                            }

                            nodeMap.putIfAbsent(pollingNodeId,
                                    createNode(pollingNodeId, "PollingNode", pollingNodeName,
                                            isHealthy ? "active" : "error"));

                            // SourceSystem → PollingNode
                            if (tf.sourceSystemIds != null) {
                                for (Long srcId : tf.sourceSystemIds) {
                                    connections.add(createConnection("SRC_" + srcId, pollingNodeId, "active"));
                                }
                            }

                            // PollingNode → SyncNode
                            connections.add(createConnection(pollingNodeId, syncNodeId, "active"));
                        }
                    }

                    // SyncNode → Target
                    if (tf.targetSystemIds != null) {
                        for (Long tgtId : tf.targetSystemIds) {
                            connections.add(createConnection(syncNodeId, "TGT_" + tgtId, "active"));
                        }
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
