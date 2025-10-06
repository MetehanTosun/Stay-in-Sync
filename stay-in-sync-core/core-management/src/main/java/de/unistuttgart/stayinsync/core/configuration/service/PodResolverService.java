package de.unistuttgart.stayinsync.core.configuration.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class PodResolverService {
    @Inject
    KubernetesClient kubernetesClient;

    public String resolvePodName(String podName) {
        if(podName == null)
        {
            return "http://localhost:8091";
        }
        Pod pod = kubernetesClient.pods()
                .inNamespace("umbrella") // or use .inAnyNamespace()
                .withName(podName)
                .get();

        if (pod == null) {
            return "http://localhost:8091";
        }

        return "http://" + pod.getStatus().getPodIP() + ":8095";
    }
}
