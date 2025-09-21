# Stay-in-Sync Monitoring Backend

This module provides the backend for central monitoring.  
It is based on Quarkus and integrates with tools such as Prometheus, Grafana, Loki, and RabbitMQ.

The backend exposes runtime and system metrics that can be scraped by Prometheus and visualized in Grafana.  
A detailed list of available metrics and their descriptions can be found in [`Metrics.md`](./docs/Metrics.md).  
This document is located at:

```
/stay-in-sync-monitoring/monitoring-backend/docs/Metrics.md
```

It can be used as a reference for writing Prometheus queries.

---

## Prerequisites

Make sure you have the following tools installed before running the `monitoring-backend` project:

- **JDK**: Java 17 or higher  
  Check installation:
  ```bash
  java -version
  ```

- **Maven Wrapper**: The project uses `./mvnw`, so Maven does not need to be installed globally.  
  If missing, generate it with:
  ```bash
  mvn -N io.takari:maven:wrapper
  ```

- **Docker**: Required for Quarkus DevServices and Testcontainers  
  Install from: [Docker Desktop](https://www.docker.com/products/docker-desktop)  
  Check installation:
  ```bash
  docker info
  ```

- **Database**: Optional, only needed if not using Docker DevServices.  
  Example configuration can be found in `application.properties`.

---

## Local Development Start

Before starting any services, ensure that **Docker Desktop** is running.

1. Build dependencies:
   ```bash
   cd stay-in-sync-core/core-sync-node
   mvn clean install -Dquarkus.package.type=fast-jar

   cd ../core-polling-node
   mvn clean install -Dquarkus.package.type=fast-jar
   ```

2. Start the monitoring backend:
   ```bash
   cd stay-in-sync-monitoring/monitoring-backend
   docker compose up -d
   ./mvnw quarkus:dev
   ```

3. Once the Quarkus dev server is up, you can access:
    - Swagger UI: [http://localhost:8093/q/swagger-ui/#/](http://localhost:8093/q/swagger-ui/#/)
    - Prometheus query interface: [http://localhost:9090/query](http://localhost:9090/query)
    - Prometheus target status: [http://localhost:9090/targets](http://localhost:9090/targets)
    - Grafana dashboards (Login `admin` / `admin`): [http://localhost:3000](http://localhost:3000)
    - Loki connection check: [http://localhost:3100/ready](http://localhost:3100/ready)

Once the containers are running, Prometheus will start scraping the configured targets and Grafana will visualize available metrics accordingly.

---

## Start RabbitMQ

To start RabbitMQ, navigate to the following directory and run:

```bash
cd stay-in-sync-monitoring/monitoring-backend/src/main/docker/elk
docker compose up -d
```

Service access point:

- RabbitMQ Management UI: [http://localhost:15672/#/](http://localhost:15672/#/)

RabbitMQ is used for processing asynchronous monitoring and logging events.

---

## Running the application in dev mode

You can run the application in Quarkus dev mode with live coding enabled using:

```bash
./mvnw quarkus:dev
```

> **Note**: Quarkus provides a Dev UI, available only in dev mode, at <http://localhost:8080/q/dev/>.

---

## Packaging and running the application

To package the application, run:

```bash
./mvnw package
```

This produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.  
The application is now runnable using:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

To build an **über-jar** (all dependencies bundled in a single JAR):

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

Run it using:

```bash
java -jar target/*-runner.jar
```

---

## Creating a native executable

You can create a native executable using:

```bash
./mvnw package -Dnative
```

If GraalVM is not installed locally, build inside a container instead:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

The resulting executable can then be run directly:

```bash
./target/monitoring-backend-1.0.0-SNAPSHOT-runner
```

For more information about building native executables, see the [Quarkus Maven guide](https://quarkus.io/guides/maven-tooling).

---

## Metrics Reference

All metrics exposed by the monitoring backend can be found in [`Metrics.md`](./docs/Metrics.md).  
This file includes:

- HTTP server metrics (requests, bytes read/written, connection durations)
- JVM metrics (memory, threads, garbage collection, buffer pools)
- Netty metrics (memory, thread-local caches, executor tasks)
- Probe metrics (HTTP response times, DNS lookups, SSL status, success/failure)
- Process metrics (CPU usage, file descriptors, uptime)
- RabbitMQ metrics (messages published/consumed, connections, channels)
- Scrape metrics (Prometheus scraping stats)
- System metrics (CPU count/usage, load average)
- Worker pool metrics (active, idle, queue size, resource usage)

This is the definitive reference for creating Prometheus queries and configuring Grafana dashboards.
