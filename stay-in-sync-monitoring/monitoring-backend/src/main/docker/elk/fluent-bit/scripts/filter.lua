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

    -- mdc auswerten
    local mdc = record["mdc"]
    if type(mdc) == "table" then
        -- scriptId extrahieren und anpassen
        local scriptId = mdc["scriptId"]
        if scriptId then
            local adjustedScriptId = string.match(scriptId, "script%-for%-(%d+)")
            if adjustedScriptId then
                record["scriptId"] = adjustedScriptId
                print("scriptId angepasst: " .. adjustedScriptId)
            else
                print("scriptId konnte nicht angepasst werden")
            end
        else
            print("scriptId nicht gefunden im mdc")
        end

        -- transformationId extrahieren
        local transformationId = mdc["transformationId"]
        if transformationId then
            local adjustedTransformationId = string.match(transformationId, "(%d+)")
            if adjustedTransformationId then
                record["transformationId"] = adjustedTransformationId
                print("transformationId extrahiert: " .. adjustedTransformationId)
            else
                print("transformationId konnte nicht angepasst werden")
            end
        else
            print("transformationId nicht gefunden im mdc")
        end
    else
        print("mdc ist nicht vorhanden oder kein Table")
    end

    return 1, timestamp, record
end
