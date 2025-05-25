package de.unistuttgart.stayinsync.scriptengine;

import jakarta.enterprise.context.ApplicationScoped;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ScriptCache {
    private static final Logger LOG = Logger.getLogger(ScriptCache.class);
    private final Map<String, Source> cache = new ConcurrentHashMap<>();

    public Source getScript(String scriptId, String scriptHash) {
        return cache.get(buildKey(scriptId, scriptHash));
    }

    // TODO: Extension point for Python / C-Scripts as language specific caches.
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
            LOG.debugf("Cached script: %s (with hash: %s)", scriptId, scriptHash);
        } catch (IOException e) {
            LOG.errorf(e, "IOException while building source for script %s", scriptId);
        } catch (PolyglotException e) {
            // TODO: Parsing/compile errors might require additional handling / features
            LOG.errorf(e, "Error parsing/pre-compiling script %s: %s", scriptId, e.getMessage());
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error putting script into cache: %s", scriptId);
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
