package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC-DataAddress.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 */
public class EDCDataAddressDto {

    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    @JsonIgnore
    private UUID id;

    /**
     * Der JSON-LD Typ der Daten-Adresse, standardmäßig "DataAddress".
     */
    @JsonProperty("@type")
    private String jsonLDType = "DataAddress";

    /**
     * Der Typ der Daten-Adresse, standardmäßig "HttpData".
     */
    @NotBlank
    private String type = "HttpData";

    /**
     * Die Basis-URL für den Zugriff auf die Daten.
     * Unterstützt beide Feldnamen: baseURL und base_url
     */
    @JsonProperty("baseUrl")
    @NotBlank
    private String baseUrl;

    /**
     * Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     */
    @JsonProperty("proxyPath")
    private Boolean proxyPath;

    /**
     * Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     */
    @JsonProperty("proxyQueryParams")
    private Boolean proxyQueryParams;

    /**
     * Gibt die ID des DTOs zurück.
     * 
     * @return Die UUID des DTOs
     */
    public UUID getId() {
        return id;
    }

    /**
     * Setzt die ID des DTOs.
     * 
     * @param id Die zu setzende UUID
     * @return Das DTO selbst für Method Chaining
     */
    public EDCDataAddressDto setId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Gibt den JSON-LD Typ der Daten-Adresse zurück.
     * 
     * @return Der JSON-LD Typ der Daten-Adresse
     */
    public String getJsonLDType() {
        return jsonLDType;
    }

    /**
     * Setzt den JSON-LD Typ der Daten-Adresse.
     * 
     * @param jsonLDType Der zu setzende JSON-LD Typ
     * @return Das DTO selbst für Method Chaining
     */
    public EDCDataAddressDto setJsonLDType(String jsonLDType) {
        this.jsonLDType = jsonLDType;
        return this;
    }

    /**
     * Gibt den Typ der Daten-Adresse zurück.
     * 
     * @return Der Typ der Daten-Adresse
     */
    public String getType() {
        return type;
    }

    /**
     * Setzt den Typ der Daten-Adresse.
     * 
     * @param type Der zu setzende Typ
     * @return Das DTO selbst für Method Chaining
     */
    public EDCDataAddressDto setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Gibt die Basis-URL für den Zugriff auf die Daten zurück.
     * 
     * @return Die Basis-URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Setzt die Basis-URL für den Zugriff auf die Daten.
     * 
     * @param baseUrl Die zu setzende Basis-URL
     * @return Das DTO selbst für Method Chaining
     */
    public EDCDataAddressDto setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Getter für den alten Feldnamen baseURL (Kompatibilität).
     * 
     * @return Die Basis-URL
     */
    @JsonProperty("baseURL")
    public String getBaseURL() {
        return baseUrl;
    }

    /**
     * Setter für den alten Feldnamen baseURL (Kompatibilität).
     * 
     * @param baseURL Die zu setzende Basis-URL
     * @return Das DTO selbst für Method Chaining
     */
    @JsonProperty("baseURL")
    public EDCDataAddressDto setBaseURL(String baseURL) {
        this.baseUrl = baseURL;
        return this;
    }

    /**
     * Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     * 
     * @return true, wenn der Pfad übernommen werden soll, sonst false
     */
    public Boolean getProxyPath() {
        return proxyPath != null ? proxyPath : Boolean.TRUE;
    }

    /**
     * Setzt, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     * 
     * @param proxyPath true, wenn der Pfad übernommen werden soll, sonst false
     * @return Das DTO selbst für Method Chaining
     */
    public EDCDataAddressDto setProxyPath(Boolean proxyPath) {
        this.proxyPath = proxyPath;
        return this;
    }

    /**
     * Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     * 
     * @return true, wenn die Query-Parameter übernommen werden sollen, sonst false
     */
    public Boolean getProxyQueryParams() {
        return proxyQueryParams != null ? proxyQueryParams : Boolean.TRUE;
    }

    /**
     * Setzt, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     * 
     * @param proxyQueryParams true, wenn die Query-Parameter übernommen werden sollen, sonst false
     * @return Das DTO selbst für Method Chaining
     */
    public EDCDataAddressDto setProxyQueryParams(Boolean proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams;
        return this;
    }
    
    /**
     * Gibt eine String-Repräsentation des DTOs zurück.
     * 
     * @return Eine lesbare Darstellung des DTOs
     */
    @Override
    public String toString() {
        return "EDCDataAddressDto{" +
                "id=" + id +
                ", jsonLDType='" + jsonLDType + '\'' +
                ", type='" + type + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", proxyPath=" + proxyPath +
                ", proxyQueryParams=" + proxyQueryParams +
                '}';
    }
}
