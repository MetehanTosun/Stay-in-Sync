package de.unistuttgart.stayinsync.core.configuration.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.logging.Log;
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
            Log.warnf("Unable to resolve pod %s", podName);
            return "http://localhost:8091";
        }
        String podIP = pod.getStatus().getPodIP();
        Log.infof("Resolved pod name %s to %s", podName, podIP);

        return "http://" + podIP + ":8091";
    }
}
