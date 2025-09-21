# Monitoring Graph and Metrics

The **Monitoring Graph** provides a visual overview of:

- **Source systems**
- **Target systems**
- **PollingNodes**
- **SyncNodes**
- **Connections** between these nodes

## Health Checks & Visualization
Source systems, target systems, and polling nodes are continuously monitored via health checks.  
Node status is represented with color codes:

- **Green (#4caf50)** → active
- **Red (#f44336)** → error
- **Yellow (#ffeb3b)** → inactive

An **expandable legend** explains shapes and colors:

- **Shapes**
  - Circle → SyncNode / PollingNode
  - Triangle → SourceSystem / ASS
  - Square → TargetSystem

- **Colors**
  - Green → active
  - Red → error
  - Yellow → inactive

The legend can be toggled open and closed at any time.

## Metrics
A **Metrics tab** displays system-level statistics for all active services.  
This is powered by an **embedded Grafana dashboard**, which allows free navigation, filtering, and custom chart creation.

- **General metrics**:
  - CPU usage
  - Thread count
  - RabbitMQ channels
  - Heap memory

- **PollingNodes**:
  - Request count

- **SyncNodes**:
  - Script load
  - Execution times

## Filtering via Graph
Filtering can be done directly in the graph:

- Click on a **PollingNode** → shows only its request count
- Click on a **SyncNode** → shows only its script-related metrics
- Click on empty space next to the graph → removes filter

---


