error id: file://<WORKSPACE>/stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/Examples.java
file://<WORKSPACE>/stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/Examples.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[66,1]

error in qdox parser
file content:
```java
offset: 1876
uri: file://<WORKSPACE>/stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/Examples.java
text:
```scala
package de.unistuttgart.stayinsync.core.configuration.rest;

final class Examples {

    public static final String VALID_ENDPOINT_PARAM_POST = """
            {
              "paramName": "userId",
              "queryParamType": "QUERY",
              "values": ["123", "456", "789"]
            }
                """;
    public static final String VALID_API_HEADER_POST = """
            {
              "name": "getUserA", 
              "used": "false", 
              "pollingIntervallInMs": "3"
            }
            """;
    public static final String VALID_API_HEADER_VALUE = """
            """;
    public static final String VALID_ENDPOINT_PARAM_VALUE_POST = """
            """;
    public static final String VALID_EXAMPLE_REQUEST_CONFIGURATION_CREATE = """
             {
              "name": "getUserA", 
              "used": "false", 
              "pollingIntervallInMs": "3"
            }
            """;
    public static final String VALID_EXAMPLE_ENDPOINT_CREATE = """
            [{
              "endpointPath": "/test/user", 
              "httpRequestType": "GET"
            },
            {
              "endpointPath": "/test/car", 
              "httpRequestType": "POST"
            }]
            """;
    public static final String VALID_QUERY_PARAM_CREATE = """
            {
              "paramName": "userId",
              "queryParamType": "QUERY",
              "values": ["123", "456", "789"]
            }""";

    private Examples() {

    }

    static final String VALID_EXAMPLE_SYNCJOB = """
            {
              "id": "1",
              "name": "Basyx sync job", 
              "deployed": false
            }
            """;

    static final String VALID_EXAMPLE_SYNCJOB_TO_CREATE = """
            {
              "name": "Basic Userdata sync",
              "deployed": true
            }
            """;

 @@   static final String VALID_SOURCE_SYSTEM_CREATE = """
            {"name": "Sync Produktion B","apiUrl": "http://localhost:4200", "description": "this is my simple api", "apiType": "REST",  "authConfig": {"authType": "BASIC","username": "admin","password": "secretpassword123"}}
            """;

    static final String VALID_SOURCE_SYSTEM_ENDPOINT_POST = """
            {
              "endpointPath": "/test",
              "httpRequestType": "GET"
            }
            """;

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

QDox parse error in file://<WORKSPACE>/stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/Examples.java