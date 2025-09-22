# 02_logs.md – Log Aggregation, Display, and Filtering

### Architecture
- All logs from the components are sent via **TCP** to **FluentBit**.
- FluentBit enriches logs with labels and forwards them to **Loki**.

## LogTab in the UI
The **LogTab** displays logs queried from Loki.  
Available filtering options include:

- Log level (INFO, WARN, ERROR, …)
- Service
- Transformation IDs
- Start and end time

**Technical details:**
- A maximum of **5000 logs** are loaded per query.
- Default timeframe is **the last hour**.

## Filtering via Graph
The graph is also integrated into log filtering:

- Click on a **SyncNode** → logs are filtered by its **Transformation IDs**.

---
