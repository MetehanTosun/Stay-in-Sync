package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity-Klasse für EDC-DataAddress.
 * Repräsentiert die Daten-Adresse eines Assets im EDC-System.
 */
@Entity
@Table(name = "edc_data_address")
public class EDCDataAddress extends UuidEntity {

    /**
     * Der JSON-LD Typ der Daten-Adresse, standardmäßig "DataAddress".
     */
    @Column(name = "jsonld_type", nullable = false)
    private String jsonLDType = "DataAddress";

    /**
     * Der Typ der Daten-Adresse, standardmäßig "HttpData".
     */
    @Column(name = "type", nullable = false)
    private String type = "HttpData";

    /**
     * Die Basis-URL für den Zugriff auf die Daten.
     */
    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    /**
     * Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     */
    @Column(name = "proxy_path", nullable = false)
    private boolean proxyPath = true;

    /**
     * Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     */
    @Column(name = "proxy_query_params", nullable = false)
    private boolean proxyQueryParams = true;

    /**
     * Default-Konstruktor für JPA.
     */
    public EDCDataAddress() {
        // Standardwerte setzen
    }

    /**
     * Konstruktor mit Basis-URL.
     * 
     * @param baseUrl Die Basis-URL für den Zugriff auf die Daten
     */
    public EDCDataAddress(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Vollständiger Konstruktor.
     * 
     * @param type Der Typ der Daten-Adresse
     * @param baseUrl Die Basis-URL für den Zugriff auf die Daten
     * @param proxyPath Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll
     * @param proxyQueryParams Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen
     */
    public EDCDataAddress(String type, String baseUrl, boolean proxyPath, boolean proxyQueryParams) {
        this.type = type != null ? type : "HttpData";
        this.baseUrl = baseUrl;
        this.proxyPath = proxyPath;
        this.proxyQueryParams = proxyQueryParams;
    }

    /**
     * Getter für JSON-LD Typ.
     */
    public String getJsonLDType() {
        return jsonLDType;
    }

    /**
     * Setter für JSON-LD Typ.
     */
    public void setJsonLDType(String jsonLDType) {
        this.jsonLDType = jsonLDType;
    }

    /**
     * Getter für Typ.
     */
    public String getType() {
        return type;
    }

    /**
     * Setter für Typ.
     */
    public void setType(String type) {
        this.type = type != null ? type : "HttpData";
    }

    /**
     * Getter für Basis-URL.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Setter für Basis-URL.
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Getter für proxyPath.
     */
    public boolean isProxyPath() {
        return proxyPath;
    }

    /**
     * Setter für proxyPath.
     */
    public void setProxyPath(Boolean proxyPath) {
        this.proxyPath = proxyPath != null ? proxyPath : true;
    }

    /**
     * Getter für proxyQueryParams.
     */
    public boolean isProxyQueryParams() {
        return proxyQueryParams;
    }

    /**
     * Setter für proxyQueryParams.
     */
    public void setProxyQueryParams(Boolean proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams != null ? proxyQueryParams : true;
    }
}
