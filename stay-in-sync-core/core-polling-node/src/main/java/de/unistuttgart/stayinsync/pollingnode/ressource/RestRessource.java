package de.unistuttgart.stayinsync.pollingnode.ressource;


import de.unistuttgart.stayinsync.pollingnode.entities.ApiAddress;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class RestRessource {
    Map<ApiAddress, HttpRequest> cachedRequests;

    public RestRessource() {
        super();
        this.cachedRequests = new HashMap<>();
    }

    @GET
    public String getJsonDataOf(final ApiAddress apiAddress){
            if(!cachedRequests.containsKey(apiAddress)){
                cacheHttpRequest(apiAddress);
            }

        HttpResponse<String> response = httpClient.send(cachedRequests.get(apiAddress),
                HttpResponse.BodyHandlers.ofString());

        // Response zurückgeben
        return Response.status(response.statusCode())
                .entity(response.body())
                .build();



    }

    public void removeCachedHttpRequest(final ApiAddress apiAddress){
        cachedRequests.remove(apiAddress);
    }

    private Boolean cacheHttpRequest(final ApiAddress apiAddress) throws{
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiAddress.getString()))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            cachedRequests.put(apiAddress, httpRequest);
        } catch() {
            logger.log("Conversion from ApiAddress" + apiAddress + "to HttpRequest was not successful. ");
        }
    }
}
