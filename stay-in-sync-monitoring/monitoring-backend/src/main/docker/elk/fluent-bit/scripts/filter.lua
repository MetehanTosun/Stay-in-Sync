function parse_nested_log(tag, timestamp, record)
    -- Prüfen, ob message existiert
    local msg_str = record["message"]
    if type(msg_str) ~= "string" then
        return 1, timestamp, record
    end

    -- syncJobId aus message extrahieren
    local syncJobId = string.match(msg_str, '"syncJobId"%s*:%s*"([^"]+)"')
    if syncJobId then
        record["syncJobId"] = syncJobId
        print("syncJobId extrahiert: " .. syncJobId)
    else
        print("syncJobId nicht gefunden im JSON")
    end

    -- level aus message extrahieren
    local level = string.match(msg_str, '"level"%s*:%s*"([^"]+)"')
    if level then
        record["level"] = level
        print("level extrahiert: " .. level)
    else
        print("level nicht gefunden im JSON")
    end

    -- scriptId aus mdc extrahieren
    local mdc = record["mdc"]
    if type(mdc) == "table" then
        local scriptId = mdc["scriptId"]
        if scriptId then
            record["scriptId"] = scriptId
            print("scriptId extrahiert: " .. scriptId)
        else
            print("scriptId nicht gefunden im mdc")
        end
    else
        print("mdc ist nicht vorhanden oder kein Table")
    end

    return 1, timestamp, record
end