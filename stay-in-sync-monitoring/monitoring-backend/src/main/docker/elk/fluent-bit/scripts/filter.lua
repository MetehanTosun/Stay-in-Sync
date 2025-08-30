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
        -- Optional: Debug-Ausgabe
        print("syncJobId extrahiert: " .. syncJobId)
    else
        print("syncJobId nicht gefunden im JSON")
    end

    -- level aus message extrahieren (z. B. "level":"error")
    local level = string.match(msg_str, '"level"%s*:%s*"([^"]+)"')
    if level then
        record["level"] = level
        -- Optional: Debug-Ausgabe
        print("level extrahiert: " .. level)
    else
        print("level nicht gefunden im JSON")
    end

    return 1, timestamp, record
end
