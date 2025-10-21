package de.unistuttgart.stayinsync.syncnode.syncjob;

import io.quarkus.arc.DefaultBean;
import io.quarkus.logging.Log;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;

/**
 * A provider responsible for creating and configuring a shared, application-wide Vert.x WebClient.
 * <p>
 * The purpose of this class is to centralize the creation and configuration of the HTTP client
 * used throughout the application. Centralization ensures that all HTTP calls adhere to the
 * <p>
 * same standards for timeouts, SSL/TLS security, and other connection properties. This
 * class is designed to be configurable via the application's properties file, allowing for
 * different settings in various environments (e.g., development, testing, production) without
 * code changes.
 * <p>
 * It replaces the default constructor-based creation with a CDI {@code @Produces} method,
 * which is the idiomatic way in Quarkus and Jakarta CDI to provide configured instances of a bean.
 */
@ApplicationScoped
public class WebClientProvider {

    /**
     * Produces a customized and configured {@link WebClient} bean for the application.
     * <p>
     * This producer method is invoked by the CDI container to create a single, shared instance
     * of the WebClient. It reads configuration properties to set up timeouts and, critically,
     * the SSL/TLS trust store. In production environments, it should be configured with a
     * proper PEM trust store to securely connect to external systems. For development,
     * it can be configured to trust all certificates, but this is logged as a security warning.
     *
     * @param vertx             The Vert.x instance, injected by Quarkus.
     * @param connectTimeout    The configured connection timeout.
     * @param trustAll          A flag to disable certificate validation (for development only).
     * @param pemTrustStorePath An optional path to a PEM file containing trusted certificates.
     * @return A configured, singleton {@link WebClient} instance.
     */
    @Produces
    @ApplicationScoped
    @DefaultBean // Ensures this bean can be easily overridden for testing purposes.
    public WebClient createWebClient(
            Vertx vertx,
            @ConfigProperty(name = "stayinsync.webclient.timeout") Duration connectTimeout,
            @ConfigProperty(name = "stayinsync.webclient.security.trust-all", defaultValue = "false") boolean trustAll,
            @ConfigProperty(name = "stayinsync.webclient.security.pem-trust-store") Optional<String> pemTrustStorePath
    ) {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout((int) connectTimeout.toMillis())
                .setSsl(true);

        configureTlsSecurity(options, trustAll, pemTrustStorePath);

        Log.info("Configured WebClient with connect timeout of " + connectTimeout);
        return WebClient.create(vertx, options);
    }

    /**
     * Configures the TLS security options for the WebClient.
     * It prioritizes a PEM trust store if provided. If not, it falls back to the `trustAll`
     * flag. Using `trustAll` in a non-development environment will produce a prominent warning.
     *
     * @param options           The WebClientOptions to configure.
     * @param trustAll          The configured boolean flag for trusting all certs.
     * @param pemTrustStorePath The Optional path to the PEM trust store.
     */
    private void configureTlsSecurity(WebClientOptions options, boolean trustAll, Optional<String> pemTrustStorePath) {
        if (pemTrustStorePath.isPresent() && !pemTrustStorePath.get().isBlank()) {
            // Priority 1: Use a specific PEM trust store if configured. This is the recommended approach.
            String path = pemTrustStorePath.get();
            Log.infof("Configuring WebClient TLS with PEM trust store at: %s", path);
            options.setPemTrustOptions(new PemTrustOptions().addCertPath(path));
            options.setTrustAll(false); // Ensure trustAll is disabled if a trust store is used.
        } else if (trustAll) {
            // Priority 2: Fall back to trustAll if configured. Issue a strong warning.
            Log.warn("SECURITY WARNING: WebClient is configured with 'trustAll=true'. " +
                    "SSL/TLS certificate validation is DISABLED. This is insecure and should ONLY be used for local development.");
            options.setTrustAll(true);
        } else {
            // Default: Use the default JVM trust store.
            Log.info("Configuring WebClient TLS with default JVM trust store. 'trustAll' is false and no PEM trust store is specified.");
            options.setTrustAll(false);
        }
    }
}