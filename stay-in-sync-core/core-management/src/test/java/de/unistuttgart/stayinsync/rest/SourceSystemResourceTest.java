package de.unistuttgart.stayinsync.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;



import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.RpcClient.Response;

import jakarta.ws.rs.core.MediaType;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementWebException;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemEndpointMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.*;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemEndpointService;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class SourceSystemResourceTest {

        @InjectMock
        SourceSystemService ssService;
    
        @InjectMock
        SourceSystemEndpointService endpointService;
    
        @InjectMock
        SourceSystemMapper ssMapper;
    
        @InjectMock
        SourceSystemEndpointMapper endpointMapper;
    
        private SourceSystem entity;
        private SourceSystemDto dto;
    
        @BeforeEach
        public void setup() {
            entity = new SourceSystem();
            entity.id = 42L;
            entity.name = "TestSys";
            entity.description = "Desc";
            entity.setType(SourceSystemType.REST_OPENAPI);
            entity.setApiUrl("http://foo");
            entity.setAuthType(AuthType.API_KEY);
            dto = new SourceSystemDto(
                42L, "TestSys", "Desc",
                SourceSystemType.REST_OPENAPI, "http://foo",
                AuthType.API_KEY, null, null, null, null, null
            );
        }
    
        @Test
        public void testGetAll() {
            when(ssService.findAllSourceSystems()).thenReturn(List.of(entity));
            when(ssMapper.toDto(entity)).thenReturn(dto);
    
            given()
              .when().get("/api/source-systems")
              .then()
                 .statusCode(200)
                 .contentType(MediaType.APPLICATION_JSON)
                 .body("[0].id", is(42))
                 .body("[0].name", is("TestSys"));
        }
    
        @Test
        public void testGetByIdFound() {
            when(ssService.findSourceSystemById(42L)).thenReturn(Optional.of(entity));
            when(ssMapper.toDto(entity)).thenReturn(dto);
    
            given()
              .when().get("/api/source-systems/42")
              .then()
                 .statusCode(200)
                 .body("id", is(42))
                 .body("name", is("TestSys"));
        }
    
        @Test
        public void testGetByIdNotFound() {
            when(ssService.findSourceSystemById(99L)).thenReturn(Optional.empty());
    
            given()
              .when().get("/api/source-systems/99")
              .then()
                 .statusCode(404);
        }
    
        @Test
        public void testCreateJson() {
            var input = new CreateSourceSystemJsonDTO(
                null, "New", SourceSystemType.REST_OPENAPI,
                "http://x", AuthType.API_KEY, null, null, null, null, null
            );
            when(ssMapper.toEntity(input)).thenReturn(entity);
            // after persist Quarkus sets id=42
            when(ssMapper.toDto(entity)).thenReturn(dto);
    
            given()
              .contentType(MediaType.APPLICATION_JSON)
              .body(input)
              .when().post("/api/source-systems")
              .then()
                 .statusCode(201)
                 .header("Location", containsString("/api/source-systems/42"))
                 .body("id", is(42))
                 .body("name", is("TestSys"));
        }
    
        @Test
        public void testUpdate() {
            var updatedDto = new SourceSystemDto(
                42L, "Upd", "D", SourceSystemType.REST_OPENAPI,
                "u", AuthType.API_KEY, null, null, null, null, null
            );
            SourceSystem updEntity = new SourceSystem();
            updEntity.id = 42L;
            when(ssMapper.toEntity(updatedDto)).thenReturn(updEntity);
            when(ssService.updateSourceSystem(updEntity)).thenReturn(Optional.of(updEntity));
            when(ssMapper.toDto(updEntity)).thenReturn(updatedDto);
    
            given()
              .contentType(MediaType.APPLICATION_JSON)
              .body(updatedDto)
              .when().put("/api/source-systems/42")
              .then()
                 .statusCode(200)
                 .body("name", is("Upd"));
        }
    
        @Test
        public void testDelete() {
            when(ssService.deleteSourceSystemById(42L)).thenReturn(true);
    
            given()
              .when().delete("/api/source-systems/42")
              .then()
                 .statusCode(204);
        }
    
        @Test
        public void testListEndpoints() {
            var ep = new SourceSystemEndpoint(); ep.id = 7L; ep.endpointPath = "/foo"; ep.httpRequestType = "GET";
            var epDto = new SourceSystemEndpointDto(7L, "/foo", "GET", false, 0, null, null);
            when(endpointService.listBySourceId(42L)).thenReturn(List.of(ep));
            when(endpointMapper.toDto(ep)).thenReturn(epDto);
    
            given()
              .when().get("/api/source-systems/42/endpoints")
              .then()
                 .statusCode(200)
                 .body("[0].id", is(7))
                 .body("[0].endpointPath", is("/foo"));
        }
    
        @Test
        public void testDiscoverEndpoints() {
            var disc = new DiscoveredEndpoint("/pets","GET");
            when(endpointService.discoverAllEndpoints(42L)).thenReturn(List.of(disc));
    
            given()
              .when().get("/api/source-systems/42/discover")
              .then()
                 .statusCode(200)
                 .body("[0].path", is("/pets"))
                 .body("[0].method", is("GET"));
        }

        @Test
        public void testDiscoverEndpointsEmpty() {
            // Simulate no endpoints discovered
            when(endpointService.discoverAllEndpoints(42L)).thenReturn(List.of());

            given()
              .when().get("/api/source-systems/42/discover")
              .then()
                 .statusCode(200)
                 .body("", hasSize(0));
        }

    
        @Test
        public void testCreateEndpoint() {
            var input = new SourceSystemEndpointDto(null, "/bar", "POST", false, 0, null, null);
            var ep = new SourceSystemEndpoint(); ep.id = 5L; ep.endpointPath = "/bar"; ep.httpRequestType = "POST";
            var epOut = new SourceSystemEndpointDto(5L, "/bar", "POST", false, 0, null, null);
    
            when(endpointService.createEndpoint(42L, "/bar", "POST")).thenReturn(ep);
            when(endpointMapper.toDto(ep)).thenReturn(epOut);
    
            given()
              .contentType(MediaType.APPLICATION_JSON)
              .body(input)
              .when().post("/api/source-systems/42/endpoints")
              .then()
                 .statusCode(201)
                 .header("Location", containsString("/api/source-systems/42/endpoints/5"))
                 .body("id", is(5))
                 .body("endpointPath", is("/bar"));
        }
    
        /** 
        @Test
        public void testExtractSchema() {
            var ep = new SourceSystemEndpoint(); ep.id = 9L; ep.jsonSchema = "{}"; ep.schemaMode = "auto";
            var epDto2 = new SourceSystemEndpointDto(9L, null, null, false, 0, null, null);
    
            when(endpointService.extractSchema(9L)).thenReturn(ep);
            when(endpointMapper.toDto(ep)).thenReturn(epDto2);
    
            given()
              .when().post("/api/source-systems/42/endpoints/9/extract")
              .then()
                 .statusCode(200)
                 .body("jsonSchema", is("{}"))
                 .body("schemaMode", is("auto"));
        }
        */
        @Test
        public void testExtractSchemaNotFound() {
            when(endpointService.extractSchema(99L))
                .thenThrow(new CoreManagementWebException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "Endpoint not found", "no"));
    
            given()
              .when().post("/api/source-systems/42/endpoints/99/extract")
              .then()
                 .statusCode(404);
        }
    }