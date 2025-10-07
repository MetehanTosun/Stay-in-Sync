# Log Aggregation, Display & Filtering

---

## 🏗️ Architektur
- Alle Logs der Komponenten werden über **TCP** an **FluentBit** gesendet.
- **FluentBit** reichert die Logs mit Labels an und leitet sie an **Loki** weiter.

---

## 🖥️ LogTab in der UI
Das **LogTab** zeigt Logs, die aus **Loki** abgefragt werden.  
Es bietet verschiedene **Filteroptionen**:

- 🏷️ **Log Level** (INFO, WARN, ERROR, …)
- ⚙️ **Service**
- 🔄 **Transformation IDs**
- ⏱️ **Start- und Endzeit**

### 📌 Technische Details
- Pro Abfrage werden maximal **5000 Logs** geladen.
- Standardzeitraum: **letzte Stunde**.

---

## 🔍 Filtering via Graph
Der **Graph** ist ebenfalls in das Log-Filtering integriert:

- Klick auf einen **SyncNode** → zeigt nur Logs mit den entsprechenden **Transformation IDs**.

---
