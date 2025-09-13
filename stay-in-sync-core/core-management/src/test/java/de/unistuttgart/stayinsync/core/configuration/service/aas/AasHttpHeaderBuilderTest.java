package de.unistuttgart.stayinsync.core.configuration.service.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.BasicAuthConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AasHttpHeaderBuilderTest {

    @Test
    void buildsReadHeadersWithBasicAuth() {
        HttpHeaderBuilder builder = new HttpHeaderBuilder();
        SyncSystem sys = new SyncSystem() {};
        BasicAuthConfig auth = new BasicAuthConfig();
        auth.username = "user";
        auth.password = "pass";
        sys.authConfig = auth;

        Map<String, String> h = builder.buildMergedHeaders(sys, HttpHeaderBuilder.Mode.READ);
        assertThat(h.get("Accept")).isEqualTo("application/json");
        assertThat(h.get("Authorization")).startsWith("Basic ");
    }

    @Test
    void buildsWriteHeadersSetsContentType() {
        HttpHeaderBuilder builder = new HttpHeaderBuilder();
        Map<String, String> h = builder.buildMergedHeaders(null, HttpHeaderBuilder.Mode.WRITE_JSON);
        assertThat(h.get("Accept")).isEqualTo("application/json");
        assertThat(h.get("Content-Type")).isEqualTo("application/json");
    }
}



