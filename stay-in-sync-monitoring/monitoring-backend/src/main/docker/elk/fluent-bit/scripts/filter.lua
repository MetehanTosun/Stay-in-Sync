-- Function to detect nested tables in logs
function parse_nested_log(tag, timestamp, record)
    local log_str = record["log"]
    local parsed = {}

    -- Extract known fields from the log string
    for k, v in string.gmatch(log_str, '([%w_]+)=("[^"]+"|[%w%.%-]+)') do
        v = v:gsub('^"(.-)"$', '%1')  -- Remove quotes from string values
        parsed[k] = v
    end

    -- Set extracted fields into the record
    if parsed["msg"] then
        record["message"] = parsed["msg"]
    end
    if parsed["level"] then
        record["level"] = parsed["level"]
    end
    if parsed["ts"] then
        record["timestamp"] = parsed["ts"]
    end
    if parsed["traceID"] then
        record["traceID"] = parsed["traceID"]
    end
    if parsed["component"] then
        record["component"] = parsed["component"]
    end
    if parsed["latency"] then
        record["latency"] = parsed["latency"]
    end
    if parsed["query"] then
        record["query"] = parsed["query"]
    end
    if parsed["caller"] then
        record["caller"] = parsed["caller"]
    end
    if parsed["message"] then
        record["message"] = parsed["message"]
    end
    if parsed["error"] then
        record["error"] = parsed["error"]
    end

    -- Additional fields for metrics extraction
    if parsed["request_count"] then
        record["request_count"] = tonumber(parsed["request_count"])
    end
    if parsed["cpu_load"] then
        record["cpu_load"] = tonumber(parsed["cpu_load"])
    end
    if parsed["script_name"] then
        record["script_name"] = parsed["script_name"]
    end
    if parsed["script_duration"] then
        record["script_duration"] = tonumber(parsed["script_duration"])
    end
    if parsed["source_system"] then
        record["source_system"] = parsed["source_system"]
    end
    if parsed["target_system"] then
        record["target_system"] = parsed["target_system"]
    end

    -- Severity mapping based on level
    if parsed["level"] == "error" then
        record["severity"] = "critical"
    elseif parsed["level"] == "warn" then
        record["severity"] = "moderate"
    else
        record["severity"] = "light"
    end

    return 1, timestamp, record
end
