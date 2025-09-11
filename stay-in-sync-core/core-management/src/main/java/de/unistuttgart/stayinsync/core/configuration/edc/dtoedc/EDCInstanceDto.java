package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public class EDCInstanceDto {

    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    @NotBlank
    private String protocolVersion;

    private String description;

    @NotBlank
    private String bpn;

    private String apiKey; // API-Schlüssel für die Authentifizierung

    private Set<UUID> edcAssetIds;

    // === Getter & Setter ===
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBpn() {
        return bpn;
    }

    public void setBpn(String bpn) {
        this.bpn = bpn;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Set<UUID> getEdcAssetIds() {
        return edcAssetIds;
    }

    public void setEdcAssetIds(Set<UUID> edcAssetIds) {
        this.edcAssetIds = edcAssetIds;
    }
}
