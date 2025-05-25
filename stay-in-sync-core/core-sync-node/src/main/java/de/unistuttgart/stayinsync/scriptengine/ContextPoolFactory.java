package de.unistuttgart.stayinsync.scriptengine;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory class responsible for creating and managing {@link ContextPool} instances.
 * This factory ensures that for each requested scripting language, a single {@link ContextPool}
 * is instantiated and reused. The size of each pool can be configured via MicroProfile Config.
 *
 * <p>This class is {@link ApplicationScoped}, meaning a single instance is created for the
 * application lifecycle. It also handles the cleanup of all created context pools when the
 * application shuts down using the {@link PreDestroy} lifecycle callback.</p>
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
@ApplicationScoped
public class ContextPoolFactory {
    private static final Logger LOG = Logger.getLogger(ContextPoolFactory.class);

    /**
     * A thread-safe map to store and retrieve {@link ContextPool} instances.
     * The key is the lowercase language identifier (e.g., "js"), and the value is the
     * corresponding {@link ContextPool}.
     */
    private final Map<String, ContextPool> pools = new ConcurrentHashMap<>();

    /**
     * Injected MicroProfile Config instance to access application configuration properties.
     * Used to determine the pool size for specific languages.
     */
    @Inject
    Config mpConfig;

    /**
     * The default size for a {@link ContextPool} if no language-specific size is configured.
     * This value is injected from MicroProfile Config, with a default of "2" if the property
     * "scriptengine.context.pool.size.default" is not set.
     */
    @ConfigProperty(name = "scriptengine.context.pool.size.default", defaultValue = "2")
    int defaultPoolSize;

    /**
     * Retrieves or creates a {@link ContextPool} for the specified scripting language.
     * <p>
     * If a pool for the given {@code languageId} (case-insensitive) already exists, it is returned.
     * Otherwise, a new {@link ContextPool} is created. The size of the new pool is determined by:
     * <ol>
     *     <li>Checking for a MicroProfile Config property named {@code scriptengine.context.pool.size.<languageId>}
     *         (e.g., {@code scriptengine.context.pool.size.js}).</li>
     *     <li>If the specific property is not found, the {@link #defaultPoolSize} is used.</li>
     * </ol>
     * The created pool is then stored for future requests for the same language.
     * </p>
     *
     * @param languageId The identifier of the scripting language (e.g., "js", "python").
     *                   This is treated case-insensitively for map lookups.
     * @return The {@link ContextPool} for the specified language.
     */
    public ContextPool getPool(String languageId) {
        String languageKey = languageId.toLowerCase();
        return pools.computeIfAbsent(languageKey, lang -> {
            String configKey = "scriptengine.context.pool.size." + lang;
            int poolSize = mpConfig.getOptionalValue(configKey, Integer.class).orElse(defaultPoolSize);
            LOG.infof("Creating ContextPool for language '%s' with size %d (default size: %d, config key: %s)",
                    lang, poolSize, defaultPoolSize, configKey);
            return new ContextPool(lang, poolSize);
        });
    }

    /**
     * Cleans up all managed {@link ContextPool} instances.
     * This method is automatically invoked by the CDI container when the application is shutting down,
     * due to the {@link PreDestroy} annotation. It iterates through all created context pools
     * and calls their {@link ContextPool#closeAllContexts()} method to release GraalVM context resources.
     * After closing all contexts, it clears the internal map of pools.
     */
    @PreDestroy
    public void cleanup() {
        LOG.info("Cleaning up all ContextPools before application shutdown...");
        pools.values().forEach(ContextPool::closeAllContexts);
        pools.clear();
        LOG.info("All ContextPools have been cleared.");
    }
}
