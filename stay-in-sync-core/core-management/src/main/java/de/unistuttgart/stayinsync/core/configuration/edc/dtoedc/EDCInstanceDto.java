package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public class EDCInstanceDto {

    public UUID id;

    @NotBlank
    public String name;

    @NotBlank
    public String url;

    @NotBlank
    public String apiKey;

    public Set<UUID> edcAssetIds;

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
