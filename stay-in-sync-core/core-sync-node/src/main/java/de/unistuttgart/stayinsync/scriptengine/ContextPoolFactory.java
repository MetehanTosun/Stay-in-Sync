package de.unistuttgart.stayinsync.scriptengine;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ContextPoolFactory {
    private static final Logger LOG = Logger.getLogger(ContextPoolFactory.class);

    private final Map<String, ContextPool> pools = new ConcurrentHashMap<>();

    @Inject
    Config mpConfig;

    @ConfigProperty(name = "scriptengine.context.pool.size.default", defaultValue = "2")
    int defaultPoolSize;

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

    @PreDestroy
    public void cleanup(){
        LOG.info("Cleaning up all ContextPools before application shutdown...");
        pools.values().forEach(ContextPool::closeAllContexts);
        pools.clear();
        LOG.info("All ContextPools have been cleared.");
    }
}
