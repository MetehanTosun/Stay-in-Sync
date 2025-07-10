package de.unistuttgart.stayinsync.pollingnode.execution.ressource;


import io.vertx.mutiny.ext.web.client.WebClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class RestClientTest {

    private WebClient webClient;
    private RestClient restClient;


    @Test
    @DisplayName("ConfigureRequest reacts correctly to null fields.")
    void testConfigureRequestAllFieldsNull(){
    }

    @Test
    @DisplayName("ConfigureRequest reacts correctly to empty fields.")
    void testConfigureGetRequestSuccessfully(){

    }


    @Test
    void testCleanupClosesWebClient() {
        restClient.cleanup();

    }
}