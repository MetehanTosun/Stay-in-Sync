package de.unistuttgart.stayinsync.core.configuration.edc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.DataAddress;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service-Klasse für das Abrufen von Daten über HTTP mit erweiterter Unterstützung für
 * Pfade, Query-Parameter und Header-Parameter.
 */
@ApplicationScoped
public class HttpDataFetcherService {

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Führt einen HTTP-Request basierend auf einer DataAddress durch.
     *
     * @param dataAddress Die DataAddress mit den Verbindungsdaten
     * @return Die HTTP-Antwort als String
     * @throws IOException Bei Fehlern in der HTTP-Kommunikation
     * @throws InterruptedException Wenn der HTTP-Request unterbrochen wird
     * @throws IllegalArgumentException Wenn dataAddress oder baseUrl ungültig ist
     */
    public String fetchData(DataAddress dataAddress) throws IOException, InterruptedException {
        if (dataAddress == null) {
            throw new IllegalArgumentException("DataAddress darf nicht null sein");
        }
        
        if (dataAddress.baseUrl == null || dataAddress.baseUrl.isEmpty()) {
            throw new IllegalArgumentException("DataAddress muss eine gültige baseUrl enthalten");
        }

        // Basis-URL vorbereiten
        StringBuilder urlBuilder = new StringBuilder(dataAddress.baseUrl);

        // Pfad hinzufügen, wenn vorhanden
        if (dataAddress.path != null && !dataAddress.path.isEmpty()) {
            if (!dataAddress.path.startsWith("/") && !dataAddress.baseUrl.endsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append(dataAddress.path);
        }

        // Query-Parameter hinzufügen, wenn vorhanden
        Map<String, String> queryParams = parseJsonParams(dataAddress.queryParams);
        if (queryParams != null && !queryParams.isEmpty()) {
            urlBuilder.append("?");
            urlBuilder.append(queryParams.entrySet().stream()
                    .map(entry -> encodeQueryParam(entry.getKey()) + "=" + encodeQueryParam(entry.getValue()))
                    .collect(Collectors.joining("&")));
        }

        // HTTP-Request aufbauen
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .GET();

        // Header-Parameter hinzufügen, wenn vorhanden
        Map<String, String> headerParams = parseJsonParams(dataAddress.headerParams);
        if (headerParams != null) {
            headerParams.forEach(requestBuilder::header);
        }

        // Request ausführen
        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        // Ergebnis zurückgeben
        return response.body();
    }

    /**
     * Parst einen JSON-String in eine Map.
     *
     * @param jsonParams JSON-String mit Parametern
     * @return Map mit Parametern oder null, wenn der String leer oder ungültig ist
     */
    private Map<String, String> parseJsonParams(String jsonParams) {
        if (jsonParams == null || jsonParams.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonParams, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Kodiert einen Query-Parameter für die URL.
     *
     * @param value Der zu kodierende Wert
     * @return Der kodierte Wert
     */
    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}