function parse_nested_log(tag, timestamp, record)
    local log_str = record["log"]
    local ok, data = pcall(loadstring("return " .. log_str))
    if ok and type(data) == "table" then
        if data["log"] then
            record["log"] = data["log"]
        end
        if data["level"] then
            record["level"] = data["level"]
        end
        if data["ts"] then
            record["timestamp"] = data["ts"]
        elseif data["time"] then
            record["timestamp"] = data["time"]
        end
        if data["msg"] then
            record["message"] = data["msg"]
        end
    end
    return 1, timestamp, record
end
