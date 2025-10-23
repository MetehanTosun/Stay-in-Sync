package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity-Klasse für EDC-DataAddress.
 * Repräsentiert die Daten-Adresse eines Assets im EDC-System.
 */
@Entity
@Table(name = "edc_data_address")
public class DataAddress extends PanacheEntity {

    /**
     * Der JSON-LD Typ der Daten-Adresse, standardmäßig "DataAddress".
     */
    @Column(name = "jsonld_type", nullable = false)
    public String jsonLDType = "DataAddress";

    /**
     * Der Typ der Daten-Adresse, standardmäßig "HttpData".
     */
    @Column(name = "type", nullable = false)
    public String type = "HttpData";

    /**
     * Die Basis-URL für den Zugriff auf die Daten.
     */
    @Column(name = "base_url", nullable = false)
    public String baseUrl;

    /**
     * Der Pfad, der an die Basis-URL angehängt werden soll.
     */
    @Column(name = "path")
    public String path;

    /**
     * Query-Parameter als JSON-String.
     */
    @Column(name = "query_params", columnDefinition = "TEXT")
    public String queryParams;

    /**
     * Header-Parameter als JSON-String.
     */
    @Column(name = "header_params", columnDefinition = "TEXT")
    public String headerParams;

    /**
     * Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll.
     */
    @Column(name = "proxy_path", nullable = false)
    public boolean proxyPath = true;

    /**
     * Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen.
     */
    @Column(name = "proxy_query_params", nullable = false)
    public boolean proxyQueryParams = true;

    /**
     * Default-Konstruktor für JPA.
     */
    public DataAddress() {
        // Standardwerte sind bereits als Feldinitialisierungen gesetzt
    }

    /**
     * Konstruktor mit Basis-URL.
     * 
     * @param baseUrl Die Basis-URL für den Zugriff auf die Daten
     */
    public DataAddress(String baseUrl) {
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
    public DataAddress(String type, String baseUrl, boolean proxyPath, boolean proxyQueryParams) {
        this.type = type != null ? type : "HttpData";
        this.baseUrl = baseUrl;
        this.proxyPath = proxyPath;
        this.proxyQueryParams = proxyQueryParams;
    }
    
    /**
     * Erweiterter Konstruktor mit allen Parametern.
     *
     * @param type Der Typ der Daten-Adresse
     * @param baseUrl Die Basis-URL für den Zugriff auf die Daten
     * @param path Der Pfad, der an die Basis-URL angehängt werden soll
     * @param queryParams Query-Parameter als JSON-String
     * @param headerParams Header-Parameter als JSON-String
     * @param proxyPath Gibt an, ob der Pfad beim Proxy-Zugriff übernommen werden soll
     * @param proxyQueryParams Gibt an, ob Query-Parameter beim Proxy-Zugriff übernommen werden sollen
     */
    public DataAddress(String type, String baseUrl, String path, String queryParams, String headerParams, boolean proxyPath, boolean proxyQueryParams) {
        this.type = type != null ? type : "HttpData";
        this.baseUrl = baseUrl;
        this.path = path;
        this.queryParams = queryParams;
        this.headerParams = headerParams;
        this.proxyPath = proxyPath;
        this.proxyQueryParams = proxyQueryParams;
    }

    /**
     * Spezielle Setter-Methode für den Typ, um den Standardwert "HttpData" zu gewährleisten.
     */
    public void setType(String type) {
        this.type = type != null ? type : "HttpData";
    }

    /**
     * Spezielle Setter-Methode für proxyPath, um den Standardwert true zu gewährleisten.
     */
    public void setProxyPath(Boolean proxyPath) {
        this.proxyPath = proxyPath != null ? proxyPath : true;
    }

    /**
     * Spezielle Setter-Methode für proxyQueryParams, um den Standardwert true zu gewährleisten.
     */
    public void setProxyQueryParams(Boolean proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams != null ? proxyQueryParams : true;
    }

    /**
     * Getter für den Pfad.
     * 
     * @return Den Pfad oder null, wenn nicht gesetzt
     */
    public String getPath() {
        return path;
    }

    /**
     * Setter für den Pfad.
     * 
     * @param path Der Pfad, der an die Basis-URL angehängt werden soll
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Getter für die Query-Parameter.
     * 
     * @return Die Query-Parameter als JSON-String oder null, wenn nicht gesetzt
     */
    public String getQueryParams() {
        return queryParams;
    }

    /**
     * Setter für die Query-Parameter.
     * 
     * @param queryParams Die Query-Parameter als JSON-String
     */
    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    /**
     * Getter für die Header-Parameter.
     * 
     * @return Die Header-Parameter als JSON-String oder null, wenn nicht gesetzt
     */
    public String getHeaderParams() {
        return headerParams;
    }

    /**
     * Setter für die Header-Parameter.
     * 
     * @param headerParams Die Header-Parameter als JSON-String
     */
    public void setHeaderParams(String headerParams) {
        this.headerParams = headerParams;
    }
}
