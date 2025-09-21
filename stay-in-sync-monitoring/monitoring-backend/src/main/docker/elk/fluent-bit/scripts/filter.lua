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
    end

    -- level aus message extrahieren
    local level = string.match(msg_str, '"level"%s*:%s*"([^"]+)"')
    if level then
        record["level"] = level
    end

    -- mdc auswerten
    local mdc = record["mdc"]
    if type(mdc) == "table" then
        -- scriptId extrahieren und anpassen
        local scriptId = mdc["scriptId"]
        if scriptId then
            local adjustedScriptId = string.match(scriptId, "script%-for%-(%d+)")
            if adjustedScriptId then
                record["scriptId"] = adjustedScriptId
            end
        end

        -- transformationId extrahieren
        local transformationId = mdc["transformationId"]
        if transformationId then
            local adjustedTransformationId = string.match(transformationId, "(%d+)")
            if adjustedTransformationId then
                record["transformationId"] = adjustedTransformationId
            end
        end
    end

    return 1, timestamp, record
end
