-- Funktion zur Erkennung verschachtelter Tabellen
function parse_nested_log(tag, timestamp, record)
    local log_str = record["log"]
    local parsed = {}

    -- Extrahiere bekannte Felder aus dem Log-String
    for k, v in string.gmatch(log_str, '([%w_]+)=("[^"]+"|[%w%.%-]+)') do
        v = v:gsub('^"(.-)"$', '%1')
        parsed[k] = v
    end

    -- Setze extrahierte Felder ins Record
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

    -- Severity-Mapping anhand Level
    if parsed["level"] == "error" then
        record["severity"] = "critical"
    elseif parsed["level"] == "warn" then
        record["severity"] = "moderate"
    else
        record["severity"] = "light"
    end

    return 1, timestamp, record
end
