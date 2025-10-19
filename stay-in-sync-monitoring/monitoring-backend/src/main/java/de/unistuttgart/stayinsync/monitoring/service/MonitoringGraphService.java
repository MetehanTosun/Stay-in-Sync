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

/**
 * Service responsible for building the monitoring graph that represents
 * source systems, target systems, sync jobs, and polling nodes, along with
 * their health status and interconnections.
 *
 * <p>
 * The graph is constructed by:
 * <ul>
 *     <li>Fetching source systems, target systems, and sync jobs from their respective services.</li>
 *     <li>Checking health status via Prometheus.</li>
 *     <li>Fetching active polling nodes from Kubernetes (with a fallback to a local node).</li>
 *     <li>Creating nodes and connections (edges) between the entities.</li>
 * </ul>
 * </p>
 */
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

    /**
     * Builds the complete monitoring graph by fetching all nodes and their connections.
     *
     * @return {@link GraphResponse} containing the nodes and connections of the system
     */
    public GraphResponse buildGraph() {
        long globalStart = System.currentTimeMillis();
        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();

        Log.info("buildGraph started");

        // Fetch systems and jobs
        List<MonitoringSourceSystemDto> monitoringSources = fetchSourceSystems(nodeMap);
        fetchTargetSystems(nodeMap);
        List<MonitoringSyncJobDto> jobs = fetchSyncJobs(nodeMap, connections);

        // Add polling nodes from Kubernetes (or fallback)
        connectPollingNodesFromK8s(nodeMap, connections, monitoringSources, jobs);

        // Build response
        GraphResponse graph = new GraphResponse();
        graph.nodes = new ArrayList<>(nodeMap.values());
        graph.connections = connections;

        Log.info("buildGraph() completed in " + (System.currentTimeMillis() - globalStart) + " ms");
        return graph;
    }

    /**
     * Fetches source systems, creates nodes, and checks health status.
     */
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

    /**
     * Fetches target systems, creates nodes, and checks health status.
     */
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

    /**
     * Fetches sync jobs, creates nodes for each job, and adds connections for transformations.
     */
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

    /**
     * Handles a single transformation: connects source systems, polling nodes, and target systems.
     */
    private void handleTransformation(MonitoringTransformationDto tf, String syncNodeId,
                                      Map<String, NodeDto> nodeMap, List<NodeConnectionDto> connections) {
        Log.info("Transformation: " + tf.name);

        if (tf.pollingNodes != null) {
            for (String pollingNodeName : tf.pollingNodes) {
                String pollingNodeId = PREFIX_POLL + pollingNodeName;
                nodeMap.putIfAbsent(pollingNodeId,
                        createNode(pollingNodeId, TYPE_POLLINGNODE, pollingNodeName, STATUS_ACTIVE));

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

    /**
     * Connects polling nodes retrieved from Kubernetes. If none are found, falls back to a local node.
     */
    private void connectPollingNodesFromK8s(Map<String, NodeDto> nodeMap,
                                            List<NodeConnectionDto> connections,
                                            List<MonitoringSourceSystemDto> monitoringSources,
                                            List<MonitoringSyncJobDto> jobs) {
        long start = System.currentTimeMillis();
        try {
            List<String> pollingNodesFromK8s = getPollingNodesWithTimeout();

            if (!pollingNodesFromK8s.isEmpty()) {
                Log.info("PollingNodes in K8s: " + pollingNodesFromK8s);
                for (String pollingNodeName : pollingNodesFromK8s) {
                    addPollingNodeAndConnections(nodeMap, connections, monitoringSources, jobs, pollingNodeName, STATUS_ACTIVE);
                }
                return; // Kubernetes provided nodes, no fallback needed
            }

            fallbackToLocalPollingNode(nodeMap, connections, monitoringSources, jobs);

        } catch (Exception e) {
            Log.error("Error while retrieving PollingNodes from K8s", e);
            fallbackToLocalPollingNode(nodeMap, connections, monitoringSources, jobs);
        }
        logDuration("kubernetesPollingNodeService.getPollingNodes()", start);
    }

    /**
     * Fetches polling nodes from Kubernetes with a timeout (2s).
     */
    private List<String> getPollingNodesWithTimeout() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            return executor.submit(() -> kubernetesPollingNodeService.getPollingNodes())
                    .get(2000, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Fallback when Kubernetes polling nodes cannot be retrieved: use a local node.
     */
    private void fallbackToLocalPollingNode(Map<String, NodeDto> nodeMap,
                                            List<NodeConnectionDto> connections,
                                            List<MonitoringSourceSystemDto> monitoringSources,
                                            List<MonitoringSyncJobDto> jobs) {
        String localPollingNode = "core-polling-node"; // Fallback node
        boolean isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live");
        Log.warn("No PollingNodes found in K8s – falling back to local node: " + localPollingNode);

        addPollingNodeAndConnections(nodeMap, connections, monitoringSources, jobs,
                localPollingNode, isHealthy ? STATUS_ACTIVE : STATUS_ERROR);
    }

    /**
     * Adds a polling node to the graph and connects it to sources and jobs.
     */
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

    /**
     * Performs a health check for a system via Prometheus.
     */
    private boolean checkPrometheus(Long id, String apiUrl, String type) {
        long start = System.currentTimeMillis();
        boolean isHealthy = false;
        try {
            if (apiUrl != null) {
                isHealthy = prometheusClient.isUp(apiUrl);
            }
        } catch (Exception e) {
            Log.warnf(e, "Prometheus check failed for %s %s (%s)", type, id, apiUrl);
        }
        Log.info("Prometheus check for " + type + " " + id + " took " + (System.currentTimeMillis() - start) + " ms");
        return isHealthy;
    }

    /**
     * Wraps a fetch call with exception handling.
     */
    private <T> List<T> safeFetch(SupplierWithException<List<T>> supplier, String entityName) {
        try {
            List<T> result = supplier.get();
            Log.info(entityName + " loaded: " + result.size());
            return result;
        } catch (Exception e) {
            Log.error("Error fetching " + entityName, e);
            return Collections.emptyList();
        }
    }

    private void logDuration(String action, long start) {
        Log.info(action + " took " + (System.currentTimeMillis() - start) + " ms");
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
