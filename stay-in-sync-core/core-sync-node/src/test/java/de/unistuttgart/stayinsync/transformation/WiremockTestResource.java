package de.unistuttgart.stayinsync.transformation;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;


import java.util.Map;

public class WiremockTestResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wiremock;

    /**
     * Called before the tests are run. This method starts the WireMockServer on a dynamic port.
     * @return A map containing the configuration properties to be injected into the test run.
     *         We inject the server's base URL so our application code can connect to it.
     */
    @Override
    public Map<String, String> start() {
        wiremock = new WireMockServer();
        wiremock.start();

        String baseUrl = wiremock.baseUrl();

        return Map.of("quarkus.wiremock.devservices.url", baseUrl);
    }

    /**
     * Called after all tests have been run. This method stops the WireMockServer
     * and releases its resources.
     */
    @Override
    public void stop() {
        if (wiremock != null) {
            wiremock.stop();
        }
    }
}
