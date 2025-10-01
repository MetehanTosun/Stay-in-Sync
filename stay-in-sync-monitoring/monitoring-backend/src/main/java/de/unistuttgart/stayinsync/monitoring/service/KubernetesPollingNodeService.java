package de.unistuttgart.stayinsync.monitoring.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class KubernetesPollingNodeService {

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "kubernetes.namespace", defaultValue = "default")
    String namespace;

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
