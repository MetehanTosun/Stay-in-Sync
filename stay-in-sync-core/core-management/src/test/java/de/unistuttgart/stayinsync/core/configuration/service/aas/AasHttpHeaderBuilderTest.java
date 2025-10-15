package de.unistuttgart.stayinsync.core.configuration.service.aas;

/**
 * Unit tests for {@link de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder}.
 * Verifies correct construction of HTTP headers for both read and write modes,
 * including Basic Authentication and content type handling.
 */
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.BasicAuthConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AasHttpHeaderBuilderTest {

    /**
     * Tests that the HttpHeaderBuilder correctly creates headers for read mode
     * when Basic Authentication is configured. Verifies that the 'Accept' header
     * is set to 'application/json' and that the 'Authorization' header starts with 'Basic '.
     */
    @Test
    void buildsReadHeadersWithBasicAuth() {
        HttpHeaderBuilder builder = new HttpHeaderBuilder();
        SourceSystem sys = new SourceSystem();
        sys.id = 1L;
        BasicAuthConfig auth = new BasicAuthConfig();
        auth.username = "user";
        auth.password = "pass";
        sys.authConfig = auth;

        Map<String, String> h = builder.buildMergedHeaders(sys, HttpHeaderBuilder.Mode.READ);
        assertThat(h.get("Accept")).isEqualTo("application/json");
        assertThat(h.get("Authorization")).startsWith("Basic ");
    }

    /**
     * Tests that the HttpHeaderBuilder correctly includes the 'Content-Type' header
     * when operating in write mode (WRITE_JSON). Ensures that both 'Accept' and 'Content-Type'
     * are set to 'application/json'.
     */
    @Test
    void buildsWriteHeadersSetsContentType() {
        HttpHeaderBuilder builder = new HttpHeaderBuilder();
        Map<String, String> h = builder.buildMergedHeaders(null, HttpHeaderBuilder.Mode.WRITE_JSON);
        assertThat(h.get("Accept")).isEqualTo("application/json");
        assertThat(h.get("Content-Type")).isEqualTo("application/json");
    }
}
