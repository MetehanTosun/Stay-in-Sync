# Monitoring Graph & Metrics

Der **Monitoring Graph** bietet eine visuelle Übersicht über:

- **Source systems**
- **Target systems**
- **PollingNodes**
- **SyncNodes**
- **Connections** between these nodes

---

## 🩺 Health Checks & Visualization
Alle Systeme (Source, Target, PollingNodes) werden kontinuierlich überwacht.  
Der Status der Knoten wird durch **Farbcodes** dargestellt:

-  **Grün** → aktiv
-  **Rot** → Fehler
-  **Gelb** → inaktiv

### 🔎 Legende
Die Legende ist **ein- und ausklappbar** und erklärt Formen & Farben:

**Formen**

- 🔵 **Kreis** → SyncNode / PollingNode
- 🔺 **Dreieck** → SourceSystem / ASS
- 🟦 **Quadrat** → TargetSystem

**Farben**
- 🟩 Grün → aktiv
- 🟥 Rot → Fehler
- 🟨 Gelb → inaktiv

---

##  Metrics
Im **Metrics Tab** werden Systemstatistiken aller aktiven Services angezeigt.  
Dies erfolgt über ein **eingebettetes Grafana-Dashboard** mit freier Navigation, Filterung und individuellen Diagrammen.

###  Allgemeine Metriken
- CPU-Auslastung
- Thread-Anzahl
- RabbitMQ-Channels
- Heap-Memory

###  PollingNodes
- Request-Anzahl

###  SyncNodes
- Script Load
- Ausführungszeiten

---

##  Filtering im Graph
Die Filterung kann direkt über den Graphen erfolgen:

- Klick auf einen **PollingNode** → zeigt nur dessen Request-Anzahl
- Klick auf einen **SyncNode** → zeigt nur scriptbezogene Metriken
- Klick auf **freien Bereich** → Filter wird entfernt

---
