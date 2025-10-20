package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) für EDC-DataAddress.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 * Implementiert als Record für mehr Effizienz und bessere Lesbarkeit.
 */
public record EDCDataAddressDto(
    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    @JsonIgnore
    Long id,

    /**
     * Der JSON-LD Typ der Daten-Adresse, standardmäßig "DataAddress".
     */
    @JsonProperty("@type")
    String jsonLDType,

    /**
     * Der Typ der Daten-Adresse, standardmäßig "HttpData".
     */
    @NotBlank
    String type,

    /**
     * Die Basis-URL für den Zugriff auf die Daten.
     */
    @JsonProperty("baseUrl")
    @NotBlank
    String baseUrl,

    /**
     * Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     */
    @JsonProperty("proxyPath")
    Boolean proxyPath,

    /**
     * Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     */
    @JsonProperty("proxyQueryParams")
    Boolean proxyQueryParams
) {
    /**
     * Standardkonstruktor mit Defaultwerten.
     */
    public EDCDataAddressDto() {
        this(null, "DataAddress", "HttpData", "", true, true);
    }
    
    /**
     * Konstruktor mit minimalen Parametern.
     * 
     * @param baseUrl Die Basis-URL für den Zugriff auf die Daten
     */
    public EDCDataAddressDto(String baseUrl) {
        this(null, "DataAddress", "HttpData", baseUrl, true, true);
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
     * Erstellt ein neues DTO mit der angegebenen ID.
     * 
     * @param id Die neue ID
     * @return Ein neues DTO mit der angegebenen ID
     */
    public EDCDataAddressDto withId(Long id) {
        return new EDCDataAddressDto(id, this.jsonLDType, this.type, this.baseUrl, 
                                    this.proxyPath, this.proxyQueryParams);
    }

    /**
     * Erstellt ein neues DTO mit dem angegebenen JSON-LD Typ.
     * 
     * @param jsonLDType Der neue JSON-LD Typ
     * @return Ein neues DTO mit dem angegebenen JSON-LD Typ
     */
    public EDCDataAddressDto withJsonLDType(String jsonLDType) {
        return new EDCDataAddressDto(this.id, jsonLDType, this.type, this.baseUrl, 
                                    this.proxyPath, this.proxyQueryParams);
    }

    /**
     * Erstellt ein neues DTO mit dem angegebenen Typ.
     * 
     * @param type Der neue Typ
     * @return Ein neues DTO mit dem angegebenen Typ
     */
    public EDCDataAddressDto withType(String type) {
        return new EDCDataAddressDto(this.id, this.jsonLDType, type, this.baseUrl, 
                                    this.proxyPath, this.proxyQueryParams);
    }

    /**
     * Erstellt ein neues DTO mit der angegebenen Basis-URL.
     * 
     * @param baseUrl Die neue Basis-URL
     * @return Ein neues DTO mit der angegebenen Basis-URL
     */
    public EDCDataAddressDto withBaseUrl(String baseUrl) {
        return new EDCDataAddressDto(this.id, this.jsonLDType, this.type, baseUrl, 
                                    this.proxyPath, this.proxyQueryParams);
    }

    /**
     * Erstellt ein neues DTO mit der angegebenen baseURL (Kompatibilitätsmethode).
     * 
     * @param baseURL Die neue Basis-URL
     * @return Ein neues DTO mit der angegebenen Basis-URL
     */
    public EDCDataAddressDto withBaseURL(String baseURL) {
        return withBaseUrl(baseURL);
    }

    /**
     * Erstellt ein neues DTO mit dem angegebenen proxyPath-Wert.
     * 
     * @param proxyPath Der neue proxyPath-Wert
     * @return Ein neues DTO mit dem angegebenen proxyPath-Wert
     */
    public EDCDataAddressDto withProxyPath(Boolean proxyPath) {
        return new EDCDataAddressDto(this.id, this.jsonLDType, this.type, this.baseUrl, 
                                    proxyPath, this.proxyQueryParams);
    }

    /**
     * Erstellt ein neues DTO mit dem angegebenen proxyQueryParams-Wert.
     * 
     * @param proxyQueryParams Der neue proxyQueryParams-Wert
     * @return Ein neues DTO mit dem angegebenen proxyQueryParams-Wert
     */
    public EDCDataAddressDto withProxyQueryParams(Boolean proxyQueryParams) {
        return new EDCDataAddressDto(this.id, this.jsonLDType, this.type, this.baseUrl, 
                                    this.proxyPath, proxyQueryParams);
    }
    
    /**
     * Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     * Falls nicht gesetzt, wird true zurückgegeben.
     * 
     * @return true, wenn der Pfad übernommen werden soll, sonst false
     */
    public Boolean getProxyPath() {
        return proxyPath != null ? proxyPath : Boolean.TRUE;
    }

    /**
     * Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     * Falls nicht gesetzt, wird true zurückgegeben.
     * 
     * @return true, wenn die Query-Parameter übernommen werden sollen, sonst false
     */
    public Boolean getProxyQueryParams() {
        return proxyQueryParams != null ? proxyQueryParams : Boolean.TRUE;
    }
}