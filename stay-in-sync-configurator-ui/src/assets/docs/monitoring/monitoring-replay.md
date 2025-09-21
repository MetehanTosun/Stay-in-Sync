## Snapshot Creation
Whenever an error occurs in the **ScriptEngine**, a **snapshot** is created.  
This snapshot captures the state and relevant execution data at the time of failure.

## Visualization in the Graph
- The affected **SyncNode** is highlighted in **red**.
- Clicking on the SyncNode opens the available snapshots.

## Snapshot Replay
Snapshots can be replayed to:

- Reproduce and analyze the error
- Validate fixes
- Reprocess the failed transformation  
