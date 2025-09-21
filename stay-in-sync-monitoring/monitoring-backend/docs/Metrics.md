# Available Metrics

Our daemon exposes various metrics that can be queried through Prometheus and visualized in Grafana dashboards.  
Below, the metrics are grouped by category. 
Each entry shows the metric name, its type, and a description.


### Metric Types
- **Counter**: A cumulative metric that increases monotonically (e.g., total number of requests). Counters reset when the process restarts.
- **Gauge**: A metric that represents a single numerical value that can increase or decrease (e.g., memory usage, number of active requests).

---

## HTTP Server Metrics
- **http_server_active_requests** – Current number of in-progress HTTP requests being handled.
- **http_server_bytes_read_max** – Maximum number of bytes received in a single request.
- **http_server_bytes_written_max** – Maximum number of bytes sent in a single response.
- **http_server_connections_seconds_max** – Maximum observed duration of an HTTP connection.
- **http_server_requests_seconds_max** – Maximum observed request processing time.

---

## JVM Metrics
### Buffers
- **jvm_buffer_count_buffers** – Estimated number of direct and mapped buffers in the JVM buffer pool.
- **jvm_buffer_memory_used_bytes** – Amount of memory currently used by the JVM buffer pool.
- **jvm_buffer_total_capacity_bytes** – Total capacity of all buffers in the JVM buffer pool.

### Classes
- **jvm_classes_loaded_classes** – Number of classes currently loaded in the JVM.
- **jvm_classes_unloaded_classes_total** – Total number of classes unloaded since JVM start.

### Garbage Collection
- **jvm_gc_live_data_size_bytes** – Size of long-lived (tenured) heap memory after garbage collection.
- **jvm_gc_max_data_size_bytes** – Maximum size of long-lived (tenured) heap memory.
- **jvm_gc_memory_allocated_bytes_total** – Total memory allocated in the young generation after garbage collections.
- **jvm_gc_memory_promoted_bytes_total** – Total bytes promoted from young to old generation during garbage collection.
- **jvm_gc_overhead** – Fraction of CPU time spent on garbage collection relative to total CPU time.
- **jvm_gc_pause_seconds_max** – Maximum observed pause time due to garbage collection.

### Memory
- **jvm_memory_committed_bytes** – Amount of memory guaranteed to be available to the JVM.
- **jvm_memory_max_bytes** – Maximum amount of memory that can be used by the JVM.
- **jvm_memory_usage_after_gc** – Ratio of used memory to maximum memory in long-lived pools after the last garbage collection.
- **jvm_memory_used_bytes** – Amount of memory currently used by the JVM.

### Threads
- **jvm_threads_daemon_threads** – Number of active daemon threads.
- **jvm_threads_live_threads** – Total number of currently active threads (daemon and non-daemon).
- **jvm_threads_peak_threads** – Peak number of threads since JVM start or last reset.
- **jvm_threads_started_threads_total** – Total number of threads started since JVM start.
- **jvm_threads_states_threads** – Number of threads in each thread state (e.g., RUNNABLE, BLOCKED).

---

## Netty Metrics
- **netty_allocator_memory_pinned** – Amount of memory pinned by the Netty allocator.
- **netty_allocator_memory_used** – Amount of memory currently used by the Netty allocator.
- **netty_allocator_pooled_arenas** – Number of pooled memory arenas managed by Netty.
- **netty_allocator_pooled_cache_size** – Size of thread-local caches in pooled arenas.
- **netty_allocator_pooled_chunk_size** – Size of chunks in pooled arenas.
- **netty_allocator_pooled_threadlocal_caches** – Number of thread-local caches used by Netty.
- **netty_eventexecutor_tasks_pending** – Number of tasks currently waiting in the Netty event executor.

---

## Probe Metrics
- **probe_dns_lookup_time_seconds** – Duration of DNS lookup during probe execution.
- **probe_duration_seconds** – Total time required to complete the probe.
- **probe_failed_due_to_regex** – Indicates if probe failed because the response did not match the expected regex.
- **probe_http_content_length** – Size of the HTTP response content in bytes.
- **probe_http_duration_seconds** – Total duration of HTTP requests including all redirects.
- **probe_http_redirects** – Number of HTTP redirects followed.
- **probe_http_ssl** – Indicates whether SSL/TLS was used in the final request.
- **probe_http_status_code** – HTTP status code returned by the probe.
- **probe_http_uncompressed_body_length** – Length of the HTTP response body after decompression.
- **probe_http_version** – Version of HTTP protocol used in the response.
- **probe_ip_addr_hash** – Hash of the resolved IP address (useful to detect IP changes).
- **probe_ip_protocol** – IP protocol used for the probe (IPv4 or IPv6).
- **probe_success** – Indicates whether the probe succeeded (1 = success, 0 = failure).

---

## Process Metrics
- **process_cpu_time_ns_total** – Total accumulated CPU time used by the JVM process, in nanoseconds.
- **process_cpu_usage** – Recent CPU usage percentage of the JVM process.
- **process_files_max_files** – Maximum number of file descriptors available to the process.
- **process_files_open_files** – Number of currently open file descriptors.
- **process_start_time_seconds** – Start time of the JVM process since Unix epoch.
- **process_uptime_seconds** – Total uptime of the JVM process.

---

## RabbitMQ Metrics
- **rabbitmq_acknowledged_published_total** – Number of acknowledged published messages.
- **rabbitmq_acknowledged_total** – Total number of acknowledged messages.
- **rabbitmq_channels** – Current number of open channels.
- **rabbitmq_connections** – Current number of open connections.
- **rabbitmq_consumed_total** – Total number of consumed messages.
- **rabbitmq_failed_to_publish_total** – Number of failed publishing attempts.
- **rabbitmq_not_acknowledged_published_total** – Number of published messages not acknowledged.
- **rabbitmq_published_total** – Total number of published messages.
- **rabbitmq_rejected_total** – Number of rejected messages.
- **rabbitmq_requeued_total** – Number of requeued messages.
- **rabbitmq_unrouted_published_total** – Number of published messages that were not routed.

---

## Scrape Metrics
- **scrape_duration_seconds** – Time taken by the last Prometheus scrape.
- **scrape_samples_post_metric_relabeling** – Number of samples remaining after relabeling.
- **scrape_samples_scraped** – Total number of samples scraped.
- **scrape_series_added** – Number of new time series added in the last scrape.

---

## System Metrics
- **system_cpu_count** – Number of CPUs available to the JVM.
- **system_cpu_usage** – Recent CPU usage percentage of the host system.
- **system_load_average_1m** – Average system load over the last 1 minute.
- **up** – Target availability: 1 if the target is up, 0 if it is down.

---

## Worker Pool Metrics
- **worker_pool_active** – Number of resources currently in use from the worker pool.
- **worker_pool_completed_total** – Total number of times resources were acquired from the pool.
- **worker_pool_idle** – Number of idle resources currently available in the pool.
- **worker_pool_queue_delay_seconds_max** – Maximum observed waiting time for tasks in the queue.
- **worker_pool_queue_size** – Current number of tasks waiting in the queue.
- **worker_pool_ratio** – Ratio of used resources to total available resources in the pool.
- **worker_pool_rejected_total** – Number of tasks rejected due to pool limits.
- **worker_pool_usage_seconds_max** – Maximum observed time a resource was held by a task.  