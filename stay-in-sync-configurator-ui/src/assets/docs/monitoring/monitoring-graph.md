# Monitoring Graph & Metrics

The **Monitoring Graph** provides a visual overview of:

- **Source systems**
- **Target systems**
- **PollingNodes**
- **SyncNodes**
- **Connections** between these nodes

---

## 🩺 Health Checks & Visualization
All systems (source, target, polling nodes) are continuously monitored.  
The status of the nodes is represented by **color codes**:

- **Green** → active
- **Red** → Error
- **Yellow** → inactive

### 🔎 Legend
The legend can be folded in and out and explains shapes and colors:

**Formen**

- 🔵 **Circle** → SyncNode / PollingNode
- 🔺 **Triangle** → SourceSystem / ASS
- 🟦 **Square** → TargetSystem

**Colors**
- 🟩 Green → active
- 🟥 Red → error
- 🟨 Yellow → inactive

---

##  Metrics
The **Metrics tab** displays system statistics for all active services.  
This is done via an **embedded Grafana dashboard** with free navigation, filtering, and customizable charts.

###  General Metrics
- CPU utilization
- Number of threads
- RabbitMQ channels
- Heap memory

###  PollingNodes
- Number of requests

###  SyncNodes
- Script load
- Execution times

---

##  Filtering in the graph
Filtering can be done directly via the graphs:

- Click on a **PollingNode** → shows only its request count
- Click on a **SyncNode** → shows only script-related metrics
- Click on **free area** → filter is removed

---
