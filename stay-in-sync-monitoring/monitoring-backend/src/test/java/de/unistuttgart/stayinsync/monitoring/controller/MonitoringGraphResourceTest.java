package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.MonitoringGraphService;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.GraphResponse;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.NodeDto;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.NodeConnectionDto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;

@QuarkusTest
class MonitoringGraphResourceTest {

    @InjectMock
    MonitoringGraphService monitoringGraphService;

    @Test
    void getGraph_shouldReturnGraphResponse() {
        // Arrange: Dummy GraphResponse
        NodeDto node = new NodeDto();
        node.id = "1";
        node.label = "Node1";
        node.type = "SourceSystem";
        node.status = "active";

        NodeConnectionDto edge = new NodeConnectionDto();
        edge.source = "1";
        edge.target = "2";
        edge.status = "active";

        GraphResponse graph = new GraphResponse();
        graph.nodes = List.of(node);
        graph.connections = List.of(edge);

        when(monitoringGraphService.buildGraph()).thenReturn(graph);

        // Act + Assert
        given()
                .when()
                .get("/api/monitoringgraph")
                .then()
                .statusCode(200)
                .body("nodes", hasSize(1))
                .body("nodes[0].id", equalTo("1"))
                .body("nodes[0].label", equalTo("Node1"))
                .body("nodes[0].status", equalTo("active"))
                .body("connections", hasSize(1))
                .body("connections[0].source", equalTo("1"))
                .body("connections[0].target", equalTo("2"))
                .body("connections[0].status", equalTo("active"));

        verify(monitoringGraphService).buildGraph();
    }
}

