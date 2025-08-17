-- Function to detect nested tables in logs
function parse_nested_log(tag, timestamp, record)
    local log_str = record["log"]
    local parsed = {}

    -- Detect JSON logs (e.g. Elasticsearch, ECS format)
    if log_str and log_str:match("^%s*{") then
        local ok, json = pcall(cjson.decode, log_str)
        if ok and type(json) == "table" then
            -- Copy JSON fields into the record
            for k,v in pairs(json) do
                record[k] = v
            end
            -- Normalize level field
            if record["log.level"] and not record["level"] then
                record["level"] = record["log.level"]
            end
            -- Normalize timestamp
            if record["@timestamp"] and not record["timestamp"] then
                record["timestamp"] = record["@timestamp"]
            end
            return 1, timestamp, record
        end
    end

    -- Extract known fields from the log string (key=value style)
    for k, v in string.gmatch(log_str, '([%w_]+)=("[^"]+"|[%w%p%a%d%-]+)') do
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
    if parsed["ts"] or parsed["t"] then
        record["timestamp"] = parsed["ts"] or parsed["t"]
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
    if parsed["error"] then
        record["error"] = parsed["error"]
    end

    -- Additional extracted fields
    if parsed["logger"] then
        record["logger"] = parsed["logger"]
    end
    if parsed["userId"] then
        record["userId"] = tonumber(parsed["userId"])
    end
    if parsed["orgId"] then
        record["orgId"] = tonumber(parsed["orgId"])
    end
    if parsed["uname"] then
        record["uname"] = parsed["uname"]
    end
    if parsed["method"] then
        record["method"] = parsed["method"]
    end
    if parsed["path"] then
        record["path"] = parsed["path"]
    end
    if parsed["status"] then
        record["status"] = tonumber(parsed["status"])
    end
    if parsed["remote_addr"] then
        record["remote_addr"] = parsed["remote_addr"]
    end
    if parsed["time_ms"] then
        record["time_ms"] = tonumber(parsed["time_ms"])
    end
    if parsed["duration"] then
        record["duration"] = parsed["duration"]
    end
    if parsed["size"] then
        record["size"] = tonumber(parsed["size"])
    end
    if parsed["handler"] then
        record["handler"] = parsed["handler"]
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


