package de.unistuttgart.stayinsync.monitoring.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Service for discovering polling nodes in a Kubernetes cluster.
 *
 * <p>
 * This service queries the Kubernetes API (via the Fabric8 client) for
 * pods that belong to the "core-polling-node" application. It returns
 * the names of those pods, which can be used to identify active polling
 * nodes running in the cluster.
 * </p>
 *
 * <p>
 * The namespace is configurable via the property
 * {@code kubernetes.namespace}, defaulting to {@code default}.
 * </p>
 */
@ApplicationScoped
public class KubernetesPollingNodeService {

    /** Fabric8 Kubernetes client used to interact with the cluster. */
    @Inject
    KubernetesClient kubernetesClient;

    /** Namespace to search for polling node pods (defaults to "default"). */
    @ConfigProperty(name = "kubernetes.namespace", defaultValue = "default")
    String namespace;

    /**
     * Retrieves the names of all polling node pods in the configured namespace.
     *
     * <p>
     * Pods are selected based on the Kubernetes label:
     * {@code app.kubernetes.io/name=core-polling-node}.
     * </p>
     *
     * @return list of pod names representing polling nodes
     */
    public List<String> getPollingNodes() {
        List<Pod> pods = kubernetesClient.pods()
                .inNamespace(namespace)
                .withLabel("app.kubernetes.io/name", "core-polling-node")
                .list()
                .getItems();

        return pods.stream()
                .map(p -> p.getMetadata().getName())
                .toList();
    }
}
