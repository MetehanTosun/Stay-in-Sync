package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.PrometheusClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SourceSystemClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.monitoring.clientinterfaces.TargetSystemClient;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class MonitoringGraphServiceTest {

    @InjectMock
    @RestClient
    SourceSystemClient sourceClient;

    @InjectMock
    @RestClient
    TargetSystemClient targetClient;

    @InjectMock
    @RestClient
    SyncJobClient jobClient;

    @InjectMock
    PrometheusClient prometheusClient;

    @InjectMock
    KubernetesPollingNodeService kubernetesPollingNodeService;

    @Inject
    MonitoringGraphService service;

    @Test
    void buildGraph_shouldBuildNodesAndConnections() {
        // Arrange
        MonitoringSourceSystemDto src = new MonitoringSourceSystemDto();
        src.id = 1L;
        src.name = "SourceA";
        src.apiUrl = "http://src";
        when(sourceClient.getAll()).thenReturn(List.of(src));

        MonitoringTargetSystemDto tgt = new MonitoringTargetSystemDto();
        tgt.id = 99L;
        tgt.name = "TargetX";
        tgt.apiUrl = "http://tgt";
        when(targetClient.getAll()).thenReturn(List.of(tgt));

        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 42L;
        job.name = "SyncJob1";

        MonitoringTransformationDto tf = new MonitoringTransformationDto();
        tf.name = "TransformationA";
        tf.sourceSystemIds = List.of(1L);
        tf.targetSystemIds = List.of(99L);
        tf.pollingNodes = List.of("poller");
        job.transformations = List.of(tf);

        when(jobClient.getAll()).thenReturn(List.of(job));
        when(prometheusClient.isUp(anyString())).thenReturn(true);

        // Keine K8s-PollingNodes für diesen Test
        when(kubernetesPollingNodeService.getPollingNodes()).thenReturn(Collections.emptyList());

        // Act
        GraphResponse graph = service.buildGraph();

        // Assert
        assertThat(graph.nodes).extracting("id")
                .containsExactlyInAnyOrder("SRC_1", "TGT_99", "42", "POLL_poller");
        assertThat(graph.connections).isNotEmpty();
        verify(prometheusClient, atLeastOnce()).isUp(anyString());
    }

    @Test
    void buildGraph_shouldHandleEmptySystemsAndJobs() {
        // Arrange: keine Systeme, keine Jobs
        when(sourceClient.getAll()).thenReturn(Collections.emptyList());
        when(targetClient.getAll()).thenReturn(Collections.emptyList());
        when(jobClient.getAll()).thenReturn(Collections.emptyList());
        when(kubernetesPollingNodeService.getPollingNodes()).thenReturn(Collections.emptyList());

        // Act
        GraphResponse graph = service.buildGraph();

        // Assert
        assertThat(graph.nodes).isEmpty();
        assertThat(graph.connections).isEmpty();
    }

    @Test
    void buildGraph_shouldMarkUnhealthyNodes() {
        // Arrange
        MonitoringSourceSystemDto src = new MonitoringSourceSystemDto();
        src.id = 10L;
        src.name = "SourceB";
        src.apiUrl = "http://bad-src";
        when(sourceClient.getAll()).thenReturn(List.of(src));

        when(targetClient.getAll()).thenReturn(Collections.emptyList());
        when(jobClient.getAll()).thenReturn(Collections.emptyList());
        when(kubernetesPollingNodeService.getPollingNodes()).thenReturn(Collections.emptyList());

        // ungesund
        when(prometheusClient.isUp("http://bad-src")).thenReturn(false);

        // Act
        GraphResponse graph = service.buildGraph();

        // Assert
        assertThat(graph.nodes).hasSize(1);
        NodeDto node = graph.nodes.get(0);
        assertThat(node.id).isEqualTo("SRC_10");
        assertThat(node.status).isEqualTo("error");
    }

    @Test
    void buildGraph_shouldSupportMultipleTransformations() {
        // Arrange
        MonitoringSourceSystemDto src = new MonitoringSourceSystemDto();
        src.id = 1L;
        src.name = "SourceA";
        src.apiUrl = "http://src";
        when(sourceClient.getAll()).thenReturn(List.of(src));

        MonitoringTargetSystemDto tgt1 = new MonitoringTargetSystemDto();
        tgt1.id = 2L;
        tgt1.name = "Target1";
        tgt1.apiUrl = "http://tgt1";

        MonitoringTargetSystemDto tgt2 = new MonitoringTargetSystemDto();
        tgt2.id = 3L;
        tgt2.name = "Target2";
        tgt2.apiUrl = "http://tgt2";

        when(targetClient.getAll()).thenReturn(List.of(tgt1, tgt2));

        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 100L;
        job.name = "MultiJob";

        MonitoringTransformationDto tf1 = new MonitoringTransformationDto();
        tf1.name = "TF1";
        tf1.sourceSystemIds = List.of(1L);
        tf1.targetSystemIds = List.of(2L);
        tf1.pollingNodes = List.of("poller1");

        MonitoringTransformationDto tf2 = new MonitoringTransformationDto();
        tf2.name = "TF2";
        tf2.sourceSystemIds = List.of(1L);
        tf2.targetSystemIds = List.of(3L);
        tf2.pollingNodes = List.of("poller2");

        job.transformations = List.of(tf1, tf2);

        when(jobClient.getAll()).thenReturn(List.of(job));
        when(prometheusClient.isUp(anyString())).thenReturn(true);
        when(kubernetesPollingNodeService.getPollingNodes()).thenReturn(Collections.emptyList());

        // Act
        GraphResponse graph = service.buildGraph();

        // Assert
        assertThat(graph.nodes).extracting("id")
                .contains("SRC_1", "TGT_2", "TGT_3", "100", "POLL_poller1", "POLL_poller2");

        assertThat(graph.connections)
                .anyMatch(c -> c.source.equals("SRC_1") && c.target.equals("POLL_poller1"))
                .anyMatch(c -> c.source.equals("SRC_1") && c.target.equals("POLL_poller2"))
                .anyMatch(c -> c.source.equals("POLL_poller1") && c.target.equals("100"))
                .anyMatch(c -> c.source.equals("POLL_poller2") && c.target.equals("100"))
                .anyMatch(c -> c.source.equals("100") && c.target.equals("TGT_2"))
                .anyMatch(c -> c.source.equals("100") && c.target.equals("TGT_3"));
    }

    @Test
    void buildGraph_shouldIncludeKubernetesPollingNodes() {
        // Arrange
        MonitoringSourceSystemDto src = new MonitoringSourceSystemDto();
        src.id = 1L;
        src.name = "SourceA";
        src.apiUrl = "http://src";
        when(sourceClient.getAll()).thenReturn(List.of(src));

        MonitoringSyncJobDto job = new MonitoringSyncJobDto();
        job.id = 42L;
        job.name = "Job42";
        job.transformations = Collections.emptyList();
        when(jobClient.getAll()).thenReturn(List.of(job));

        when(targetClient.getAll()).thenReturn(Collections.emptyList());
        when(prometheusClient.isUp(anyString())).thenReturn(true);

        // Kubernetes-PollingNodes mocken
        when(kubernetesPollingNodeService.getPollingNodes()).thenReturn(List.of("k8sPoller1", "k8sPoller2"));

        // Act
        GraphResponse graph = service.buildGraph();

        // Assert: K8s-PollingNodes existieren
        assertThat(graph.nodes).extracting("id")
                .contains("SRC_1", "42", "POLL_k8sPoller1", "POLL_k8sPoller2");

        // Assert: Verbindungen von Source → K8s-Poller → Job
        assertThat(graph.connections)
                .anyMatch(c -> c.source.equals("SRC_1") && c.target.equals("POLL_k8sPoller1"))
                .anyMatch(c -> c.source.equals("SRC_1") && c.target.equals("POLL_k8sPoller2"))
                .anyMatch(c -> c.source.equals("POLL_k8sPoller1") && c.target.equals("42"))
                .anyMatch(c -> c.source.equals("POLL_k8sPoller2") && c.target.equals("42"));
    }
}
