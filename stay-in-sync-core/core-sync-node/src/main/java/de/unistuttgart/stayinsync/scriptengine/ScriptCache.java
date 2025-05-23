package de.unistuttgart.stayinsync.scriptengine;

import jakarta.enterprise.context.ApplicationScoped;
import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ScriptCache {

    private final Map<String, Source> cache = new ConcurrentHashMap<>();

    public Source getScript(String scriptId, String scriptHash) {
        return cache.get(buildKey(scriptId, scriptHash));
    }

    public boolean containsScript(String scriptId, String scriptHash) {
        return cache.containsKey(buildKey(scriptId, scriptHash));
    }

    public void putScript(String scriptId, String scriptHash, String scriptCode) {
        try {
            String wrappedScriptCode = "(function() {\n" +
                    "   \"use strict\";\n" +
                    scriptCode +
                    "\n})();";

            Source source = Source.newBuilder("js", wrappedScriptCode, scriptId)
                    .build();
            cache.put(buildKey(scriptId, scriptHash), source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to generate a unique key per version of a compiled script.
     *
     * @param scriptId   the ID of the script
     * @param scriptHash the generated unique hash of the script
     * @return returns a concatenation of the scriptId and the hash
     */
    private String buildKey(String scriptId, String scriptHash) {
        return scriptId + ":" + scriptHash;
    }
}
