package de.unistuttgart.stayinsync.syncnode.syncjob;

import io.quarkus.cache.CacheKeyGenerator;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.reflect.Method;

/**
 * Generates a cache key for the DirectiveExecutor's CHECK requests.
 * The key is based on the full, unique URL string that is passed as a parameter
 * to the cached method. This ensures that identical requests to the same endpoint
 * with the same parameters are cached correctly.
 */
@ApplicationScoped
public class UrlCacheKeyGenerator implements CacheKeyGenerator {
    /**
     * Generates the cache key.
     *
     * @param method The method being invoked (cachedCheckRequest).
     * @param params The parameters passed to the method. We expect the full URL
     *               string (`fullUrlForCache`) to be among them.
     * @return The full URL string to be used as the cache key.
     */
    @Override
    public Object generate(Method method, Object... params) {
        for (Object param : params) {
            if (param instanceof String) {
                return param;
            }
        }
        throw new IllegalArgumentException("Could not generate cache key: No String parameter (fullUrlForCache) found.");
    }
}
