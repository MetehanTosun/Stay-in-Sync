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
        long globalStart = System.currentTimeMillis();

        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();

        // Logging Start
        Log.info("buildGraph gestartet");

        // --- SourceSystems ---
        long t1 = System.currentTimeMillis();
        List<MonitoringSourceSystemDto> monitoringSources = new ArrayList<>();
        try {
            monitoringSources = sourceSystemClient.getAll();
            Log.info("Monitoring Sources geladen: " + monitoringSources.size());
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der SourceSystems", e);
        }
        long t2 = System.currentTimeMillis();
        Log.info("sourceSystemClient.getAll() dauerte " + (t2 - t1) + " ms");

        // pro SourceSystem: Prometheus-Check
        for (MonitoringSourceSystemDto src : monitoringSources) {
            long ps = System.currentTimeMillis();
            boolean isHealthy = false;
            try {
                if (src.apiUrl != null) {
                    isHealthy = prometheusClient.isUp(src.apiUrl);
                }
            } catch (Exception e) {
                Log.warnf(e, "Prometheus-Check fehlgeschlagen für SourceSystem %s (%s)", src.id, src.apiUrl);
            }
            long pe = System.currentTimeMillis();
            Log.info("Prometheus-Check für SourceSystem " + src.id + " dauerte " + (pe - ps) + " ms");

            nodeMap.put("SRC_" + src.id,
                    createNode("SRC_" + src.id, "SourceSystem", src.name, isHealthy ? "active" : "error"));
        }

        // --- TargetSystems ---
        long t3 = System.currentTimeMillis();
        List<MonitoringTargetSystemDto> monitoringTargets = new ArrayList<>();
        try {
            monitoringTargets = targetSystemClient.getAll();
            Log.info("Monitoring Targets geladen: " + monitoringTargets.size());
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der TargetSystems", e);
        }
        long t4 = System.currentTimeMillis();
        Log.info("targetSystemClient.getAll() dauerte " + (t4 - t3) + " ms");

        // pro TargetSystem: Prometheus-Check
        for (MonitoringTargetSystemDto tgt : monitoringTargets) {
            long ps = System.currentTimeMillis();
            boolean isHealthy = false;
            try {
                if (tgt.apiUrl != null) {
                    isHealthy = prometheusClient.isUp(tgt.apiUrl);
                }
            } catch (Exception e) {
                Log.warnf(e, "Prometheus-Check fehlgeschlagen für TargetSystem %s (%s)", tgt.id, tgt.apiUrl);
            }
            long pe = System.currentTimeMillis();
            Log.info("Prometheus-Check für TargetSystem " + tgt.id + " dauerte " + (pe - ps) + " ms");

            nodeMap.put("TGT_" + tgt.id,
                    createNode("TGT_" + tgt.id, "TargetSystem", tgt.name, isHealthy ? "active" : "error"));
        }

        // --- SyncJobs ---
        long t5 = System.currentTimeMillis();
        List<MonitoringSyncJobDto> jobs = new ArrayList<>();
        try {
            jobs = syncJobClient.getAll();
            Log.info("Jobs geladen: " + jobs.size());
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der SyncJobs", e);
        }
        long t6 = System.currentTimeMillis();
        Log.info("syncJobClient.getAll() dauerte " + (t6 - t5) + " ms");

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
        long t7 = System.currentTimeMillis();

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
        long t8 = System.currentTimeMillis();
        Log.info("kubernetesPollingNodeService.getPollingNodes() dauerte " + (t8 - t7) + " ms");

        GraphResponse graph = new GraphResponse();
        graph.nodes = new ArrayList<>(nodeMap.values());
        graph.connections = connections;

        long globalEnd = System.currentTimeMillis();
        Log.info("buildGraph() komplett dauerte " + (globalEnd - globalStart) + " ms");
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
