# monitoring-backend

This project provides the backend for the monitoring service and is built with [Quarkus](https://quarkus.io/), the Supersonic Subatomic Java Framework.

The backend exposes runtime and system metrics that can be scraped by **Prometheus** and visualized in **Grafana**.

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

## Running the application in dev mode

You can run the application in dev mode with live coding enabled using:

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

## REST Endpoints

The backend also exposes REST endpoints that can be used to integrate with other services.  
Refer to the Quarkus documentation and project code for examples.