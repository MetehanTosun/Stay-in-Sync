package de.unistuttgart.stayinsync.core.configuration.service.aas;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RequestHeadersContext {
    private static final ThreadLocal<Map<String, String>> CTX = ThreadLocal.withInitial(HashMap::new);

    private RequestHeadersContext() {}

    public static void set(Map<String, String> headers) {
        CTX.set(headers != null ? new HashMap<>(headers) : new HashMap<>());
    }

    public static Map<String, String> get() {
        Map<String, String> m = CTX.get();
        return m != null ? Collections.unmodifiableMap(m) : Collections.emptyMap();
    }

    public static void clear() {
        CTX.remove();
    }
}
