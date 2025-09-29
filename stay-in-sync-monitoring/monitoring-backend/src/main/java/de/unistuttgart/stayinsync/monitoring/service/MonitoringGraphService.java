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

        // Versuche Logging (soll Fehler nicht blockieren)
        try {
            clientLogger.logUrl();
        } catch (Exception e) {
            Log.warn("Konnte URL nicht loggen", e);
        }

        // --- SourceSystems ---
        List<MonitoringSourceSystemDto> monitoringSources = new ArrayList<>();
        try {
            monitoringSources = sourceSystemClient.getAll();
            Log.info("Monitoring Sources: " + monitoringSources);
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der SourceSystems", e);
        }

        for (MonitoringSourceSystemDto src : monitoringSources) {
            boolean isHealthy = false;
            try {
                if (src.apiUrl != null) {
                    isHealthy = prometheusClient.isUp(src.apiUrl);
                }
            } catch (Exception e) {
                Log.warnf(e, "Prometheus-Check fehlgeschlagen für SourceSystem %s (%s)", src.id, src.apiUrl);
            }
            nodeMap.put("SRC_" + src.id,
                    createNode("SRC_" + src.id, "SourceSystem", src.name, isHealthy ? "active" : "error"));
        }

        // --- TargetSystems ---
        List<MonitoringTargetSystemDto> monitoringTargets = new ArrayList<>();
        try {
            monitoringTargets = targetSystemClient.getAll();
            Log.info("Monitoring Targets: " + monitoringTargets);
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der TargetSystems", e);
        }

        for (MonitoringTargetSystemDto tgt : monitoringTargets) {
            boolean isHealthy = false;
            try {
                if (tgt.apiUrl != null) {
                    isHealthy = prometheusClient.isUp(tgt.apiUrl);
                }
            } catch (Exception e) {
                Log.warnf(e, "Prometheus-Check fehlgeschlagen für TargetSystem %s (%s)", tgt.id, tgt.apiUrl);
            }
            nodeMap.put("TGT_" + tgt.id,
                    createNode("TGT_" + tgt.id, "TargetSystem", tgt.name, isHealthy ? "active" : "error"));
        }

        // --- SyncJobs ---
        List<MonitoringSyncJobDto> jobs = new ArrayList<>();
        try {
            jobs = syncJobClient.getAll();
            Log.info("Jobs: " + jobs);
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der SyncJobs", e);
        }

        for (MonitoringSyncJobDto job : jobs) {
            String syncNodeId = job.id.toString();
            nodeMap.put(syncNodeId, createNode(syncNodeId, "SyncNode", job.name, "active"));

            if (job.transformations != null) {
                for (MonitoringTransformationDto tf : job.transformations) {
                    Log.info("Transformation: " + tf.name);

                    // --- PollingNodes hinzufügen ---
                    if (tf.pollingNodes != null) {
                        for (String pollingNodeName : tf.pollingNodes) {
                            String pollingNodeId = "POLL_" + pollingNodeName;
                            boolean isHealthy = false;
                            try {
                                isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live");
                            } catch (Exception e) {
                                Log.warnf(e, "Prometheus-Check fehlgeschlagen für PollingNode %s", pollingNodeName);
                            }

                            nodeMap.putIfAbsent(pollingNodeId,
                                    createNode(pollingNodeId, "PollingNode", pollingNodeName,
                                            isHealthy ? "active" : "error"));

                            if (tf.sourceSystemIds != null) {
                                for (Long srcId : tf.sourceSystemIds) {
                                    connections.add(createConnection("SRC_" + srcId, pollingNodeId, "active"));
                                }
                            }
                            connections.add(createConnection(pollingNodeId, syncNodeId, "active"));
                        }
                    }

                    // --- SyncNode → Target ---
                    if (tf.targetSystemIds != null) {
                        for (Long tgtId : tf.targetSystemIds) {
                            connections.add(createConnection(syncNodeId, "TGT_" + tgtId, "active"));
                        }
                    }
                }
            }
        }

        // --- PollingNodes aus K8s ---
        try {
            List<String> pollingNodesFromK8s = kubernetesPollingNodeService.getPollingNodes();
            if (!pollingNodesFromK8s.isEmpty()) {
                Log.info("PollingNodes in K8s: " + pollingNodesFromK8s);
                for (String pollingNodeName : pollingNodesFromK8s) {
                    String pollingNodeId = "POLL_" + pollingNodeName;
                    nodeMap.putIfAbsent(pollingNodeId,
                            createNode(pollingNodeId, "PollingNode", pollingNodeName, "active"));

                    // alle SourceSystems verbinden
                    for (MonitoringSourceSystemDto src : monitoringSources) {
                        connections.add(createConnection("SRC_" + src.id, pollingNodeId, "active"));
                    }
                    // alle Jobs verbinden
                    for (MonitoringSyncJobDto job : jobs) {
                        connections.add(createConnection(pollingNodeId, job.id.toString(), "active"));
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der PollingNodes aus K8s", e);
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
