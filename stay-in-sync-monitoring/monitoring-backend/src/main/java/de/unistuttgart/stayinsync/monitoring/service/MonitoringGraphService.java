package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.*;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class MonitoringGraphService {

    private static final String TYPE_SOURCE = "SourceSystem";
    private static final String TYPE_TARGET = "TargetSystem";
    private static final String TYPE_SYNCNODE = "SyncNode";
    private static final String TYPE_POLLINGNODE = "PollingNode";

    private static final String PREFIX_SRC = "SRC_";
    private static final String PREFIX_TGT = "TGT_";
    private static final String PREFIX_POLL = "POLL_";

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_ERROR = "error";

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
    KubernetesPollingNodeService kubernetesPollingNodeService;

    public GraphResponse buildGraph() {
        long globalStart = System.currentTimeMillis();
        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();

        Log.info("buildGraph gestartet");

        List<MonitoringSourceSystemDto> monitoringSources = fetchSourceSystems(nodeMap);
        fetchTargetSystems(nodeMap);
        List<MonitoringSyncJobDto> jobs = fetchSyncJobs(nodeMap, connections);

        connectPollingNodesFromK8s(nodeMap, connections, monitoringSources, jobs);

        GraphResponse graph = new GraphResponse();
        graph.nodes = new ArrayList<>(nodeMap.values());
        graph.connections = connections;

        long globalEnd = System.currentTimeMillis();
        Log.info("buildGraph() komplett dauerte " + (globalEnd - globalStart) + " ms");
        return graph;
    }


    private List<MonitoringSourceSystemDto> fetchSourceSystems(Map<String, NodeDto> nodeMap) {
        long start = System.currentTimeMillis();
        List<MonitoringSourceSystemDto> sources = safeFetch(sourceSystemClient::getAll, "SourceSystems");

        for (MonitoringSourceSystemDto src : sources) {
            boolean isHealthy = checkPrometheus(src.id, src.apiUrl, TYPE_SOURCE);
            nodeMap.put(PREFIX_SRC + src.id,
                    createNode(PREFIX_SRC + src.id, TYPE_SOURCE, src.name, isHealthy ? STATUS_ACTIVE : STATUS_ERROR));
        }
        logDuration("sourceSystemClient.getAll()", start);
        return sources;
    }

    private List<MonitoringTargetSystemDto> fetchTargetSystems(Map<String, NodeDto> nodeMap) {
        long start = System.currentTimeMillis();
        List<MonitoringTargetSystemDto> targets = safeFetch(targetSystemClient::getAll, "TargetSystems");

        for (MonitoringTargetSystemDto tgt : targets) {
            boolean isHealthy = checkPrometheus(tgt.id, tgt.apiUrl, TYPE_TARGET);
            nodeMap.put(PREFIX_TGT + tgt.id,
                    createNode(PREFIX_TGT + tgt.id, TYPE_TARGET, tgt.name, isHealthy ? STATUS_ACTIVE : STATUS_ERROR));
        }
        logDuration("targetSystemClient.getAll()", start);
        return targets;
    }

    private List<MonitoringSyncJobDto> fetchSyncJobs(Map<String, NodeDto> nodeMap, List<NodeConnectionDto> connections) {
        long start = System.currentTimeMillis();
        List<MonitoringSyncJobDto> jobs = safeFetch(syncJobClient::getAll, "SyncJobs");

        for (MonitoringSyncJobDto job : jobs) {
            String syncNodeId = job.id.toString();
            nodeMap.put(syncNodeId, createNode(syncNodeId, TYPE_SYNCNODE, job.name, STATUS_ACTIVE));

            if (job.transformations != null) {
                for (MonitoringTransformationDto tf : job.transformations) {
                    handleTransformation(tf, syncNodeId, nodeMap, connections);
                }
            }
        }
        logDuration("syncJobClient.getAll()", start);
        return jobs;
    }

    private void handleTransformation(MonitoringTransformationDto tf, String syncNodeId,
                                      Map<String, NodeDto> nodeMap, List<NodeConnectionDto> connections) {
        Log.info("Transformation: " + tf.name);

        if (tf.pollingNodes != null) {
            for (String pollingNodeName : tf.pollingNodes) {
                String pollingNodeId = PREFIX_POLL + pollingNodeName;
                nodeMap.putIfAbsent(pollingNodeId,
                        createNode(pollingNodeId, TYPE_POLLINGNODE, pollingNodeName, STATUS_ERROR));

                if (tf.sourceSystemIds != null) {
                    for (Long srcId : tf.sourceSystemIds) {
                        connections.add(createConnection(PREFIX_SRC + srcId, pollingNodeId));
                    }
                }
                connections.add(createConnection(pollingNodeId, syncNodeId));
            }
        }

        if (tf.targetSystemIds != null) {
            for (Long tgtId : tf.targetSystemIds) {
                connections.add(createConnection(syncNodeId, PREFIX_TGT + tgtId));
            }
        }
    }

    private void connectPollingNodesFromK8s(Map<String, NodeDto> nodeMap,
                                            List<NodeConnectionDto> connections,
                                            List<MonitoringSourceSystemDto> monitoringSources,
                                            List<MonitoringSyncJobDto> jobs) {
        long start = System.currentTimeMillis();
        try {
            // Kubernetes-Call mit Timeout-Thread
            List<String> pollingNodesFromK8s = getPollingNodesWithTimeout();

            if (!pollingNodesFromK8s.isEmpty()) {
                Log.info("PollingNodes in K8s: " + pollingNodesFromK8s);
                for (String pollingNodeName : pollingNodesFromK8s) {
                    addPollingNodeAndConnections(nodeMap, connections, monitoringSources, jobs, pollingNodeName, STATUS_ACTIVE);
                }
                return; // fertig, K8s hat geliefert
            }

            // Falls leer → fallback auf lokal
            fallbackToLocalPollingNode(nodeMap, connections, monitoringSources, jobs);

        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der PollingNodes aus K8s", e);
            fallbackToLocalPollingNode(nodeMap, connections, monitoringSources, jobs);
        }
        logDuration("kubernetesPollingNodeService.getPollingNodes()", start);
    }

    private List<String> getPollingNodesWithTimeout() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            return executor.submit(() -> kubernetesPollingNodeService.getPollingNodes())
                    .get(2000, TimeUnit.MILLISECONDS);
        }
    }

    private void fallbackToLocalPollingNode(Map<String, NodeDto> nodeMap,
                                            List<NodeConnectionDto> connections,
                                            List<MonitoringSourceSystemDto> monitoringSources,
                                            List<MonitoringSyncJobDto> jobs) {
        String localPollingNode = "core-polling-node"; // Fallback
        boolean isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live");
        Log.warn("Keine PollingNodes aus K8s gefunden – fallback auf lokalen Node: " + localPollingNode);

        addPollingNodeAndConnections(nodeMap, connections, monitoringSources, jobs, localPollingNode, isHealthy ? STATUS_ACTIVE : STATUS_ERROR);
    }

    private void addPollingNodeAndConnections(Map<String, NodeDto> nodeMap,
                                              List<NodeConnectionDto> connections,
                                              List<MonitoringSourceSystemDto> monitoringSources,
                                              List<MonitoringSyncJobDto> jobs,
                                              String pollingNodeName,
                                              String status) {
        String pollingNodeId = PREFIX_POLL + pollingNodeName;
        nodeMap.putIfAbsent(pollingNodeId,
                createNode(pollingNodeId, TYPE_POLLINGNODE, pollingNodeName, status));

        for (MonitoringSourceSystemDto src : monitoringSources) {
            connections.add(createConnection(PREFIX_SRC + src.id, pollingNodeId));
        }
        for (MonitoringSyncJobDto job : jobs) {
            connections.add(createConnection(pollingNodeId, job.id.toString()));
        }
    }


    private boolean checkPrometheus(Long id, String apiUrl, String type) {
        long start = System.currentTimeMillis();
        boolean isHealthy = false;
        try {
            if (apiUrl != null) {
                isHealthy = prometheusClient.isUp(apiUrl);
            }
        } catch (Exception e) {
            Log.warnf(e, "Prometheus-Check fehlgeschlagen für %s %s (%s)", type, id, apiUrl);
        }
        Log.info("Prometheus-Check für " + type + " " + id + " dauerte " + (System.currentTimeMillis() - start) + " ms");
        return isHealthy;
    }

    private <T> List<T> safeFetch(SupplierWithException<List<T>> supplier, String entityName) {
        try {
            List<T> result = supplier.get();
            Log.info(entityName + " geladen: " + result.size());
            return result;
        } catch (Exception e) {
            Log.error("Fehler beim Abrufen der " + entityName, e);
            return Collections.emptyList();
        }
    }

    private void logDuration(String action, long start) {
        Log.info(action + " dauerte " + (System.currentTimeMillis() - start) + " ms");
    }

    private NodeDto createNode(String id, String type, String label, String status) {
        NodeDto node = new NodeDto();
        node.id = id;
        node.type = type;
        node.label = label;
        node.status = status;
        return node;
    }

    private NodeConnectionDto createConnection(String sourceId, String targetId) {
        NodeConnectionDto conn = new NodeConnectionDto();
        conn.source = sourceId;
        conn.target = targetId;
        conn.status = STATUS_ACTIVE;
        return conn;
    }

    @FunctionalInterface
    interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
