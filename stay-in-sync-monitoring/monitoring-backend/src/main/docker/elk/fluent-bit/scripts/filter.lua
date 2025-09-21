function parse_nested_log(tag, timestamp, record)
    -- Check whether message exists
    local msg_str = record["message"]
    if type(msg_str) ~= "string" then
        return 1, timestamp, record
    end

    -- Extract syncJobId from message
    local syncJobId = string.match(msg_str, '"syncJobId"%s*:%s*"([^"]+)"')
    if syncJobId then
        record["syncJobId"] = syncJobId
        print("syncJobId extracted: " .. syncJobId)
    else
        print("syncJobId not found in JSON")
    end

    -- extract level from message
    local level = string.match(msg_str, '"level"%s*:%s*"([^"]+)"')
    if level then
        record["level"] = level
        print("level extrahiert: " .. level)
    else
        print("Level not found in JSON")
    end

    -- evaluate mdc
    local mdc = record["mdc"]
    if type(mdc) == "table" then
        -- Extract and customize scriptId
        local scriptId = mdc["scriptId"]
        if scriptId then
            local adjustedScriptId = string.match(scriptId, "script%-for%-(%d+)")
            if adjustedScriptId then
                record["scriptId"] = adjustedScriptId
                print("scriptId adjusted: " .. adjustedScriptId)
            else
                print("scriptId could not be customized")
            end
        else
            print("scriptId not found in mdc")
        end

        -- Extract transformationId
        local transformationId = mdc["transformationId"]
        if transformationId then
            local adjustedTransformationId = string.match(transformationId, "(%d+)")
            if adjustedTransformationId then
                record["transformationId"] = adjustedTransformationId
                print("transformationId extracted: " .. adjustedTransformationId)
            else
                print("transformationId could not be customized")
            end
        else
            print("transformationId not found in mdc")
        end
    else
        print("mdc does not exist or is not a table")
    end

    return 1, timestamp, record
end
