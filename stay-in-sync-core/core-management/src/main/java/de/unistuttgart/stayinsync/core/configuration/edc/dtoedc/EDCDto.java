package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.NotNull;

public class EDCDto {

    @Null(groups = Create.class)
    @NotNull(groups = Update.class)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    @NotBlank
    private String apiKey;

    /**
     * IDs der Assets, die zu diesem EDC gehören.
     * Wird bei Create leer gelassen, beim Read/Update geliefert.
     */
    private Set<@NotNull Long> edcAssetIds;

    // Marker‑Interfaces für Validation‑Groups
    public interface Create {}
    public interface Update {}

    // Getter & Setter

    public Long getId() {
        return id;
    }

    public EDCDto setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public EDCDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public EDCDto setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public EDCDto setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public Set<Long> getEdcAssetIds() {
        return edcAssetIds;
    }

    public EDCDto setEdcAssetIds(Set<Long> edcAssetIds) {
        this.edcAssetIds = edcAssetIds;
        return this;
    }
}
