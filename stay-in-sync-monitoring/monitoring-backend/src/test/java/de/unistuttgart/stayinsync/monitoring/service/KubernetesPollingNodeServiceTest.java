package de.unistuttgart.stayinsync.monitoring.service;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@QuarkusTest
class KubernetesPollingNodeServiceTest {

    KubernetesClient kubernetesClient;
    KubernetesPollingNodeService service;

    @BeforeEach
    void setUp() {
        kubernetesClient = mock(KubernetesClient.class);
        service = new KubernetesPollingNodeService();
        service.kubernetesClient = kubernetesClient; // direkt setzen
        service.namespace = "default";               // ConfigProperty simulieren
    }

    @Test
    void getPollingNodes_shouldReturnPodNames() {
        // Arrange
        Pod pod1 = new Pod();
        pod1.setMetadata(new ObjectMeta());
        pod1.getMetadata().setName("poller-1");

        Pod pod2 = new Pod();
        pod2.setMetadata(new ObjectMeta());
        pod2.getMetadata().setName("poller-2");

        PodList podList = new PodList();
        podList.setItems(List.of(pod1, pod2));

        var podsOperation = mock(MixedOperation.class);
        var inNamespaceOperation = mock(NonNamespaceOperation.class);
        var withLabelOperation = mock(FilterWatchListDeletable.class);

        when(kubernetesClient.pods()).thenReturn(podsOperation);
        when(podsOperation.inNamespace("default")).thenReturn(inNamespaceOperation);
        when(inNamespaceOperation.withLabel("app.kubernetes.io/name", "core-polling-node"))
                .thenReturn(withLabelOperation);
        when(withLabelOperation.list()).thenReturn(podList);

        // Act
        List<String> result = service.getPollingNodes();

        // Assert
        assertThat(result).containsExactlyInAnyOrder("poller-1", "poller-2");
    }

    @Test
    void getPollingNodes_shouldReturnEmptyListWhenNoPodsFound() {
        // Arrange
        PodList podList = new PodList();
        podList.setItems(List.of());

        var podsOperation = mock(MixedOperation.class);
        var inNamespaceOperation = mock(NonNamespaceOperation.class);
        var withLabelOperation = mock(FilterWatchListDeletable.class);

        when(kubernetesClient.pods()).thenReturn(podsOperation);
        when(podsOperation.inNamespace("default")).thenReturn(inNamespaceOperation);
        when(inNamespaceOperation.withLabel("app.kubernetes.io/name", "core-polling-node"))
                .thenReturn(withLabelOperation);
        when(withLabelOperation.list()).thenReturn(podList);

        // Act
        List<String> result = service.getPollingNodes();

        // Assert
        assertThat(result).isEmpty();
    }
}
