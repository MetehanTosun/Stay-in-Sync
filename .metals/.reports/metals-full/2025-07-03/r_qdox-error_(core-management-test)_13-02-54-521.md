error id: file://<WORKSPACE>/stay-in-sync-core/core-management/src/test/java/de/unistuttgart/stayinsync/rest/SourceSystemResourceTest.java
file://<WORKSPACE>/stay-in-sync-core/core-management/src/test/java/de/unistuttgart/stayinsync/rest/SourceSystemResourceTest.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[67,36]

error in qdox parser
file content:
```java
offset: 2295
uri: file://<WORKSPACE>/stay-in-sync-core/core-management/src/test/java/de/unistuttgart/stayinsync/rest/SourceSystemResourceTest.java
text:
```scala
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

    /**
     * Test that getting all SourceSystems returns an empty list initially.
     */
    @Test
    public void testGetAllEmpty() {
        given()
                .when().get("/api/config/source-system")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    /**
     * Test creating a new SourceSystem and then retrieving it by ID.
     */
    @Test
    public void testCreateAndGetById() {
        String jsonBody = """
                {
                    "name": "TestSensor",
                    "description": "Test Description",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:1234"
                }
                """;

        // Create a new SourceSystem via POST
        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/s@@ource-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        // Extract ID from the Location header
        String id = location.substring(location.lastIndexOf("/") + 1);

        // Retrieve the created SourceSystem by ID and verify fields
        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("TestSensor"))
                .body("description", equalTo("Test Description"))
                .body("apiUrl", equalTo("http://localhost:1234"));
    }

    /**
     * Test updating an existing SourceSystem.
     */
    @Test
    public void testUpdate() {
        String jsonBodyCreate = """
                {
                    "name": "SensorBeforeUpdate",
                    "description": "Description before update",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:1111"
                }
                """;

        // Create a new SourceSystem
        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBodyCreate)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        String jsonBodyUpdate = """
                {
                    "id": %s, 
                    "name": "SensorAfterUpdate",
                    "description": "Description after update",
                    "apiType": "REST",
                    "apiUrl": "http://localhost:2222"
                }
                """.formatted(id);

        // Update the SourceSystem via PUT
        given()
                .contentType(ContentType.JSON)
                .body(jsonBodyUpdate)
                .when().put("/api/config/source-system/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("SensorAfterUpdate"))
                .body("description", equalTo("Description after update"))
                .body("apiType", equalTo("REST"))
                .body("apiUrl", equalTo("http://localhost:2222"));
    }

    /**
     * Test deleting an existing SourceSystem and verify it no longer exists.
     */
    @Test
    public void testDelete() {
        String jsonBody = """
                {
                    "name": "ToDelete",
                    "description": "Will be deleted",
                    "apiType": "REST",
                    "apiUrl": "http://localhost/delete"
                }
                """;

        // Create a new SourceSystem
        String location = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/api/config/source-system")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");

        String id = location.substring(location.lastIndexOf("/") + 1);

        // Delete the SourceSystem
        given()
                .when().delete("/api/config/source-system/" + id)
                .then()
                .statusCode(204);

        // Verify it no longer exists
        given()
                .when().get("/api/config/source-system/" + id)
                .then()
                .statusCode(404);
    }

    /**
     * Test retrieving a SourceSystem by an ID that does not exist returns 404.
     */
    @Test
    public void testGetByIdNotFound() {
        given()
                .when().get("/api/config/source-system/9999999")
                .then()
                .statusCode(404);
    }

    /**
     * Test deleting a SourceSystem by an ID that does not exist returns 404.
     */
    @Test
    public void testDeleteNotFound() {
        given()
                .when().delete("/api/config/source-system/9999999")
                .then()
                .statusCode(404);
    }
}
```

```



#### Error stacktrace:

```
com.thoughtworks.qdox.parser.impl.Parser.yyerror(Parser.java:2025)
	com.thoughtworks.qdox.parser.impl.Parser.yyparse(Parser.java:2147)
	com.thoughtworks.qdox.parser.impl.Parser.parse(Parser.java:2006)
	com.thoughtworks.qdox.library.SourceLibrary.parse(SourceLibrary.java:232)
	com.thoughtworks.qdox.library.SourceLibrary.parse(SourceLibrary.java:190)
	com.thoughtworks.qdox.library.SourceLibrary.addSource(SourceLibrary.java:94)
	com.thoughtworks.qdox.library.SourceLibrary.addSource(SourceLibrary.java:89)
	com.thoughtworks.qdox.library.SortedClassLibraryBuilder.addSource(SortedClassLibraryBuilder.java:162)
	com.thoughtworks.qdox.JavaProjectBuilder.addSource(JavaProjectBuilder.java:174)
	scala.meta.internal.mtags.JavaMtags.indexRoot(JavaMtags.scala:49)
	scala.meta.internal.metals.SemanticdbDefinition$.foreachWithReturnMtags(SemanticdbDefinition.scala:99)
	scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:489)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7(Indexer.scala:361)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7$adapted(Indexer.scala:356)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1306)
	scala.collection.parallel.ParIterableLike$Foreach.leaf(ParIterableLike.scala:938)
	scala.collection.parallel.Task.$anonfun$tryLeaf$1(Tasks.scala:52)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.util.control.Breaks$$anon$1.catchBreak(Breaks.scala:97)
	scala.collection.parallel.Task.tryLeaf(Tasks.scala:55)
	scala.collection.parallel.Task.tryLeaf$(Tasks.scala:49)
	scala.collection.parallel.ParIterableLike$Foreach.tryLeaf(ParIterableLike.scala:935)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal(Tasks.scala:169)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal$(Tasks.scala:156)
	scala.collection.parallel.AdaptiveWorkStealingForkJoinTasks$AWSFJTWrappedTask.internal(Tasks.scala:304)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.compute(Tasks.scala:149)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.compute$(Tasks.scala:148)
	scala.collection.parallel.AdaptiveWorkStealingForkJoinTasks$AWSFJTWrappedTask.compute(Tasks.scala:304)
	java.base/java.util.concurrent.RecursiveAction.exec(RecursiveAction.java:194)
	java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
	java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
	java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
	java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
	java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
```
#### Short summary: 

QDox parse error in file://<WORKSPACE>/stay-in-sync-core/core-management/src/test/java/de/unistuttgart/stayinsync/rest/SourceSystemResourceTest.java