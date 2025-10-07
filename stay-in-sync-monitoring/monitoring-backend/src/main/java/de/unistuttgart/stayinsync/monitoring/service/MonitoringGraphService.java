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

/**
 * MonitoringGraphService is responsible for building a monitoring graph representation
 * of the entire synchronization ecosystem.
 *
 * It fetches:
 * - Source systems
 * - Target systems
 * - Sync jobs and their transformations
 * - Polling nodes
 *
 * Then constructs a graph consisting of nodes and connections that represent
 * the data flow and system health.
 */
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

    /**
     * Builds the full monitoring graph by aggregating nodes and connections.
     *
     * @return A {@link GraphResponse} containing all nodes and their connections.
     */
    public GraphResponse buildGraph() {
        Map<String, NodeDto> nodeMap = new HashMap<>();
        List<NodeConnectionDto> connections = new ArrayList<>();

        // 1. Fetch all source systems and add them as nodes
        for (MonitoringSourceSystemDto src : sourceSystemClient.getAll()) {
            boolean isHealthy = prometheusClient.isUp(src.apiUrl);
            nodeMap.put("SRC_" + src.id,
                    createNode("SRC_" + src.id, "SourceSystem", src.name, isHealthy ? "active" : "error"));
        }

        Log.info(nodeMap.toString());

        // 2. Fetch all target systems and add them as nodes
        for (MonitoringTargetSystemDto tgt : targetSystemClient.getAll()) {
            boolean isHealthy = prometheusClient.isUp(tgt.apiUrl);
            nodeMap.put("TGT_" + tgt.id,
                    createNode("TGT_" + tgt.id, "TargetSystem", tgt.name, isHealthy ? "active" : "error"));
        }

        Log.info(nodeMap.toString());

        // 3. Fetch all sync jobs and add them along with their transformations
        List<MonitoringSyncJobDto> jobs = syncJobClient.getAll();
        Log.info("Jobs: " + jobs);

        for (MonitoringSyncJobDto job : jobs) {
            String syncNodeId = job.id.toString();
            nodeMap.put(syncNodeId, createNode(syncNodeId, "SyncNode", job.name, "active"));

            // Add transformations for each job
            if (job.transformations != null) {
                for (MonitoringTransformationDto tf : job.transformations) {
                    Log.info(tf.name);

                    Log.info("SourceSystems:" + tf.sourceSystemIds);
                    Log.info("TargetSystems:" + tf.targetSystemIds);
                    Log.info("PollingNodes:" + tf.pollingNodes);

                    //Add PollingNodes
                    if (tf.pollingNodes != null) {
                        for (String pollingNodeName : tf.pollingNodes) {
                            String pollingNodeId = "POLL_" + pollingNodeName;
                            // TODO: Replace with WorkerPodName when running on Kubernetes
                            boolean isHealthy = prometheusClient.isUp("http://host.docker.internal:8095/q/health/live"); // Placeholder
                            nodeMap.putIfAbsent(pollingNodeId,
                                    createNode(pollingNodeId, "PollingNode", pollingNodeName,
                                            isHealthy ? "active" : "error"));

                            // Create connection: SourceSystem → PollingNode
                            for (Long srcId : tf.sourceSystemIds) {
                                connections.add(createConnection("SRC_" + srcId, pollingNodeId, "active"));
                            }

                            // Create connection: PollingNode → SyncNode
                            connections.add(createConnection(pollingNodeId, syncNodeId, "active"));
                        }
                    }

                    // Create connections: SyncNode → TargetSystems
                    for (Long tgtId : tf.targetSystemIds) {
                        connections.add(createConnection(syncNodeId, "TGT_" + tgtId, "active"));
                    }
                }
            }
        }

        // Assemble final graph response
        GraphResponse graph = new GraphResponse();
        graph.nodes = new ArrayList<>(nodeMap.values());
        graph.connections = connections;
        return graph;
    }

    //Helper method to create a new node.

    private NodeDto createNode(String id, String type, String label, String status) {
        NodeDto node = new NodeDto();
        node.id = id;
        node.type = type;
        node.label = label;
        node.status = status;
        return node;
    }

    //Helper method to create a connection between two nodes.


    private NodeConnectionDto createConnection(String sourceId, String targetId, String status) {
        NodeConnectionDto conn = new NodeConnectionDto();
        conn.source = sourceId;
        conn.target = targetId;
        conn.status = status;
        return conn;
    }
}
